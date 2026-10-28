package com.humberto.b2b.ordermanagement.partner.application;

import java.util.UUID;

public class PartnerNotFoundException extends RuntimeException {

    public PartnerNotFoundException(UUID partnerId) {
        super("Partner not found: " + partnerId);
    }
}
