package com.kutuphane.memberservice.controller;

import com.kutuphane.memberservice.entity.Member;
import com.kutuphane.memberservice.entity.MemberStatus;
import com.kutuphane.memberservice.exception.DuplicateEmailException;
import com.kutuphane.memberservice.exception.MemberNotFoundException;
import com.kutuphane.memberservice.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    void createMember_whenValid_shouldReturn201() throws Exception {
        Member saved = new Member(1L, "Ahmet Yılmaz", "ahmet@example.com", "05551112233", LocalDate.now(), MemberStatus.ACTIVE);
        when(memberService.createMember(any())).thenReturn(saved);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ahmet Yılmaz",
                                  "email": "ahmet@example.com",
                                  "phone": "05551112233"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/members/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createMember_whenInvalidEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ahmet Yılmaz",
                                  "email": "gecersiz-eposta",
                                  "phone": "05551112233"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void createMember_whenDuplicateEmail_shouldReturn409() throws Exception {
        when(memberService.createMember(any())).thenThrow(new DuplicateEmailException("ahmet@example.com"));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ahmet Yılmaz",
                                  "email": "ahmet@example.com",
                                  "phone": "05551112233"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void getMemberById_whenNotFound_shouldReturn404() throws Exception {
        when(memberService.getMemberById(99L)).thenThrow(new MemberNotFoundException(99L));

        mockMvc.perform(get("/api/members/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Üye bulunamadı! ID: 99"));
    }
}
