package com.humberto.b2b.ordermanagement.partner.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Partner to register")
public record CreatePartnerRequest(
        @Schema(description = "Total credit the partner can use for approved orders", example = "1000.00")
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal creditLimit
) {
}
