package com.geekup.concert.concert;

import com.geekup.concert.common.exception.BusinessException;
import com.geekup.concert.concert.dto.ConcertResponse;
import com.geekup.concert.concert.dto.CreateConcertRequest;
import com.geekup.concert.ticket.TicketCategory;
import com.geekup.concert.ticket.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    public Page<ConcertResponse> listPublishedConcerts(Pageable pageable) {
        Page<Concert> concerts = concertRepository.findAllPublished(pageable);
        return concerts.map(this::toResponse);
    }

    public Page<ConcertResponse> listUpcomingConcerts(Pageable pageable) {
        Page<Concert> concerts = concertRepository.findUpcomingPublished(Instant.now(), pageable);
        return concerts.map(this::toResponse);
    }

    public ConcertResponse getConcertDetail(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Concert not found"));

        List<TicketCategory> categories = ticketCategoryRepository.findByConcertIdOrderBySortOrder(id);
        return toResponseWithCategories(concert, categories);
    }

    public Map<Long, Integer> getAvailability(Long concertId) {
        concertRepository.findById(concertId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Concert not found"));

        List<TicketCategory> categories = ticketCategoryRepository.findByConcertIdOrderBySortOrder(concertId);
        return categories.stream()
                .collect(Collectors.toMap(TicketCategory::getId, TicketCategory::getAvailableQuantity));
    }

    @Transactional
    public ConcertResponse createConcert(CreateConcertRequest request, Long createdBy) {
        Concert concert = Concert.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(request.getVenue())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .status(Concert.ConcertStatus.DRAFT)
                .coverImageUrl(request.getCoverImageUrl())
                .createdBy(createdBy)
                .build();

        concert = concertRepository.save(concert);
        return toResponse(concert);
    }

    @Transactional
    public ConcertResponse publishConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Concert not found"));

        if (concert.getStatus() != Concert.ConcertStatus.DRAFT) {
            throw new BusinessException("INVALID_STATE_TRANSITION",
                    "Concert can only be published from DRAFT state");
        }

        List<TicketCategory> categories = ticketCategoryRepository.findByConcertIdOrderBySortOrder(id);
        if (categories.isEmpty()) {
            throw new BusinessException("NO_TICKET_CATEGORIES",
                    "Concert must have at least one ticket category before publishing");
        }

        concert.setStatus(Concert.ConcertStatus.PUBLISHED);
        concert = concertRepository.save(concert);
        return toResponse(concert);
    }

    @Transactional
    public ConcertResponse unpublishConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Concert not found"));

        if (concert.getStatus() != Concert.ConcertStatus.PUBLISHED) {
            throw new BusinessException("INVALID_STATE_TRANSITION",
                    "Only published concerts can be unpublished");
        }

        concert.setStatus(Concert.ConcertStatus.DRAFT);
        concert = concertRepository.save(concert);
        return toResponse(concert);
    }

    private ConcertResponse toResponse(Concert concert) {
        List<TicketCategory> categories = ticketCategoryRepository.findByConcertIdOrderBySortOrder(concert.getId());
        return toResponseWithCategories(concert, categories);
    }

    private ConcertResponse toResponseWithCategories(Concert concert, List<TicketCategory> categories) {
        List<ConcertResponse.TicketCategoryResponse> categoryResponses = categories.stream()
                .map(tc -> ConcertResponse.TicketCategoryResponse.builder()
                        .id(tc.getId())
                        .name(tc.getName())
                        .priceAmount(tc.getPriceAmount())
                        .priceCurrency(tc.getPriceCurrency())
                        .totalQuantity(tc.getTotalQuantity())
                        .availableQuantity(tc.getAvailableQuantity())
                        .build())
                .toList();

        return ConcertResponse.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .description(concert.getDescription())
                .venue(concert.getVenue())
                .startsAt(concert.getStartsAt())
                .endsAt(concert.getEndsAt())
                .status(concert.getStatus().name())
                .coverImageUrl(concert.getCoverImageUrl())
                .createdAt(concert.getCreatedAt())
                .ticketCategories(categoryResponses)
                .build();
    }
}
