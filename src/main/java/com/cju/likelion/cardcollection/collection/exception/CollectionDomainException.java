package com.cju.likelion.cardcollection.collection.exception;
import lombok.Getter; import org.springframework.http.HttpStatus;
@Getter public class CollectionDomainException extends RuntimeException { private final String code; private final HttpStatus status; public CollectionDomainException(String code, String message, HttpStatus status) { super(message); this.code = code; this.status = status; } }
