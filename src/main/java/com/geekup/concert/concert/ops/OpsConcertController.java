package com.geekup.concert.concert.ops;

import com.geekup.concert.concert.ConcertService;
import com.geekup.concert.concert.dto.ConcertResponse;
import com.geekup.concert.concert.dto.CreateConcertRequest;
import com.geekup.concert.ticket.TicketCategory;
import com.geekup.concert.ticket.TicketCategoryService;
import com.geekup.concert.ticket.dto.CreateTicketCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ops/concerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@Tag(name = "Concerts (Operations)", description = "Operator/Admin concert management APIs")
public class OpsConcertController {

    private final ConcertService concertService;
    private final TicketCategoryService ticketCategoryService;

    @PostMapping
    @Operation(summary = "Create a concert in DRAFT state")
    public ResponseEntity<ConcertResponse> createConcert(
            @Valid @RequestBody CreateConcertRequest request,
            Authentication auth) {
        Long createdBy = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(concertService.createConcert(request, createdBy));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish the concert (DRAFT -> PUBLISHED)")
    public ResponseEntity<ConcertResponse> publishConcert(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.publishConcert(id));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish the concert (pause sales)")
    public ResponseEntity<ConcertResponse> unpublishConcert(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.unpublishConcert(id));
    }

    @PostMapping("/{id}/ticket-categories")
    @Operation(summary = "Add a ticket category to a concert")
    public ResponseEntity<TicketCategory> addTicketCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateTicketCategoryRequest request) {
        request.setConcertId(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketCategoryService.createCategory(request));
    }
}
