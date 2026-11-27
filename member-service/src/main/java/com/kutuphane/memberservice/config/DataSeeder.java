package com.kutuphane.memberservice.config;

import com.kutuphane.memberservice.entity.Member;
import com.kutuphane.memberservice.entity.MemberStatus;
import com.kutuphane.memberservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// Uygulama ilk kez boş veritabanıyla açıldığında demo hesaplarla (member1 -> ID 1, member2 -> ID 2)
// eşleşen başlangıç üyelerini otomatik yükler.
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<Member> sampleMembers = List.of(
                new Member(null, "Ahmet Yılmaz", "ahmet@kutuphane.com", "0555 111 2233", today.minusMonths(4), MemberStatus.ACTIVE),
                new Member(null, "Ayşe Demir", "ayse@kutuphane.com", "0555 444 5566", today.minusMonths(2), MemberStatus.ACTIVE),
                new Member(null, "Mehmet Kaya", "mehmet@kutuphane.com", "0555 777 8899", today.minusDays(15), MemberStatus.SUSPENDED)
        );

        memberRepository.saveAll(sampleMembers);
        log.info("Başlangıç verisi yüklendi: {} örnek üye eklendi.", sampleMembers.size());
    }
}
