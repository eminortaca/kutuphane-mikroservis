package com.kutuphane.memberservice.exception;

import java.time.LocalDateTime;

// Hata olduğunda istemciye dönen standart JSON gövdesi.
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
}
