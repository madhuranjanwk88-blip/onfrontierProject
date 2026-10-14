package com.hotel.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> badRequest(BadRequestException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException e, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "Duplicate or invalid data (e.g. room number / email already exists)", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e, HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> sb.append(f.getField()).append(": ").append(f.getDefaultMessage()).append("; "));
        return body(HttpStatus.BAD_REQUEST, sb.toString(), req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraint(ConstraintViolationException e, HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        e.getConstraintViolations()
                .forEach(v -> sb.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("; "));
        return body(HttpStatus.BAD_REQUEST, sb.toString(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "Malformed JSON request body", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "Missing required parameter '" + e.getParameterName() + "'", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> mediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String msg, HttpServletRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", LocalDateTime.now().toString());
        m.put("status", status.value());
        m.put("error", msg);
        m.put("path", req.getRequestURI());
        return new ResponseEntity<>(m, status);
    }
}
