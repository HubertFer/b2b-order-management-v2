package com.humberto.b2b.ordermanagement.partner.api.dto;

import com.humberto.b2b.ordermanagement.partner.domain.Partner;
import com.humberto.b2b.ordermanagement.shared.api.MoneyFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record PartnerResponse(UUID id, BigDecimal creditLimit, BigDecimal usedCredit, BigDecimal availableCredit) {

    public static PartnerResponse from(Partner partner) {
        return new PartnerResponse(
                partner.id(),
                MoneyFormat.of(partner.creditLimit()),
                MoneyFormat.of(partner.usedCredit()),
                MoneyFormat.of(partner.availableCredit())
        );
    }
}
