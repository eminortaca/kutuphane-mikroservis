package com.kutuphane.borrowingservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private Integer stock;
}