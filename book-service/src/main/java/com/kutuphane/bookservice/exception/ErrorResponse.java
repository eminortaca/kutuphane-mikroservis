package com.kutuphane.bookservice.exception;

import java.time.LocalDateTime;

// Hata olduğunda istemciye dönen standart JSON gövdesi. Örnek:
// { "status": 404, "error": "Not Found", "message": "Kitap bulunamadı! ID: 5", "timestamp": "..." }
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
}
