package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.DebitPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

public class ApproveOrderService {

    private final FindOrderForUpdatePort findOrderForUpdatePort;
    private final DebitPartnerCreditPort debitPartnerCreditPort;
    private final PartnerCreditPort partnerCreditPort;
    private final SaveOrderPort saveOrderPort;
    private final Clock clock;

    public ApproveOrderService(FindOrderForUpdatePort findOrderForUpdatePort,
                               DebitPartnerCreditPort debitPartnerCreditPort,
                               PartnerCreditPort partnerCreditPort,
                               SaveOrderPort saveOrderPort,
                               Clock clock) {
        this.findOrderForUpdatePort = findOrderForUpdatePort;
        this.debitPartnerCreditPort = debitPartnerCreditPort;
        this.partnerCreditPort = partnerCreditPort;
        this.saveOrderPort = saveOrderPort;
        this.clock = clock;
    }

    @Transactional
    public Order approve(UUID orderId) {
        Order order = findOrderForUpdatePort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Order approvedOrder = order.transitionTo(OrderStatus.APROVADO, clock.instant());
        if (!debitPartnerCreditPort.debitIfAvailable(order.partnerId(), order.total())) {
            BigDecimal availableCredit = partnerCreditPort.findAvailableCredit(order.partnerId())
                    .orElse(BigDecimal.ZERO);
            throw new InsufficientOrderCreditException(order.partnerId(), availableCredit, order.total());
        }
        return saveOrderPort.save(approvedOrder);
    }
}
