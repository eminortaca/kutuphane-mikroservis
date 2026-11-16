package com.kutuphane.borrowingservice.exception;

import java.time.LocalDateTime;

// Hata olduğunda istemciye dönen standart JSON gövdesi (Book-Service'teki ile aynı formatta)
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
}
