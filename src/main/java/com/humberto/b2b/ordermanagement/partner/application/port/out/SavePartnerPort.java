package com.humberto.b2b.ordermanagement.partner.application.port.out;

import com.humberto.b2b.ordermanagement.partner.domain.Partner;

public interface SavePartnerPort {

    Partner save(Partner partner);
}
