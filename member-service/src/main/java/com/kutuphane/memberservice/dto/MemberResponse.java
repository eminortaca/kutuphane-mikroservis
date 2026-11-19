package com.kutuphane.memberservice.dto;

import com.kutuphane.memberservice.entity.Member;
import com.kutuphane.memberservice.entity.MemberStatus;

import java.time.LocalDate;

// İstemciye dönen üye verisi. Entity doğrudan dışarıya açılmaz.
public record MemberResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate membershipDate,
        MemberStatus status
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getMembershipDate(),
                member.getStatus()
        );
    }
}
