package com.humberto.b2b.ordermanagement.order.domain;

public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(OrderStatus currentStatus, OrderStatus requestedStatus) {
        super("Cannot transition from " + currentStatus + " to " + requestedStatus);
    }
}
