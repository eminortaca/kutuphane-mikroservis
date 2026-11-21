package com.kutuphane.bookservice.controller;

import com.kutuphane.bookservice.entity.Book;
import com.kutuphane.bookservice.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalBookController.class)
class InternalBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void decreaseStock_withoutInternalSecret_shouldReturn403() throws Exception {
        mockMvc.perform(put("/internal/books/1/decrease-stock"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void decreaseStock_withValidInternalSecret_shouldReturn200() throws Exception {
        Book updated = new Book(1L, "Suç ve Ceza", "Dostoyevski", "978-01", "Roman", 1866, 2, 3);
        when(bookService.decreaseStock(1L)).thenReturn(updated);

        mockMvc.perform(put("/internal/books/1/decrease-stock")
                        .header("X-Internal-Secret", "kutuphane-internal-secret-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stock").value(2));
    }
}
