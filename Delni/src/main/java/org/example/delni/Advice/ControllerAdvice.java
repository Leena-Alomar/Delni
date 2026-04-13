package org.example.delni.Advice;

import jakarta.validation.ConstraintViolationException;
import org.example.delni.API.ApiException;
import org.example.delni.API.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLIntegrityConstraintViolationException;

@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ControllerAdvice.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> apiException(ApiException apiException) {
        return ResponseEntity.status(400).body(new ApiResponse(apiException.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(new ApiResponse("HTTP method is not supported for this endpoint"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> httpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(400).body(new ApiResponse("Malformed request body"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> dataIntegrityViolationException(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(new ApiResponse("Request conflicts with existing data"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldError() != null
                ? exception.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        return ResponseEntity.status(400).body(new ApiResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> constraintViolationException(ConstraintViolationException exception) {
        return ResponseEntity.status(400).body(new ApiResponse(exception.getMessage()));
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<?> sqlIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException exception) {
        return ResponseEntity.status(409).body(new ApiResponse("Request conflicts with existing data"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(400).body(new ApiResponse("Invalid request parameter type"));
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<?> invalidDataAccessResourceUsageException(InvalidDataAccessResourceUsageException exception) {
        log.warn("Data access usage error", exception);
        return ResponseEntity.status(400).body(new ApiResponse("Request could not be processed"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> httpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(415).body(new ApiResponse("Media type is not supported"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParams(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(400).body(new ApiResponse(exception.getParameterName() + " parameter is missing"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<?> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return ResponseEntity.status(406).body(new ApiResponse("Requested media type not acceptable"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNoHandler(NoHandlerFoundException exception) {
        return ResponseEntity.status(404).body(new ApiResponse("Endpoint not found"));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(TypeMismatchException exception) {
        return ResponseEntity.status(400).body(new ApiResponse("Invalid request parameter type"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<?> handleMissingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.status(400).body(new ApiResponse("Missing header: " + exception.getHeaderName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception exception) {
        log.error("Unhandled server error", exception);
        return ResponseEntity.status(500).body(new ApiResponse("Unexpected server error"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException exception) {
        log.warn("Authentication error", exception);
        return ResponseEntity.status(401).body(new ApiResponse("Authentication failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(403).body(new ApiResponse("You do not have permission to access this resource"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException exception) {
        log.warn("Security error", exception);
        return ResponseEntity.status(403).body(new ApiResponse("Security error"));
    }
}
