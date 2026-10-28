package com.humberto.b2b.ordermanagement.partner.api.handler;

import com.humberto.b2b.ordermanagement.partner.api.controller.PartnerController;
import com.humberto.b2b.ordermanagement.partner.application.PartnerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PartnerController.class)
class PartnerExceptionHandler {

    @ExceptionHandler(PartnerNotFoundException.class)
    ProblemDetail handlePartnerNotFound(PartnerNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
