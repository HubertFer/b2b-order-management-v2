package com.humberto.b2b.ordermanagement.order.api.controller;

import com.humberto.b2b.ordermanagement.order.api.dto.ChangeOrderStatusRequest;
import com.humberto.b2b.ordermanagement.order.api.dto.CreateOrderRequest;
import com.humberto.b2b.ordermanagement.order.api.dto.OrderPageResponse;
import com.humberto.b2b.ordermanagement.order.api.dto.OrderResponse;
import com.humberto.b2b.ordermanagement.order.application.ApproveOrderService;
import com.humberto.b2b.ordermanagement.order.application.CancelOrderService;
import com.humberto.b2b.ordermanagement.order.application.ChangeOrderStatusService;
import com.humberto.b2b.ordermanagement.order.application.CreateOrderService;
import com.humberto.b2b.ordermanagement.order.application.GetOrderService;
import com.humberto.b2b.ordermanagement.order.application.OrderCreation;
import com.humberto.b2b.ordermanagement.order.application.OrderSearchCriteria;
import com.humberto.b2b.ordermanagement.order.application.SearchOrdersService;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final CancelOrderService cancelOrderService;
    private final ChangeOrderStatusService changeOrderStatusService;

    OrderController(CreateOrderService createOrderService,
                    GetOrderService getOrderService,
                    SearchOrdersService searchOrdersService,
                    ApproveOrderService approveOrderService,
                    CancelOrderService cancelOrderService,
                    ChangeOrderStatusService changeOrderStatusService) {
        this.createOrderService = createOrderService;
        this.getOrderService = getOrderService;
        this.searchOrdersService = searchOrdersService;
        this.approveOrderService = approveOrderService;
        this.cancelOrderService = cancelOrderService;
        this.changeOrderStatusService = changeOrderStatusService;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(
            @RequestHeader(name = "Idempotency-Key", required = false)
            @Size(min = 1, max = 100) @Pattern(regexp = "[\\x21-\\x7E]+") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderCreation creation = createOrderService.create(request.partnerId(), request.toDomainItems(), idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creation.order().id())
                .toUri();
        ResponseEntity.BodyBuilder response = creation.replayed()
                ? ResponseEntity.ok().location(location)
                : ResponseEntity.created(location);
        return response.body(OrderResponse.from(creation.order()));
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

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable UUID id) {
        return OrderResponse.from(cancelOrderService.cancel(id));
    }

    @PatchMapping("/{id}/status")
    OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeOrderStatusRequest request) {
        Order order = switch (request.status()) {
            case APROVADO -> approveOrderService.approve(id);
            case CANCELADO -> cancelOrderService.cancel(id);
            default -> changeOrderStatusService.changeStatus(id, request.status());
        };
        return OrderResponse.from(order);
    }
}
