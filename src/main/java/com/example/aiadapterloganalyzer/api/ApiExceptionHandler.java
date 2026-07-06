package com.example.aiadapterloganalyzer.api;

import com.example.aiadapterloganalyzer.agent.DiagnosisValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DiagnosisValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ApiErrorResponse handleDiagnosisValidation(DiagnosisValidationException exception) {
        return new ApiErrorResponse("diagnosis_validation_failed", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleRequestValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("request validation failed");
        return new ApiErrorResponse("request_validation_failed", message);
    }
}
