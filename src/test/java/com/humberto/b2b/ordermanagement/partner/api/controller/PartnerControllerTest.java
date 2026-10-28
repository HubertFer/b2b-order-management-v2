package com.humberto.b2b.ordermanagement.partner.api.controller;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsPartnerWithLocationAndFullAvailableCredit() throws Exception {
        MockHttpServletResponse response = postPartner("{\"creditLimit\": 500.00}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditLimit").value(500.0))
                .andExpect(jsonPath("$.usedCredit").value(0.0))
                .andExpect(jsonPath("$.availableCredit").value(500.0))
                .andReturn().getResponse();
        String partnerId = JsonPath.read(response.getContentAsString(), "$.id");

        assertTrue(response.getHeader("Location").endsWith("/partners/" + partnerId));
    }

    @Test
    void showsAvailableCreditAfterApproval() throws Exception {
        String partnerId = JsonPath.read(postPartner("{\"creditLimit\": 100.00}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        String orderBody = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"partnerId": "%s", "items": [{"productId": "%s", "quantity": 3, "unitPrice": 10.00}]}
                        """.formatted(partnerId, UUID.randomUUID())))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/orders/{id}/approve", JsonPath.<String>read(orderBody, "$.id")));

        mockMvc.perform(get("/partners/{id}", partnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(100.0))
                .andExpect(jsonPath("$.usedCredit").value(30.0))
                .andExpect(jsonPath("$.availableCredit").value(70.0));
    }

    @Test
    void returnsNotFoundForUnknownPartner() throws Exception {
        mockMvc.perform(get("/partners/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidCreditLimit() throws Exception {
        postPartner("{\"creditLimit\": -1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("creditLimit"));
        postPartner("{}")
                .andExpect(status().isBadRequest());
    }

    private ResultActions postPartner(String json) throws Exception {
        return mockMvc.perform(post("/partners").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
