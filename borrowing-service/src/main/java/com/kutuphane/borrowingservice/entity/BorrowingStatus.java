package com.kutuphane.borrowingservice.entity;

// Ödünç alma işleminin durumu
public enum BorrowingStatus {
    BORROWED, // Ödünç alındı (henüz iade edilmedi)
    RETURNED  // İade edildi
}
