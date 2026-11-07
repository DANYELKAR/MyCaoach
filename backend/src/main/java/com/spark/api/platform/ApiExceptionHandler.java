package com.spark.api.platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class,
    MaxUploadSizeExceededException.class
  })
  ResponseEntity<ApiError> validation(Exception error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Invalid request.", request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> notFound(NoResourceFoundException error, HttpServletRequest request) {
    return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found.", request);
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiError> business(BusinessException error, HttpServletRequest request) {
    return ResponseEntity.status(error.status)
        .body(new ApiError(error.code, error.getMessage(), RequestIdFilter.requestId(request)));
  }

  @ExceptionHandler({
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  ResponseEntity<ApiError> invalidParameter(Exception error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "VALIDATION_ERROR",
        "Invalid request parameters.",
        request);
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> method(Exception error, HttpServletRequest request) {
    return response(
        HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed.", request);
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiError> media(Exception error, HttpServletRequest request) {
    return response(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "UNSUPPORTED_MEDIA_TYPE",
        "Unsupported content type.",
        request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception error, HttpServletRequest request) {
    // Exception messages may contain input or credentials. Log only type and correlation id.
    LOG.error(
        "request_failed type={} requestId={}",
        error.getClass().getSimpleName(),
        RequestIdFilter.requestId(request));
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "An unexpected error occurred.",
        request);
  }

  private ResponseEntity<ApiError> response(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(new ApiError(code, message, RequestIdFilter.requestId(request)));
  }
}
