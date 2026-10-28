package com.humberto.b2b.ordermanagement.order.api;

import com.humberto.b2b.ordermanagement.order.application.ApproveOrderService;
import com.humberto.b2b.ordermanagement.order.application.CreateOrderService;
import com.humberto.b2b.ordermanagement.order.application.GetOrderService;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final CreateOrderService createOrderService;
    private final GetOrderService getOrderService;
    private final ApproveOrderService approveOrderService;

    OrderController(CreateOrderService createOrderService,
                    GetOrderService getOrderService,
                    ApproveOrderService approveOrderService) {
        this.createOrderService = createOrderService;
        this.getOrderService = getOrderService;
        this.approveOrderService = approveOrderService;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = createOrderService.create(request.partnerId(), request.toDomainItems());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    OrderResponse getById(@PathVariable UUID id) {
        return OrderResponse.from(getOrderService.getById(id));
    }

    @PostMapping("/{id}/approve")
    OrderResponse approve(@PathVariable UUID id) {
        return OrderResponse.from(approveOrderService.approve(id));
    }
}
