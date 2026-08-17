package com.cju.likelion.cardcollection.common.exception;

import com.cju.likelion.cardcollection.common.api.ApiErrorResponse;
import com.cju.likelion.cardcollection.auth.exception.DuplicateEmailException;
import com.cju.likelion.cardcollection.auth.exception.InvalidCredentialsException;
import com.cju.likelion.cardcollection.auth.exception.OAuthLoginException;
import com.cju.likelion.cardcollection.card.exception.CardDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("EMAIL_ALREADY_EXISTS", exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("INVALID_CREDENTIALS", exception.getMessage()));
    }

    @ExceptionHandler(OAuthLoginException.class)
    public ResponseEntity<ApiErrorResponse> handleOAuthLogin(OAuthLoginException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("OAUTH_LOGIN_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(CardDomainException.class)
    public ResponseEntity<ApiErrorResponse> handleCardDomain(CardDomainException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("INVALID_REQUEST", message));
    }
}
