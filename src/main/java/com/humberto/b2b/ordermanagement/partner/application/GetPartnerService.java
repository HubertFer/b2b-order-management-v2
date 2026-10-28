package com.humberto.b2b.ordermanagement.partner.application;

import com.humberto.b2b.ordermanagement.partner.application.port.out.LoadPartnerPort;
import com.humberto.b2b.ordermanagement.partner.domain.Partner;

import java.util.UUID;

public class GetPartnerService {

    private final LoadPartnerPort loadPartnerPort;

    public GetPartnerService(LoadPartnerPort loadPartnerPort) {
        this.loadPartnerPort = loadPartnerPort;
    }

    public Partner getById(UUID partnerId) {
        return loadPartnerPort.findById(partnerId).orElseThrow(() -> new PartnerNotFoundException(partnerId));
    }
}
