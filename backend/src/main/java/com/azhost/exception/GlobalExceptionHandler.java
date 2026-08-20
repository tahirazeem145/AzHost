package com.azhost.exception;

import com.azhost.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final org.springframework.core.env.Environment environment;

    public GlobalExceptionHandler(org.springframework.core.env.Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProjectNotFound(ProjectNotFoundException ex, HttpServletRequest request) {
        logger.warn("Project not found: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProjectSourceNotAvailableException.class)
    public ResponseEntity<ErrorResponseDto> handleProjectSourceNotAvailable(ProjectSourceNotAvailableException ex, HttpServletRequest request) {
        logger.warn("Project source not available for analysis: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BuildQueueFullException.class)
    public ResponseEntity<ErrorResponseDto> handleBuildQueueFull(BuildQueueFullException ex, HttpServletRequest request) {
        logger.warn("Build queue full: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(BuildAlreadyInProgressException.class)
    public ResponseEntity<ErrorResponseDto> handleBuildAlreadyInProgress(BuildAlreadyInProgressException ex, HttpServletRequest request) {
        logger.warn("Build already in progress: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BuildNotSuccessfulException.class)
    public ResponseEntity<ErrorResponseDto> handleBuildNotSuccessful(BuildNotSuccessfulException ex, HttpServletRequest request) {
        logger.warn("Build not successful for deployment: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DeploymentAlreadyInProgressException.class)
    public ResponseEntity<ErrorResponseDto> handleDeploymentAlreadyInProgress(DeploymentAlreadyInProgressException ex, HttpServletRequest request) {
        logger.warn("Deployment already in progress: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DeploymentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleDeploymentNotFound(DeploymentNotFoundException ex, HttpServletRequest request) {
        logger.warn("Deployment not found: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "DEPLOYMENT_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(BuildEngineUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleBuildEngineUnavailable(BuildEngineUnavailableException ex, HttpServletRequest request) {
        logger.error("Build engine unavailable: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(BuildNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleBuildNotFound(BuildNotFoundException ex, HttpServletRequest request) {
        logger.warn("Build not found: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "BUILD_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateProjectSlugException.class)

    public ResponseEntity<ErrorResponseDto> handleDuplicateProjectSlug(DuplicateProjectSlugException ex, HttpServletRequest request) {
        logger.warn("Duplicate project slug conflict: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));

        logger.warn("Validation error processing request path {}: {}", request.getRequestURI(), validationMessage);
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                validationMessage.isBlank() ? "Validation failed for request parameters" : validationMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(com.azhost.github.exception.GitHubAuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubAuthentication(com.azhost.github.exception.GitHubAuthenticationException ex, HttpServletRequest request) {
        logger.warn("GitHub authentication failed: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(com.azhost.github.exception.GitHubAuthorizationException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubAuthorization(com.azhost.github.exception.GitHubAuthorizationException ex, HttpServletRequest request) {
        logger.warn("GitHub authorization failed: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(com.azhost.github.exception.GitHubRepositoryNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubRepositoryNotFound(com.azhost.github.exception.GitHubRepositoryNotFoundException ex, HttpServletRequest request) {
        logger.warn("GitHub repository not found: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(com.azhost.github.exception.GitHubConnectionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubConnectionNotFound(com.azhost.github.exception.GitHubConnectionNotFoundException ex, HttpServletRequest request) {
        logger.warn("GitHub connection not found: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(com.azhost.github.exception.GitHubSourceAcquisitionException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubSourceAcquisition(com.azhost.github.exception.GitHubSourceAcquisitionException ex, HttpServletRequest request) {
        logger.warn("GitHub source acquisition failed: {}", ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception processing request path: {}", request.getRequestURI(), ex);
        
        java.util.List<String> activeProfiles = java.util.Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("prod")) {
            String reqId = org.slf4j.MDC.get("requestId");
            java.util.Map<String, Object> error = java.util.Map.of(
                "code", "INTERNAL_ERROR",
                "message", "An unexpected error occurred.",
                "requestId", reqId != null ? reqId : "unknown"
            );
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

