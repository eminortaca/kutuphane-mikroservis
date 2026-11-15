package com.kutuphane.bookservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

// Yeni kitap eklerken istemciden sadece bu alanları kabul ediyoruz.
// Entity'yi (Book) doğrudan kullanmadığımız için dışarıdan "id" gönderilemez ve
// kurallara uymayan veriler (boş başlık, negatif stok gibi) daha içeri girmeden reddedilir.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BookRequest {

    @NotBlank(message = "Kitap adı boş olamaz!")
    private String title;

    @NotBlank(message = "Yazar adı boş olamaz!")
    private String author;

    @NotNull(message = "Stok sayısı belirtilmelidir!")
    @Min(value = 0, message = "Stok sayısı 0'dan küçük olamaz!")
    private Integer stock;
}
