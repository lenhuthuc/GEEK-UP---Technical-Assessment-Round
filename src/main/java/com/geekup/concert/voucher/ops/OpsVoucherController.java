package com.geekup.concert.voucher.ops;

import com.geekup.concert.voucher.Voucher;
import com.geekup.concert.voucher.VoucherService;
import com.geekup.concert.voucher.dto.CreateVoucherRequest;
import com.geekup.concert.voucher.dto.VoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ops/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
@Tag(name = "Vouchers (Operations)", description = "Operator/Admin voucher management APIs")
public class OpsVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create a voucher campaign")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(voucherService.createVoucher(request));
    }

    @GetMapping
    @Operation(summary = "List vouchers with search and filter")
    public ResponseEntity<Page<VoucherResponse>> listVouchers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Voucher.VoucherStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(voucherService.listVouchers(code, status, pageable));
    }
}
