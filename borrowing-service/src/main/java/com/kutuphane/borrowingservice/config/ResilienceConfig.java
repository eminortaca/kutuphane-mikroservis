package com.kutuphane.borrowingservice.config;

import com.kutuphane.borrowingservice.exception.BookNotFoundException;
import com.kutuphane.borrowingservice.exception.MemberNotFoundException;
import com.kutuphane.borrowingservice.exception.OutOfStockException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// DEVRE KESİCİ (Circuit Breaker) AYARLARI - Resilience4j
// Evdeki sigorta gibi düşün: Book-Service sürekli hata veriyorsa "sigorta atar" ve bir süre
// hiç istek göndermeyiz. Böylece hem Book-Service toparlanmaya vakit bulur hem de bizim
// thread'lerimiz boşuna bekleyip kilitlenmez.
//
// Durumlar:  KAPALI (normal) --(hata oranı yüksek)--> AÇIK (istek yok) --(10 sn sonra)--> YARI AÇIK (3 deneme)
//            YARI AÇIK'ta denemeler başarılıysa tekrar KAPALI, değilse tekrar AÇIK.
@Configuration
public class ResilienceConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)                         // Son 10 çağrıya bak
                        .minimumNumberOfCalls(5)                       // En az 5 çağrı olmadan karar verme
                        .failureRateThreshold(50)                      // %50 ve üstü hata varsa sigorta atsın
                        .waitDurationInOpenState(Duration.ofSeconds(10)) // AÇIK durumda 10 sn bekle
                        .permittedNumberOfCallsInHalfOpenState(3)      // YARI AÇIK'ta 3 deneme isteğine izin ver
                        // "Kitap yok", "stok yok" ve "üye yok" arıza değil, normal iş kuralı.
                        // Bunlar sigortayı attırmasın:
                        .ignoreExceptions(BookNotFoundException.class, OutOfStockException.class, MemberNotFoundException.class)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(4))        // 4 sn'den uzun süren çağrı başarısız sayılır
                        .build())
                .build());
    }
}
