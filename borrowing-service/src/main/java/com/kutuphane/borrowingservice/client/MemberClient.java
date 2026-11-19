package com.kutuphane.borrowingservice.client;

import com.kutuphane.borrowingservice.dto.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Eureka üzerinden member-service ile haberleşir.
// fallbackFactory: Member-Service çökerse / yavaşlarsa / devre kesici açılırsa ne yapılacağını söyler.
@FeignClient(name = "member-service", path = "/api/members", fallbackFactory = MemberClientFallbackFactory.class)
public interface MemberClient {

    @GetMapping("/{id}")
    MemberDto getMemberById(@PathVariable("id") Long id);
}
