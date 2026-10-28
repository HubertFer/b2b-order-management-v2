package com.humberto.b2b.ordermanagement.partner.application.port.out;

import com.humberto.b2b.ordermanagement.partner.domain.Partner;

import java.util.Optional;
import java.util.UUID;

public interface LoadPartnerPort {

    Optional<Partner> findById(UUID partnerId);
}
