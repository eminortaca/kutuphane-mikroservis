package com.kutuphane.memberservice.dto;

import com.kutuphane.memberservice.entity.MemberStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

// Yeni üye eklerken veya güncellerken istemciden kabul edilen alanlar
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MemberRequest {

    @NotBlank(message = "Üye adı boş olamaz!")
    private String name;

    @NotBlank(message = "E-posta adresi boş olamaz!")
    @Email(message = "Geçerli bir e-posta adresi giriniz!")
    private String email;

    @NotBlank(message = "Telefon numarası boş olamaz!")
    private String phone;

    private MemberStatus status;
}
