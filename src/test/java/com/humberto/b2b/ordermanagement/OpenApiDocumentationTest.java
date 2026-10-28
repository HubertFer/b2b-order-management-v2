package com.humberto.b2b.ordermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("B2B Order Management API"))
                .andExpect(jsonPath("$.paths['/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/orders/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/orders/{id}/approve'].post").exists())
                .andExpect(jsonPath("$.paths['/orders/{id}/cancel'].post").exists())
                .andExpect(jsonPath("$.paths['/orders/{id}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/partners'].post").exists())
                .andExpect(jsonPath("$.paths['/partners/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/orders'].post.parameters[?(@.name == 'Idempotency-Key')]").exists());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
