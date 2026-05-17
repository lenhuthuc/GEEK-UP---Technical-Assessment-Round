package com.geekup.concert.voucher;

import com.geekup.concert.voucher.dto.VoucherPreviewRequest;
import com.geekup.concert.voucher.dto.VoucherPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vouchers (Customer)", description = "Customer voucher preview API")
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping("/preview")
    @Operation(summary = "Preview discount when a voucher is applied (no commit)")
    public ResponseEntity<VoucherPreviewResponse> previewDiscount(
            @Valid @RequestBody VoucherPreviewRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                voucherService.previewDiscount(request.getVoucherCode(), request.getSubtotal(), userId));
    }
}
