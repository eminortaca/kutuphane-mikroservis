package com.kutuphane.memberservice.controller;

import com.kutuphane.memberservice.dto.MemberRequest;
import com.kutuphane.memberservice.dto.MemberResponse;
import com.kutuphane.memberservice.entity.MemberStatus;
import com.kutuphane.memberservice.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 1. Yeni üye ekle -> 201 Created + Location: /api/members/{id}
    @PostMapping
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberRequest request) {
        MemberResponse created = MemberResponse.from(memberService.createMember(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // 2. Tüm üyeleri listele
    @GetMapping
    public List<MemberResponse> getAllMembers() {
        return memberService.getAllMembers().stream()
                .map(MemberResponse::from)
                .toList();
    }

    // 3. ID'ye göre üye getir
    @GetMapping("/{id}")
    public MemberResponse getMemberById(@PathVariable Long id) {
        return MemberResponse.from(memberService.getMemberById(id));
    }

    // 4. Üye bilgilerini güncelle
    @PutMapping("/{id}")
    public MemberResponse updateMember(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(memberService.updateMember(id, request));
    }

    // 5. Üye durumunu güncelle (ACTIVE / SUSPENDED)
    @PutMapping("/{id}/status")
    public MemberResponse updateMemberStatus(@PathVariable Long id, @RequestParam MemberStatus status) {
        return MemberResponse.from(memberService.updateMemberStatus(id, status));
    }

    // 6. Üye sil -> 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}
