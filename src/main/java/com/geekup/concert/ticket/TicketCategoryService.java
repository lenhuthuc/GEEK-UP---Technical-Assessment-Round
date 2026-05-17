package com.geekup.concert.ticket;

import com.geekup.concert.common.exception.BusinessException;
import com.geekup.concert.concert.Concert;
import com.geekup.concert.concert.ConcertRepository;
import com.geekup.concert.ticket.dto.CreateTicketCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketCategoryService {

    private final TicketCategoryRepository ticketCategoryRepository;
    private final ConcertRepository concertRepository;

    @Transactional
    public TicketCategory createCategory(CreateTicketCategoryRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Concert not found"));

        if (concert.getStatus() != Concert.ConcertStatus.DRAFT) {
            throw new BusinessException("INVALID_STATE_TRANSITION",
                    "Ticket categories can only be added to DRAFT concerts");
        }

        TicketCategory category = TicketCategory.builder()
                .concertId(request.getConcertId())
                .name(request.getName())
                .priceAmount(request.getPriceAmount())
                .priceCurrency(request.getPriceCurrency())
                .totalQuantity(request.getTotalQuantity())
                .availableQuantity(request.getTotalQuantity())
                .sortOrder(request.getSortOrder())
                .build();

        return ticketCategoryRepository.save(category);
    }
}
