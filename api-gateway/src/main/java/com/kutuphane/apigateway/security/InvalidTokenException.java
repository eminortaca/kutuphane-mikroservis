package com.kutuphane.apigateway.security;

// Token eksik, imzası bozuk veya süresi dolmuşsa fırlatılır -> HTTP 401 (Unauthorized)
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
