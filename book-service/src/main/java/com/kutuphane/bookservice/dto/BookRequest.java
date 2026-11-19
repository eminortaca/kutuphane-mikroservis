package com.kutuphane.bookservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

// Yeni kitap eklerken veya güncellerken istemciden sadece bu alanları kabul ediyoruz.
// Entity'yi (Book) doğrudan kullanmadığımız için dışarıdan "id" gönderilemez ve
// kurallara uymayan veriler (boş başlık, negatif stok gibi) daha içeri girmeden reddedilir.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BookRequest {

    @NotBlank(message = "Kitap adı boş olamaz!")
    private String title;

    @NotBlank(message = "Yazar adı boş olamaz!")
    private String author;

    private String isbn;

    private String category;

    @Min(value = 1000, message = "Yayın yılı geçersiz!")
    @Max(value = 2100, message = "Yayın yılı geçersiz!")
    private Integer publishYear;

    @NotNull(message = "Stok sayısı belirtilmelidir!")
    @Min(value = 0, message = "Stok sayısı 0'dan küçük olamaz!")
    private Integer stock;

    @Min(value = 0, message = "Toplam kopya sayısı 0'dan küçük olamaz!")
    private Integer totalCopies;
}
