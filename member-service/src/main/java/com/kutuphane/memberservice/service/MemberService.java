package com.kutuphane.memberservice.service;

import com.kutuphane.memberservice.dto.MemberRequest;
import com.kutuphane.memberservice.entity.Member;
import com.kutuphane.memberservice.entity.MemberStatus;
import com.kutuphane.memberservice.exception.DuplicateEmailException;
import com.kutuphane.memberservice.exception.MemberNotFoundException;
import com.kutuphane.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// Üye yönetimi iş mantığı burada toplanır; Controller sadece HTTP isteklerini karşılar.
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 1. Yeni üye oluştur
    public Member createMember(MemberRequest request) {
        String email = request.getEmail().trim();
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        Member member = new Member();
        member.setName(request.getName().trim());
        member.setEmail(email);
        member.setPhone(request.getPhone().trim());
        member.setMembershipDate(LocalDate.now());
        member.setStatus(request.getStatus() != null ? request.getStatus() : MemberStatus.ACTIVE);
        return memberRepository.save(member);
    }

    // 2. Tüm üyeleri listele
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    // 3. ID'ye göre üye getir (yoksa 404)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }

    // 4. Üye bilgilerini güncelle
    public Member updateMember(Long id, MemberRequest request) {
        Member member = getMemberById(id);

        String email = request.getEmail().trim();
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }

        member.setName(request.getName().trim());
        member.setEmail(email);
        member.setPhone(request.getPhone().trim());
        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
        }
        return memberRepository.save(member);
    }

    // 5. Üye durumunu güncelle (ACTIVE / SUSPENDED)
    public Member updateMemberStatus(Long id, MemberStatus status) {
        Member member = getMemberById(id);
        member.setStatus(status);
        return memberRepository.save(member);
    }

    // 6. Üye sil
    public void deleteMember(Long id) {
        Member member = getMemberById(id);
        memberRepository.delete(member);
    }
}
