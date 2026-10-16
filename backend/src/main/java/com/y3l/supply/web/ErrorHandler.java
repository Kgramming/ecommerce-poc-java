package com.y3l.supply.web;

import com.y3l.supply.dto.Dtos.ErrorBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mirrors FastAPI's {"detail": "..."} error shape. */
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApi(ApiException e) {
        return ResponseEntity.status(e.status()).body(new ErrorBody(e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleBadJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorBody("invalid request body"));
    }
}
