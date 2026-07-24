package com.appsdeveloperblog.reactive.ws.users.infrastructure;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<ErrorResponse> handleDuplicateKeyException(DuplicateKeyException exception) {
        return Mono.just(
                ErrorResponse.builder(
                        exception,
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                ).build()
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<ErrorResponse> handleGeneralException(Exception exception) {
        return Mono.just(
                ErrorResponse.builder(
                        exception,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        exception.getMessage()
                ).build()
        );
    }

    // Catch Validation errors (through WebExchangeBindException, which is thrown upon validation violations)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ErrorResponse> handleWebExchangeBindException(WebExchangeBindException exception) {

        // Custom error message
        String errorMessage = exception.getBindingResult().getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return Mono.just(
                ErrorResponse.builder(
                        exception,
                        HttpStatus.BAD_REQUEST,
                        errorMessage
                ).build()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<ErrorResponse> handleBadCredentialsException(BadCredentialsException exception) {
        return Mono.just(ErrorResponse.builder(
                exception,
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
                ).build()
        );
    }
}
