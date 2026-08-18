package com.azhost.controller;

import com.azhost.service.SystemInfoService;
import com.azhost.security.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InfoController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemInfoService systemInfoService;

    @Test
    void getInfo_ShouldReturnAppInformation() throws Exception {
        given(systemInfoService.getApplicationInfo()).willReturn(
                new com.azhost.dto.InfoResponseDto("AZHost", "0.1.0", "Phase 1", "development")
        );

        mockMvc.perform(get("/api/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("AZHost"))
                .andExpect(jsonPath("$.version").value("0.1.0"))
                .andExpect(jsonPath("$.phase").value("Phase 1"))
                .andExpect(jsonPath("$.status").value("development"));
    }

}
