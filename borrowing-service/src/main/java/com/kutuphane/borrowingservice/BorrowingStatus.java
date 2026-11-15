package com.kutuphane.borrowingservice;

// Ödünç alma işleminin durumu
public enum BorrowingStatus {
    BORROWED, // Ödünç alındı (henüz iade edilmedi)
    RETURNED  // İade edildi
}
