package com.humberto.b2b.ordermanagement.shared.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<ValidationError> errors = exception.getBindingResult().getAllErrors().stream()
                .map(error -> new ValidationError(
                        error instanceof FieldError fieldError ? fieldError.getField() : null,
                        error.getDefaultMessage()))
                .toList();
        exception.getBody().setProperty("errors", errors);
        return handleExceptionInternal(exception, exception.getBody(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<ValidationError> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(
                                error instanceof FieldError fieldError
                                        ? fieldError.getField()
                                        : result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        exception.getBody().setProperty("errors", errors);
        return handleExceptionInternal(exception, exception.getBody(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        if (exception.getMostSpecificCause() instanceof UnrecognizedPropertyException unknownField) {
            String field = pathOf(unknownField);
            ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Unknown field: " + field);
            body.setProperty("errors", List.of(new ValidationError(field, "is not part of this request")));
            return handleExceptionInternal(exception, body, headers, status, request);
        }
        return super.handleHttpMessageNotReadable(exception, headers, status, request);
    }

    private static String pathOf(UnrecognizedPropertyException exception) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : exception.getPath()) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    record ValidationError(String field, String message) {
    }
}
