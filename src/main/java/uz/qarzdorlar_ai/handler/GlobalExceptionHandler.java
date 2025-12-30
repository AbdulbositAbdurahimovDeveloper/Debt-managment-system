package uz.qarzdorlar_ai.handler;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uz.qarzdorlar_ai.exception.*;
import uz.qarzdorlar_ai.payload.errors.ErrorDTO;
import uz.qarzdorlar_ai.payload.errors.FieldErrorDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * A centralized global exception handler for the entire application.
 * This class uses {@code @RestControllerAdvice} to intercept exceptions thrown from any controller
 * or filter chain component and translates them into a consistent, structured JSON response
 * wrapped in a {@link ResponseDTO}. This approach simplifies client-side error handling and
 * provides clear, secure, and actionable feedback for all types of errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // === JWT EXCEPTION HANDLERS ===

    /**
     * Handles {@link ExpiredJwtException}, which is thrown when a JWT has passed its expiration time.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ResponseDTO<Object>> handleExpiredJwtException() {
        log.warn("Attempted to use an expired JWT token.");
        ErrorDTO error = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), "Token has expired. Please log in again.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link SignatureException}, indicating that the JWT's signature does not match.
     * This suggests the token may have been tampered with.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status.
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ResponseDTO<Object>> handleSignatureException() {
        log.warn("JWT signature validation failed. The token may have been tampered with.");
        ErrorDTO error = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT signature. Token might be corrupted or tampered.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link MalformedJwtException}, thrown for incorrectly structured JWTs.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status.
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ResponseDTO<Object>> handleMalformedJwtException() {
        log.warn("Malformed JWT token received.");
        ErrorDTO error = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), "Malformed JWT token.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles generic {@link JwtException} for any other JWT-related issues not caught by more specific handlers.
     * @param e The caught JwtException.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ResponseDTO<Object>> handleGenericJwtException(JwtException e) {
        log.warn("An unspecified JWT error occurred: {}", e.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), "An issue occurred with the provided token.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.UNAUTHORIZED);
    }

    // === SECURITY EXCEPTION HANDLERS ===

    /**
     * Handles {@link BadCredentialsException}, thrown for incorrect login attempts.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status.
     */
    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<ResponseDTO<Object>> handleBadCredentials() {
        log.warn("Authentication failed due to bad credentials for a user.");
        ErrorDTO error = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link AccessDeniedException}, which occurs when an authenticated user
     * attempts to access a resource they do not have permission for.
     * @param ex The caught AccessDeniedException.
     * @return A {@link ResponseEntity} with a 403 Forbidden status.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseDTO<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access Denied: A user attempted an unauthorized action. Message: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.FORBIDDEN.value(), "Access Denied. You do not have sufficient permissions to perform this action.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.FORBIDDEN);
    }

    // === STANDARD HTTP & VALIDATION EXCEPTION HANDLERS ===

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ResponseDTO<Object>> handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid token exception: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link MethodArgumentNotValidException}, which is thrown when DTO validation fails.
     * @param ex The exception thrown when validation fails.
     * @return A {@link ResponseEntity} with a 400 Bad Request status and detailed field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldErrorDTO> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.add(new FieldErrorDTO(fieldError.getField(), fieldError.getDefaultMessage()))
        );
        log.warn("Validation failed for request: {}", fieldErrors);
        ErrorDTO error = new ErrorDTO(HttpStatus.BAD_REQUEST.value(), "Validation failed. Please check your input.", fieldErrors);
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles cases where the request body is missing or malformed.
     * @param ex The caught HttpMessageNotReadableException.
     * @return A {@link ResponseEntity} with a 400 Bad Request status.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Failed to read request body: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.BAD_REQUEST.value(), "Request body is missing or malformed.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles cases where a requested endpoint (URL) does not exist.
     * @param ex The caught NoResourceFoundException.
     * @return A {@link ResponseEntity} with a 404 Not Found status.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseDTO<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        String message = String.format("Resource not found for %s request to '%s'.", ex.getHttpMethod(), ex.getResourcePath());
        log.warn(message);
        ErrorDTO error = new ErrorDTO(HttpStatus.NOT_FOUND.value(), message);
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.NOT_FOUND);
    }

    // === CUSTOM APPLICATION EXCEPTION HANDLERS ===

    /**
     * Handles cases where an entity is not found in the database.
     * @param ex Your custom {@link EntityNotFoundException}.
     * @return A {@link ResponseEntity} with a 404 Not Found status.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseDTO<Object>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles generic bad request errors initiated by business logic.
     * This is useful for cases where input is syntactically valid but semantically incorrect.
     * @param ex Your custom {@link BadRequestException}.
     * @return A {@link ResponseEntity} with a 400 Bad Request status.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseDTO<Object>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles rate limiting or throttling errors.
     * This is triggered when a user or IP address makes too many requests in a given timeframe.
     * @param ex Your custom {@link TooManyRequestsException}.
     * @return A {@link ResponseEntity} with a 429 Too Many Requests status.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ResponseDTO<Object>> handleTooManyRequests(TooManyRequestsException ex) {
        log.warn("Rate limiting triggered: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.TOO_MANY_REQUESTS);
    }



    /**
     * Handles data conflict errors, like a duplicate username or email.
     * @param ex Your custom {@link DataConflictException}.
     * @return A {@link ResponseEntity} with a 409 Conflict status.
     */
    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<ResponseDTO<Object>> handleDataConflict(DataConflictException ex) {
        log.warn("Data conflict: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.CONFLICT);
    }

    // === FILE UPLOAD EXCEPTION HANDLER ===

    /**
     * Handles exceptions thrown when an uploaded file exceeds the maximum configured size.
     * @return A {@link ResponseEntity} with a 413 Payload Too Large status.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseDTO<Object>> handleMaxUploadSizeExceeded() {
        log.warn("File upload failed: Maximum upload size was exceeded.");
        ErrorDTO error = new ErrorDTO(HttpStatus.PAYLOAD_TOO_LARGE.value(), "File size exceeds the configured maximum limit.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * Handles business logic errors (e.g., negative amounts, missing receiver for transfer).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDTO<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        ErrorDTO error = new ErrorDTO(HttpStatus.BAD_REQUEST.value(), ex.getMessage()); // 400 qaytarish kerak
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.BAD_REQUEST);
    }


    // === CATCH-ALL SAFETY NET HANDLER ===

    /**
     * A catch-all handler for any other unhandled exceptions. This is a critical safety net.
     * It logs the full error stack trace for debugging and returns a generic, safe
     * error message to the client, hiding internal implementation details.
     * @param ex The generic exception.
     * @return A {@link ResponseEntity} with a 500 Internal Server Error status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Object>> handleAllOtherExceptions(Exception ex) {
        log.error("An unexpected internal server error occurred", ex); // Log full stack trace
        ErrorDTO error = new ErrorDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected internal server error occurred. Please contact the administrator.");
        return new ResponseEntity<>(ResponseDTO.error(error), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}