package com.cju.likelion.cardcollection.catalog.service;
import org.springframework.http.HttpStatus;
public class CatalogDomainException extends RuntimeException { private final String code; private final HttpStatus status; public CatalogDomainException(String code, String message, HttpStatus status) { super(message); this.code = code; this.status = status; } public String getCode() { return code; } public HttpStatus getStatus() { return status; } }
