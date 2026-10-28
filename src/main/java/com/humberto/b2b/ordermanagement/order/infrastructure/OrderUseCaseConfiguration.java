package com.humberto.b2b.ordermanagement.order.infrastructure;

import com.humberto.b2b.ordermanagement.order.application.ApproveOrderService;
import com.humberto.b2b.ordermanagement.order.application.CancelOrderService;
import com.humberto.b2b.ordermanagement.order.application.ChangeOrderStatusService;
import com.humberto.b2b.ordermanagement.order.application.CreateOrderService;
import com.humberto.b2b.ordermanagement.order.application.GetOrderService;
import com.humberto.b2b.ordermanagement.order.application.SearchOrdersService;
import com.humberto.b2b.ordermanagement.order.application.port.out.DebitPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.OrderIdempotencyPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RecordOrderStatusChangePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RefundPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SearchOrdersPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class OrderUseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateOrderService createOrderService(PartnerCreditPort partnerCreditPort,
                                          SaveOrderPort saveOrderPort,
                                          FindOrderPort findOrderPort,
                                          OrderIdempotencyPort orderIdempotencyPort,
                                          Clock clock) {
        return new CreateOrderService(partnerCreditPort, saveOrderPort, findOrderPort, orderIdempotencyPort, clock);
    }

    @Bean
    GetOrderService getOrderService(FindOrderPort findOrderPort) {
        return new GetOrderService(findOrderPort);
    }

    @Bean
    SearchOrdersService searchOrdersService(SearchOrdersPort searchOrdersPort) {
        return new SearchOrdersService(searchOrdersPort);
    }

    @Bean
    ApproveOrderService approveOrderService(FindOrderForUpdatePort findOrderForUpdatePort,
                                            DebitPartnerCreditPort debitPartnerCreditPort,
                                            PartnerCreditPort partnerCreditPort,
                                            SaveOrderPort saveOrderPort,
                                            RecordOrderStatusChangePort recordOrderStatusChangePort,
                                            Clock clock) {
        return new ApproveOrderService(findOrderForUpdatePort, debitPartnerCreditPort, partnerCreditPort,
                saveOrderPort, recordOrderStatusChangePort, clock);
    }

    @Bean
    CancelOrderService cancelOrderService(FindOrderForUpdatePort findOrderForUpdatePort,
                                          RefundPartnerCreditPort refundPartnerCreditPort,
                                          SaveOrderPort saveOrderPort,
                                          RecordOrderStatusChangePort recordOrderStatusChangePort,
                                          Clock clock) {
        return new CancelOrderService(findOrderForUpdatePort, refundPartnerCreditPort, saveOrderPort,
                recordOrderStatusChangePort, clock);
    }

    @Bean
    ChangeOrderStatusService changeOrderStatusService(FindOrderForUpdatePort findOrderForUpdatePort,
                                                      SaveOrderPort saveOrderPort,
                                                      RecordOrderStatusChangePort recordOrderStatusChangePort,
                                                      Clock clock) {
        return new ChangeOrderStatusService(findOrderForUpdatePort, saveOrderPort, recordOrderStatusChangePort, clock);
    }
}
