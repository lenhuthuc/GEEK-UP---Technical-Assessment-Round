package com.geekup.concert.unit;

import com.geekup.concert.common.exception.BusinessException;
import com.geekup.concert.voucher.*;
import com.geekup.concert.voucher.dto.VoucherPreviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @InjectMocks
    private VoucherService voucherService;

    private Voucher percentVoucher;
    private Voucher fixedVoucher;

    @BeforeEach
    void setUp() {
        percentVoucher = Voucher.builder()
                .id(1L)
                .code("SUMMER10")
                .type(Voucher.VoucherType.PERCENT)
                .value(BigDecimal.TEN)
                .minOrderAmount(BigDecimal.valueOf(1000000))
                .maxDiscountAmount(BigDecimal.valueOf(500000))
                .maxUses(100)
                .usedCount(0)
                .maxUsesPerUser(2)
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .status(Voucher.VoucherStatus.ACTIVE)
                .build();

        fixedVoucher = Voucher.builder()
                .id(2L)
                .code("FLAT200K")
                .type(Voucher.VoucherType.FIXED)
                .value(BigDecimal.valueOf(200000))
                .minOrderAmount(BigDecimal.valueOf(500000))
                .maxDiscountAmount(null)
                .maxUses(50)
                .usedCount(0)
                .maxUsesPerUser(1)
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .status(Voucher.VoucherStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldCalculatePercentDiscount() {
        BigDecimal subtotal = BigDecimal.valueOf(3000000);
        BigDecimal discount = voucherService.calculateDiscount(percentVoucher, subtotal);
        // 10% of 3,000,000 = 300,000
        assertEquals(BigDecimal.valueOf(300000), discount);
    }

    @Test
    void shouldCapPercentDiscountAtMax() {
        BigDecimal subtotal = BigDecimal.valueOf(10000000);
        BigDecimal discount = voucherService.calculateDiscount(percentVoucher, subtotal);
        // 10% of 10,000,000 = 1,000,000, but capped at 500,000
        assertEquals(BigDecimal.valueOf(500000), discount);
    }

    @Test
    void shouldCalculateFixedDiscount() {
        BigDecimal subtotal = BigDecimal.valueOf(1000000);
        BigDecimal discount = voucherService.calculateDiscount(fixedVoucher, subtotal);
        assertEquals(BigDecimal.valueOf(200000), discount);
    }

    @Test
    void shouldPreviewValidVoucher() {
        BigDecimal subtotal = BigDecimal.valueOf(3000000);
        when(voucherRepository.findByCode("SUMMER10")).thenReturn(Optional.of(percentVoucher));
        when(voucherUsageRepository.countByVoucherIdAndUserId(1L, 1L)).thenReturn(0L);

        VoucherPreviewResponse response = voucherService.previewDiscount("SUMMER10", subtotal, 1L);

        assertTrue(response.isValid());
        assertEquals(BigDecimal.valueOf(300000), response.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(2700000), response.getTotalAfterDiscount());
    }

    @Test
    void shouldRejectExpiredVoucher() {
        percentVoucher.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        when(voucherRepository.findByCode("SUMMER10")).thenReturn(Optional.of(percentVoucher));

        VoucherPreviewResponse response = voucherService.previewDiscount("SUMMER10", BigDecimal.valueOf(3000000), 1L);

        assertFalse(response.isValid());
    }

    @Test
    void shouldRejectExhaustedVoucher() {
        percentVoucher.setUsedCount(100);
        when(voucherRepository.findByCode("SUMMER10")).thenReturn(Optional.of(percentVoucher));

        VoucherPreviewResponse response = voucherService.previewDiscount("SUMMER10", BigDecimal.valueOf(3000000), 1L);

        assertFalse(response.isValid());
    }

    @Test
    void shouldRejectWhenUserLimitReached() {
        when(voucherRepository.findByCode("SUMMER10")).thenReturn(Optional.of(percentVoucher));
        when(voucherUsageRepository.countByVoucherIdAndUserId(1L, 1L)).thenReturn(2L);

        VoucherPreviewResponse response = voucherService.previewDiscount("SUMMER10", BigDecimal.valueOf(3000000), 1L);

        assertFalse(response.isValid());
    }

    @Test
    void shouldRejectBelowMinOrder() {
        when(voucherRepository.findByCode("SUMMER10")).thenReturn(Optional.of(percentVoucher));

        VoucherPreviewResponse response = voucherService.previewDiscount("SUMMER10", BigDecimal.valueOf(500000), 1L);

        assertFalse(response.isValid());
    }
}
