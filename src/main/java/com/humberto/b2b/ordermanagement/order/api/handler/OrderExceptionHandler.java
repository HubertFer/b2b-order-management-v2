package com.humberto.b2b.ordermanagement.order.api.handler;

import com.humberto.b2b.ordermanagement.order.api.controller.OrderController;
import com.humberto.b2b.ordermanagement.order.application.IdempotencyKeyReusedException;
import com.humberto.b2b.ordermanagement.order.application.InsufficientOrderCreditException;
import com.humberto.b2b.ordermanagement.order.application.OrderNotFoundException;
import com.humberto.b2b.ordermanagement.order.application.PartnerNotFoundException;
import com.humberto.b2b.ordermanagement.order.domain.InvalidOrderTransitionException;
import com.humberto.b2b.ordermanagement.shared.api.MoneyFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OrderController.class)
class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidOrderTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({PartnerNotFoundException.class, IdempotencyKeyReusedException.class})
    ProblemDetail handleUnprocessable(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(InsufficientOrderCreditException.class)
    ProblemDetail handleInsufficientCredit(InsufficientOrderCreditException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        problem.setProperty("availableCredit", MoneyFormat.of(exception.availableCredit()));
        problem.setProperty("requestedAmount", MoneyFormat.of(exception.requestedAmount()));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
