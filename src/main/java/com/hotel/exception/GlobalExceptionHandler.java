package com.hotel.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequestException e, HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT,
                "Duplicate or invalid data (e.g. room number / email already exists)", request);
    }

    /** Bean validation on an @Valid @RequestBody argument. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        ErrorResponse error = newError(HttpStatus.BAD_REQUEST, "Validation failed", request);
        e.getBindingResult().getFieldErrors().forEach(f -> error.addFieldError(
                f.getField(),
                f.getRejectedValue() == null ? null : String.valueOf(f.getRejectedValue()),
                f.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors().forEach(g ->
                error.addFieldError(g.getObjectName(), null, g.getDefaultMessage()));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /** Bean validation on @Validated method params (path variables, request params). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException e,
                                                             HttpServletRequest request) {
        ErrorResponse error = newError(HttpStatus.BAD_REQUEST, "Validation failed", request);
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            error.addFieldError(
                    String.valueOf(v.getPropertyPath()),
                    v.getInvalidValue() == null ? null : String.valueOf(v.getInvalidValue()),
                    v.getMessage());
        }
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /** e.g. /api/rooms/abc where a Long is expected, or an unparseable date. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest request) {
        String type = e.getRequiredType() == null ? "the expected type" : e.getRequiredType().getSimpleName();
        return body(HttpStatus.BAD_REQUEST,
                String.format("Parameter '%s' has invalid value '%s'; expected %s", e.getName(), e.getValue(), type),
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException e,
                                                      HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST,
                String.format("Required parameter '%s' is missing", e.getParameterName()), request);
    }

    /** Malformed or empty JSON body, including values Jackson cannot map onto a field. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.debug("Unreadable request body on {}: {}", request.getRequestURI(), e.getMessage());
        if (e.getCause() instanceof InvalidFormatException) {
            InvalidFormatException cause = (InvalidFormatException) e.getCause();
            Class<?> target = cause.getTargetType();
            String expected = target != null && target.isEnum()
                    ? "one of " + Arrays.toString(target.getEnumConstants())
                    : "type " + (target == null ? "unknown" : target.getSimpleName());
            return body(HttpStatus.BAD_REQUEST, String.format("Invalid value '%s' for field '%s'; expected %s",
                    cause.getValue(), fieldPath(cause), expected), request);
        }
        return body(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    private static String fieldPath(InvalidFormatException e) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(ref.getFieldName());
        }
        return sb.length() == 0 ? "(unknown)" : sb.toString();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                          HttpServletRequest request) {
        return body(HttpStatus.METHOD_NOT_ALLOWED,
                String.format("Method %s is not supported for this endpoint", e.getMethod()), request);
    }

    /**
     * Unmapped URL. Reached only because spring.mvc.throw-exception-if-no-handler-found
     * is enabled, so 404s stay JSON instead of falling through to the default error page.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> noHandler(NoHandlerFoundException e, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND,
                String.format("No endpoint %s %s", e.getHttpMethod(), e.getRequestURL()), request);
    }

    /** Catch-all: log the stack trace server-side, return a generic message to the caller. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.", request);
    }

    private ResponseEntity<ErrorResponse> body(HttpStatus status, String message, HttpServletRequest request) {
        return new ResponseEntity<>(newError(status, message, request), status);
    }

    private ErrorResponse newError(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request == null ? null : request.getRequestURI());
    }
}
