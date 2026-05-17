package com.geekup.concert.concert;

import com.geekup.concert.concert.dto.ConcertResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
@Tag(name = "Concerts (Public)", description = "Public concert browsing APIs")
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    @Operation(summary = "List published concerts with pagination")
    public ResponseEntity<Page<ConcertResponse>> listConcerts(
            @RequestParam(required = false, defaultValue = "false") boolean upcomingOnly,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ConcertResponse> concerts = upcomingOnly
                ? concertService.listUpcomingConcerts(pageable)
                : concertService.listPublishedConcerts(pageable);
        return ResponseEntity.ok(concerts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get concert detail with ticket categories")
    public ResponseEntity<ConcertResponse> getConcertDetail(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getConcertDetail(id));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Get remaining ticket counts per category")
    public ResponseEntity<Map<Long, Integer>> getAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getAvailability(id));
    }
}
