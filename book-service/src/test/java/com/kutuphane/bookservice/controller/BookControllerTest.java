package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.exception.BookInUseException;
import com.kutuphane.bookservice.exception.BookNotFoundException;
import com.kutuphane.bookservice.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void createBook_whenValid_shouldReturn201AndLocationHeader() throws Exception {
        Book saved = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 3, 3);
        when(bookService.createBook(any())).thenReturn(saved);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Suç ve Ceza",
                                  "author": "Dostoyevski",
                                  "isbn": "978-01",
                                  "category": "Roman",
                                  "publishYear": 1866,
                                  "stock": 3,
                                  "totalCopies": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/books/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Suç ve Ceza"))
                .andExpect(jsonPath("$.availableCopies").value(3));
    }

    @Test
    void createBook_whenInvalid_shouldReturn400WithErrorResponse() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "author": "Dostoyevski",
                                  "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getAllBooks_shouldReturn200AndPaginationHeaders() throws Exception {
        Book book = new Book(1L, "Nutuk", "Atatürk", "978-02", "Tarih", 1927, 2, 2);
        when(bookService.searchBooks(null, null, null, 0, 20))
                .thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(header().string("X-Total-Pages", "1"))
                .andExpect(jsonPath("$[0].title").value("Nutuk"));
    }

    @Test
    void getBookById_whenNotFound_shouldReturn404() throws Exception {
        when(bookService.getBookById(99L)).thenThrow(new BookNotFoundException(99L));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Kitap bulunamadı! ID: 99"));
    }

    @Test
    void deleteBook_whenBookInUse_shouldReturn409() throws Exception {
        doThrow(new BookInUseException("Bu kitabın şu anda ödünçte olan 1 kopyası bulunduğu için silinemez!"))
                .when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
