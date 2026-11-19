package com.kutuphane.memberservice.entity;

// Kütüphane üyesinin hesap durumu
public enum MemberStatus {
    ACTIVE,    // Aktif üye (kitap ödünç alabilir)
    SUSPENDED  // Askıya alınmış üye (kitap ödünç alamaz)
}
