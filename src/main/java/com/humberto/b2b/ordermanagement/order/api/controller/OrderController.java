package com.humberto.b2b.ordermanagement.order.api.controller;

import com.humberto.b2b.ordermanagement.order.api.dto.CreateOrderRequest;
import com.humberto.b2b.ordermanagement.order.api.dto.OrderPageResponse;
import com.humberto.b2b.ordermanagement.order.api.dto.OrderResponse;
import com.humberto.b2b.ordermanagement.order.application.ApproveOrderService;
import com.humberto.b2b.ordermanagement.order.application.CreateOrderService;
import com.humberto.b2b.ordermanagement.order.application.GetOrderService;
import com.humberto.b2b.ordermanagement.order.application.OrderSearchCriteria;
import com.humberto.b2b.ordermanagement.order.application.SearchOrdersService;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CreateOrderService createOrderService;
    private final GetOrderService getOrderService;
    private final SearchOrdersService searchOrdersService;
    private final ApproveOrderService approveOrderService;

    OrderController(CreateOrderService createOrderService,
                    GetOrderService getOrderService,
                    SearchOrdersService searchOrdersService,
                    ApproveOrderService approveOrderService) {
        this.createOrderService = createOrderService;
        this.getOrderService = getOrderService;
        this.searchOrdersService = searchOrdersService;
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

    @GetMapping
    OrderPageResponse search(@RequestParam(required = false) OrderStatus status,
                             @RequestParam(required = false) Instant createdFrom,
                             @RequestParam(required = false) Instant createdTo,
                             @RequestParam(defaultValue = "0") @Min(0) int page,
                             @RequestParam(defaultValue = "20") @Min(1) @Max(SearchOrdersService.MAX_PAGE_SIZE) int size) {
        OrderSearchCriteria criteria = new OrderSearchCriteria(status, createdFrom, createdTo);
        return OrderPageResponse.from(searchOrdersService.search(criteria, page, size));
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
