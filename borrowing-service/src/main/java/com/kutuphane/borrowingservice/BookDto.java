package com.kutuphane.borrowingservice;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private Integer stock;
}