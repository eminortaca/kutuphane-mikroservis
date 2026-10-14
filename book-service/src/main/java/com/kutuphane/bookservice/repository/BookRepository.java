package com.kutuphane.bookservice.repository;

import com.kutuphane.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}