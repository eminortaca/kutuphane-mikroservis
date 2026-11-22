package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.dto.MemberDto;
import com.kutuphane.borrowingservice.exception.MemberNotFoundException;
import com.kutuphane.borrowingservice.exception.MemberServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

// Member-Service çağrısı başarısız olunca Feign buraya düşer.
@Slf4j
@Component
public class MemberClientFallbackFactory implements FallbackFactory<MemberClient> {

    @Override
    public MemberClient create(Throwable cause) {
        return id -> {
            throw toException(cause, id);
        };
    }

    private RuntimeException toException(Throwable cause, Long memberId) {
        // İş kuralı hatası (üye yok) Member-Service'in arızası değildir; aynen iletiyoruz.
        if (cause instanceof MemberNotFoundException ex) {
            return ex;
        }

        if (cause instanceof CallNotPermittedException) {
            log.warn("Devre kesici açık, Member-Service çağrılmadı. memberId={}", memberId);
            return new MemberServiceUnavailableException(
                    "Üye servisi şu an yanıt vermiyor, lütfen biraz sonra tekrar deneyin.");
        }

        log.error("Member-Service çağrısı başarısız. memberId={}, sebep={}", memberId, cause.toString());
        return new MemberServiceUnavailableException(
                "Üye servisine şu an ulaşılamıyor, lütfen biraz sonra tekrar deneyin.");
    }
}
