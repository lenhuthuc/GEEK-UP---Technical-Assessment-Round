package com.geekup.concert.booking;

import com.geekup.concert.booking.dto.BookingResponse;
import com.geekup.concert.booking.dto.HoldRequest;
import com.geekup.concert.common.audit.AuditService;
import com.geekup.concert.common.exception.BusinessException;
import com.geekup.concert.concert.Concert;
import com.geekup.concert.concert.ConcertRepository;
import com.geekup.concert.payment.Payment;
import com.geekup.concert.payment.PaymentRepository;
import com.geekup.concert.ticket.TicketCategory;
import com.geekup.concert.ticket.TicketCategoryRepository;
import com.geekup.concert.voucher.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final ConcertRepository concertRepository;
    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    @Value("${app.booking.hold-ttl-minutes:10}")
    private int holdTtlMinutes;

    @Value("${app.booking.max-tickets-per-booking:8}")
    private int maxTicketsPerBooking;

    /**
     * Phase 1: Hold booking - creates a PENDING_PAYMENT booking and decrements available inventory.
     * All inside one @Transactional block with SELECT ... FOR UPDATE on ticket_categories.
     */
    @Transactional
    public BookingResponse hold(HoldRequest request, String idempotencyKey, Long userId) {
        // 1) Idempotency check at DB level
        Optional<Booking> existing = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // 2) Validate concert
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Concert not found"));

        if (concert.getStatus() != Concert.ConcertStatus.PUBLISHED) {
            throw new BusinessException("CONCERT_NOT_AVAILABLE", "Concert is not available for booking");
        }

        // 3) Validate total ticket count
        int totalTickets = request.getItems().stream().mapToInt(HoldRequest.HoldItem::getQuantity).sum();
        if (totalTickets > maxTicketsPerBooking) {
            throw new BusinessException("MAX_TICKETS_EXCEEDED",
                    "Maximum " + maxTicketsPerBooking + " tickets per booking");
        }

        // 4) Lock the affected ticket_category rows (SELECT ... FOR UPDATE)
        List<Long> categoryIds = request.getItems().stream()
                .map(HoldRequest.HoldItem::getTicketCategoryId)
                .toList();

        List<TicketCategory> categories = ticketCategoryRepository.findAllByIdForUpdate(categoryIds);

        Map<Long, TicketCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategory::getId, Function.identity()));

        // 5) Validate all categories belong to the same concert
        for (TicketCategory cat : categories) {
            if (!cat.getConcertId().equals(request.getConcertId())) {
                throw new BusinessException("INVALID_CATEGORY",
                        "Ticket category " + cat.getId() + " does not belong to concert " + request.getConcertId());
            }
        }

        // 6) Validate availability and calculate subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        List<BookingItem> bookingItems = new ArrayList<>();

        for (HoldRequest.HoldItem item : request.getItems()) {
            TicketCategory cat = categoryMap.get(item.getTicketCategoryId());
            if (cat == null) {
                throw new BusinessException("NOT_FOUND",
                        "Ticket category not found: " + item.getTicketCategoryId());
            }

            if (cat.getAvailableQuantity() < item.getQuantity()) {
                throw new BusinessException("OUT_OF_STOCK",
                        "Not enough tickets in category: " + cat.getName(),
                        "Available: " + cat.getAvailableQuantity() + ", Requested: " + item.getQuantity());
            }

            // Decrement available quantity
            cat.setAvailableQuantity(cat.getAvailableQuantity() - item.getQuantity());

            BigDecimal itemSubtotal = cat.getPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            bookingItems.add(BookingItem.builder()
                    .ticketCategoryId(cat.getId())
                    .quantity(item.getQuantity())
                    .unitPrice(cat.getPriceAmount())
                    .subtotal(itemSubtotal)
                    .build());
        }

        // 7) Apply voucher (preview only - not committed to voucher_usages yet)
        BigDecimal discount = BigDecimal.ZERO;
        Long voucherId = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            discount = voucherService.calculateDiscountForBooking(request.getVoucherCode(), subtotal, userId);
            Voucher voucher = voucherRepository.findByCode(request.getVoucherCode().toUpperCase()).orElse(null);
            if (voucher != null) {
                voucherId = voucher.getId();
            }
        }

        BigDecimal totalAmount = subtotal.subtract(discount);

        // 8) Create booking
        Booking booking = Booking.builder()
                .userId(userId)
                .concertId(request.getConcertId())
                .status(BookingStatus.PENDING_PAYMENT)
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .voucherId(voucherId)
                .idempotencyKey(idempotencyKey)
                .holdExpiresAt(Instant.now().plusSeconds(holdTtlMinutes * 60L))
                .build();

        booking = bookingRepository.save(booking);

        // 9) Save booking items
        for (BookingItem item : bookingItems) {
            item.setBookingId(booking.getId());
        }
        bookingItemRepository.saveAll(bookingItems);
        booking.setItems(bookingItems);

        log.info("Booking {} created for user {} - concert {} - total {}",
                booking.getId(), userId, request.getConcertId(), totalAmount);

        return toResponse(booking);
    }

    /**
     * Phase 2: Confirm payment - mock webhook callback.
     * Transitions PENDING_PAYMENT -> PAID or PENDING_PAYMENT -> FAILED.
     */
    @Transactional
    public BookingResponse confirmPayment(Long bookingId, Long userId, boolean success,
                                          String providerTxnId, String provider) {
        // Idempotent on provider_txn_id
        if (providerTxnId != null && paymentRepository.existsByProviderTxnId(providerTxnId)) {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));
            return toResponse(booking);
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));

        // Verify ownership (skip for webhook calls where userId is null)
        if (userId != null && !booking.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "You do not own this booking");
        }
        // For webhook: use booking's userId
        Long effectiveUserId = userId != null ? userId : booking.getUserId();

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("INVALID_STATE_TRANSITION",
                    "Booking is not in PENDING_PAYMENT state");
        }

        // Check if hold has expired
        if (booking.getHoldExpiresAt() != null && Instant.now().isAfter(booking.getHoldExpiresAt())) {
            throw new BusinessException("BOOKING_EXPIRED", "Booking hold has expired");
        }

        if (success) {
            booking.setStatus(BookingStatus.PAID);
            booking.setPaidAt(Instant.now());

            // Save payment record
            Payment payment = Payment.builder()
                    .bookingId(bookingId)
                    .provider(provider != null ? provider : "MOCK")
                    .providerTxnId(providerTxnId != null ? providerTxnId : UUID.randomUUID().toString())
                    .amount(booking.getTotalAmount())
                    .currency("VND")
                    .status("SUCCESS")
                    .build();
            paymentRepository.save(payment);

            // Commit voucher usage now
            if (booking.getVoucherId() != null) {
                if (!voucherUsageRepository.existsByVoucherIdAndBookingId(booking.getVoucherId(), bookingId)) {
                    VoucherUsage usage = VoucherUsage.builder()
                            .voucherId(booking.getVoucherId())
                            .userId(effectiveUserId)
                            .bookingId(bookingId)
                            .usedAt(Instant.now())
                            .build();
                    voucherUsageRepository.save(usage);
                    voucherRepository.incrementUsedCount(booking.getVoucherId());
                }
            }

            log.info("Booking {} PAID by user {}", bookingId, userId);
        } else {
            booking.setStatus(BookingStatus.FAILED);
            // Return tickets to inventory
            ticketCategoryRepository.releaseQuantities(bookingId);

            // Save payment record
            Payment payment = Payment.builder()
                    .bookingId(bookingId)
                    .provider(provider != null ? provider : "MOCK")
                    .providerTxnId(providerTxnId != null ? providerTxnId : UUID.randomUUID().toString())
                    .amount(booking.getTotalAmount())
                    .currency("VND")
                    .status("FAILED")
                    .build();
            paymentRepository.save(payment);

            log.info("Booking {} FAILED for user {}", bookingId, userId);
        }

        bookingRepository.save(booking);
        return toResponse(booking);
    }

    /**
     * Cancel a booking while in PENDING_PAYMENT state.
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "You do not own this booking");
        }

        BookingStateMachine.validate(booking.getStatus(), BookingStatus.CANCELLED);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setCancelledReason(reason != null ? reason : "User cancelled");

        // Return tickets to inventory
        ticketCategoryRepository.releaseQuantities(bookingId);

        bookingRepository.save(booking);
        log.info("Booking {} CANCELLED by user {}", bookingId, userId);

        return toResponse(booking);
    }

    /**
     * Get a single booking by ID for the authenticated user.
     */
    public BookingResponse getBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "You do not own this booking");
        }

        return toResponse(booking);
    }

    /**
     * List bookings for the authenticated user.
     */
    public Page<BookingResponse> listUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Ops: List all bookings with filters.
     */
    public Page<BookingResponse> searchBookings(BookingStatus status, Long concertId,
                                                 Long userId, Pageable pageable) {
        return bookingRepository.searchBookings(status, concertId, userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Ops: Get booking detail (no ownership check).
     */
    public BookingResponse getBookingForOps(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));
        return toResponse(booking);
    }

    /**
     * Ops: Manually update booking status per the state machine.
     */
    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatus newStatus,
                                                String reason, Long actorId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));

        String beforeStatus = booking.getStatus().name();
        BookingStateMachine.validate(booking.getStatus(), newStatus);

        booking.setStatus(newStatus);

        // If cancelling or failing, return tickets
        if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.FAILED) {
            ticketCategoryRepository.releaseQuantities(bookingId);
            booking.setCancelledAt(Instant.now());
            booking.setCancelledReason(reason);
        }

        bookingRepository.save(booking);

        // Audit log
        auditService.log(actorId, "OPERATOR", "UPDATE_BOOKING_STATUS",
                "BOOKING", bookingId.toString(), beforeStatus, newStatus.name(), null);

        return toResponse(booking);
    }

    /**
     * Ops: Mark booking as refunded.
     */
    @Transactional
    public BookingResponse refundBooking(Long bookingId, Long actorId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking not found"));

        BookingStateMachine.validate(booking.getStatus(), BookingStatus.REFUNDED);

        booking.setStatus(BookingStatus.REFUNDED);
        booking.setCancelledAt(Instant.now());
        booking.setCancelledReason("Refunded by operator");

        // Return tickets to inventory
        ticketCategoryRepository.releaseQuantities(bookingId);

        bookingRepository.save(booking);

        auditService.log(actorId, "OPERATOR", "REFUND_BOOKING",
                "BOOKING", bookingId.toString(), "PAID", "REFUNDED", null);

        log.info("Booking {} REFUNDED by operator {}", bookingId, actorId);

        return toResponse(booking);
    }

    // ---- Response builder ----

    private BookingResponse toResponse(Booking booking) {
        List<BookingItem> items = bookingItemRepository.findByBookingId(booking.getId());

        // Fetch category names
        List<Long> categoryIds = items.stream().map(BookingItem::getTicketCategoryId).toList();
        Map<Long, TicketCategory> categoryMap = ticketCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(TicketCategory::getId, Function.identity()));

        // Fetch concert title
        String concertTitle = concertRepository.findById(booking.getConcertId())
                .map(Concert::getTitle).orElse("Unknown");

        // Fetch voucher code
        String voucherCode = null;
        if (booking.getVoucherId() != null) {
            voucherCode = voucherRepository.findById(booking.getVoucherId())
                    .map(Voucher::getCode).orElse(null);
        }

        List<BookingResponse.BookingItemResponse> itemResponses = items.stream()
                .map(item -> {
                    TicketCategory cat = categoryMap.get(item.getTicketCategoryId());
                    return BookingResponse.BookingItemResponse.builder()
                            .id(item.getId())
                            .ticketCategoryId(item.getTicketCategoryId())
                            .ticketCategoryName(cat != null ? cat.getName() : "Unknown")
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .build();
                })
                .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .concertId(booking.getConcertId())
                .concertTitle(concertTitle)
                .status(booking.getStatus().name())
                .subtotalAmount(booking.getSubtotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .totalAmount(booking.getTotalAmount())
                .voucherCode(voucherCode)
                .holdExpiresAt(booking.getHoldExpiresAt())
                .paidAt(booking.getPaidAt())
                .cancelledAt(booking.getCancelledAt())
                .cancelledReason(booking.getCancelledReason())
                .createdAt(booking.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
