package com.aerosaga;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** One place that turns exceptions into clean JSON errors. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Abort, storm or query for a drone that has no (running) mission. */
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<ApiError> notFound(WorkflowNotFoundException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "No mission found for this drone", req);
    }

    /** Temporal returned an error: NOT_FOUND means the workflow is gone, anything else means the server is unreachable. */
    @ExceptionHandler(WorkflowServiceException.class)
    public ResponseEntity<ApiError> serviceError(WorkflowServiceException e, HttpServletRequest req) {
        Throwable cause = e.getCause();
        if (cause instanceof StatusRuntimeException
                && ((StatusRuntimeException) cause).getStatus().getCode() == Status.Code.NOT_FOUND) {
            return build(HttpStatus.NOT_FOUND, "No running mission found for this drone", req);
        }
        log.warn("Temporal call failed: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Temporal server is unavailable. Is docker compose running?", req);
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ApiError> grpcError(StatusRuntimeException e, HttpServletRequest req) {
        log.warn("gRPC call failed: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Temporal server is unavailable. Is docker compose running?", req);
    }

    /** Broken JSON in a request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> badBody(HttpMessageNotReadableException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing JSON body", req);
    }

    /** For example ?count=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParam(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'", req);
    }

    /** Anything unexpected. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> fallback(Exception e, HttpServletRequest req) {
        log.error("Unhandled error on {}", req.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
