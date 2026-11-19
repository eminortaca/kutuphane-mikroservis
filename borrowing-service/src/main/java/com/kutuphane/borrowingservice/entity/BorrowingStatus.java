package com.kutuphane.borrowingservice.entity;

// Ödünç alma işleminin durumu
public enum BorrowingStatus {
    BORROWED, // Ödünç alındı (süresi devam ediyor)
    OVERDUE,  // Teslim tarihi geçti ve henüz iade edilmedi
    RETURNED  // İade edildi
}
