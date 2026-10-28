package com.humberto.b2b.ordermanagement.partner.application;

import com.humberto.b2b.ordermanagement.partner.application.port.out.SavePartnerPort;
import com.humberto.b2b.ordermanagement.partner.domain.Partner;

import java.math.BigDecimal;
import java.util.UUID;

public class CreatePartnerService {

    private final SavePartnerPort savePartnerPort;

    public CreatePartnerService(SavePartnerPort savePartnerPort) {
        this.savePartnerPort = savePartnerPort;
    }

    public Partner create(BigDecimal creditLimit) {
        return savePartnerPort.save(Partner.create(UUID.randomUUID(), creditLimit));
    }
}
