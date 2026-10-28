package com.humberto.b2b.ordermanagement.order.api.dto;

import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Target status of the order")
public record ChangeOrderStatusRequest(
        @Schema(description = "PENDENTE -> APROVADO or CANCELADO; APROVADO -> EM_PROCESSAMENTO or CANCELADO; "
                + "EM_PROCESSAMENTO -> ENVIADO or CANCELADO; ENVIADO -> ENTREGUE", example = "APROVADO")
        @NotNull OrderStatus status
) {
}
