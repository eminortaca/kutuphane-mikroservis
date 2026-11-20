package com.kutuphane.memberservice.service;

import com.kutuphane.memberservice.dto.MemberRequest;
import com.kutuphane.memberservice.entity.Member;
import com.kutuphane.memberservice.entity.MemberStatus;
import com.kutuphane.memberservice.exception.DuplicateEmailException;
import com.kutuphane.memberservice.exception.MemberNotFoundException;
import com.kutuphane.memberservice.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void createMember_shouldDefaultStatusToActiveAndSetMembershipDate() {
        MemberRequest request = new MemberRequest("Ahmet Yılmaz", "ahmet@example.com", "05551112233", null);
        when(memberRepository.existsByEmailIgnoreCase("ahmet@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        Member created = memberService.createMember(request);

        assertEquals(1L, created.getId());
        assertEquals(MemberStatus.ACTIVE, created.getStatus());
        assertEquals(LocalDate.now(), created.getMembershipDate());
    }

    @Test
    void createMember_whenDuplicateEmail_shouldThrowDuplicateEmailException() {
        MemberRequest request = new MemberRequest("Ahmet Yılmaz", "ahmet@example.com", "05551112233", null);
        when(memberRepository.existsByEmailIgnoreCase("ahmet@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> memberService.createMember(request));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void updateMemberStatus_shouldChangeStatus() {
        Member existing = new Member(1L, "Ahmet Yılmaz", "ahmet@example.com", "05551112233", LocalDate.now(), MemberStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Member updated = memberService.updateMemberStatus(1L, MemberStatus.SUSPENDED);

        assertEquals(MemberStatus.SUSPENDED, updated.getStatus());
    }

    @Test
    void getMemberById_whenNotFound_shouldThrowMemberNotFoundException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class, () -> memberService.getMemberById(99L));
    }
}
