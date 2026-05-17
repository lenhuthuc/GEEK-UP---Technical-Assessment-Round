package com.geekup.concert.voucher;

import com.geekup.concert.common.exception.BusinessException;
import com.geekup.concert.voucher.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        if (voucherRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("ALREADY_EXISTS", "Voucher code already exists");
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .type(Voucher.VoucherType.valueOf(request.getType()))
                .value(request.getValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .maxUses(request.getMaxUses())
                .usedCount(0)
                .maxUsesPerUser(request.getMaxUsesPerUser())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .status(Voucher.VoucherStatus.ACTIVE)
                .build();

        voucher = voucherRepository.save(voucher);
        return toResponse(voucher);
    }

    public VoucherPreviewResponse previewDiscount(String voucherCode, BigDecimal subtotal, Long userId) {
        try {
            Voucher voucher = validateVoucher(voucherCode, subtotal, userId);
            BigDecimal discount = calculateDiscount(voucher, subtotal);

            return VoucherPreviewResponse.builder()
                    .code(voucher.getCode())
                    .type(voucher.getType().name())
                    .discountAmount(discount)
                    .subtotalBefore(subtotal)
                    .totalAfterDiscount(subtotal.subtract(discount))
                    .valid(true)
                    .message("Voucher applied successfully")
                    .build();
        } catch (BusinessException e) {
            return VoucherPreviewResponse.builder()
                    .code(voucherCode)
                    .discountAmount(BigDecimal.ZERO)
                    .subtotalBefore(subtotal)
                    .totalAfterDiscount(subtotal)
                    .valid(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    public BigDecimal calculateDiscountForBooking(String voucherCode, BigDecimal subtotal, Long userId) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = validateVoucher(voucherCode, subtotal, userId);
        return calculateDiscount(voucher, subtotal);
    }

    public Voucher validateVoucher(String voucherCode, BigDecimal subtotal, Long userId) {
        Voucher voucher = voucherRepository.findByCode(voucherCode.toUpperCase())
                .orElseThrow(() -> new BusinessException("VOUCHER_INVALID", "Invalid voucher code"));

        Instant now = Instant.now();

        if (voucher.getStatus() != Voucher.VoucherStatus.ACTIVE) {
            throw new BusinessException("VOUCHER_INVALID", "Voucher is not active");
        }

        if (now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidUntil())) {
            throw new BusinessException("VOUCHER_EXPIRED", "Voucher is not valid at this time");
        }

        if (voucher.getUsedCount() >= voucher.getMaxUses()) {
            throw new BusinessException("VOUCHER_EXHAUSTED", "Voucher has reached maximum usage");
        }

        if (voucher.getMinOrderAmount() != null && subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new BusinessException("VOUCHER_MIN_ORDER",
                    "Minimum order amount is " + voucher.getMinOrderAmount());
        }

        // Check per-user limit
        long userUsageCount = voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), userId);
        if (userUsageCount >= voucher.getMaxUsesPerUser()) {
            throw new BusinessException("VOUCHER_USER_LIMIT", "You have reached the maximum usage for this voucher");
        }

        return voucher;
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount;

        if (voucher.getType() == Voucher.VoucherType.PERCENT) {
            discount = subtotal.multiply(voucher.getValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            // Cap discount at max_discount_amount
            if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            // FIXED
            discount = voucher.getValue();
        }

        // Discount cannot exceed subtotal
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount;
    }

    public Page<VoucherResponse> listVouchers(String code, Voucher.VoucherStatus status, Pageable pageable) {
        return voucherRepository.searchVouchers(code, status, pageable).map(this::toResponse);
    }

    private VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .type(voucher.getType().name())
                .value(voucher.getValue())
                .minOrderAmount(voucher.getMinOrderAmount())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .maxUses(voucher.getMaxUses())
                .usedCount(voucher.getUsedCount())
                .maxUsesPerUser(voucher.getMaxUsesPerUser())
                .validFrom(voucher.getValidFrom())
                .validUntil(voucher.getValidUntil())
                .status(voucher.getStatus().name())
                .build();
    }
}
