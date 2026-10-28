package com.humberto.b2b.ordermanagement.partner.infrastructure;

import com.humberto.b2b.ordermanagement.partner.application.CreatePartnerService;
import com.humberto.b2b.ordermanagement.partner.application.GetPartnerService;
import com.humberto.b2b.ordermanagement.partner.application.port.out.LoadPartnerPort;
import com.humberto.b2b.ordermanagement.partner.application.port.out.SavePartnerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PartnerUseCaseConfiguration {

    @Bean
    CreatePartnerService createPartnerService(SavePartnerPort savePartnerPort) {
        return new CreatePartnerService(savePartnerPort);
    }

    @Bean
    GetPartnerService getPartnerService(LoadPartnerPort loadPartnerPort) {
        return new GetPartnerService(loadPartnerPort);
    }
}
