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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Orders", description = "Creation, lookup and lifecycle of B2B orders")
@ApiResponse(responseCode = "400", description = "Invalid request",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
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
    @Operation(summary = "Creates an order",
            description = "Checks that the partner has enough available credit for the order total, without debiting it. "
                    + "Requests repeated with the same Idempotency-Key return the order created by the first one.")
    @ApiResponse(responseCode = "201", description = "Order created with status PENDENTE")
    @ApiResponse(responseCode = "200", description = "Order already created with this Idempotency-Key")
    @ApiResponse(responseCode = "422",
            description = "Partner not found, not enough available credit (with availableCredit and requestedAmount), "
                    + "or Idempotency-Key reused with a different request",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<OrderResponse> create(
            @Parameter(description = "Optional client-generated key, unique per partner (1 to 100 printable ASCII characters)",
                    example = "order-001")
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
    @Operation(summary = "Searches orders by status and creation period",
            description = "All filters are optional. The period includes createdFrom and excludes createdTo. "
                    + "Results are sorted from newest to oldest.")
    @ApiResponse(responseCode = "200", description = "Page of orders")
    OrderPageResponse search(
            @Parameter(description = "Only orders in this status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Created at or after this instant (ISO-8601, UTC)", example = "2026-10-01T00:00:00Z")
            @RequestParam(required = false) Instant createdFrom,
            @Parameter(description = "Created before this instant (ISO-8601, UTC)", example = "2026-11-01T00:00:00Z")
            @RequestParam(required = false) Instant createdTo,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 to 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(SearchOrdersService.MAX_PAGE_SIZE) int size) {
        OrderSearchCriteria criteria = new OrderSearchCriteria(status, createdFrom, createdTo);
        return OrderPageResponse.from(searchOrdersService.search(criteria, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets an order by id")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OrderResponse getById(@PathVariable UUID id) {
        return OrderResponse.from(getOrderService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approves a pending order", description = "Atomically debits the order total from the partner's credit.")
    @ApiResponse(responseCode = "200", description = "Order approved")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Order is not PENDENTE",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Not enough available credit (with availableCredit and requestedAmount)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OrderResponse approve(@PathVariable UUID id) {
        return OrderResponse.from(approveOrderService.approve(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancels an order",
            description = "Allowed up to EM_PROCESSAMENTO. If the order was approved, its total is refunded to the partner.")
    @ApiResponse(responseCode = "200", description = "Order cancelled")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Order can no longer be cancelled",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OrderResponse cancel(@PathVariable UUID id) {
        return OrderResponse.from(cancelOrderService.cancel(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Updates the order status",
            description = "Accepts any transition allowed by the lifecycle. APROVADO debits credit like POST /approve "
                    + "and CANCELADO refunds it when needed like POST /cancel.")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Transition not allowed",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Not enough available credit for approval",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeOrderStatusRequest request) {
        Order order = switch (request.status()) {
            case APROVADO -> approveOrderService.approve(id);
            case CANCELADO -> cancelOrderService.cancel(id);
            default -> changeOrderStatusService.changeStatus(id, request.status());
        };
        return OrderResponse.from(order);
    }
}
