package com.humberto.b2b.ordermanagement.order.api;

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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAndFetchesOrder() throws Exception {
        UUID partnerId = insertPartner("100.00", "0");

        String body = postOrder(orderJson(partnerId, 2, "12.50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partnerId").value(partnerId.toString()))
                .andExpect(jsonPath("$.total").value(25.0))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.5));
    }

    @Test
    void returnsLocationOfCreatedOrder() throws Exception {
        MockHttpServletResponse response = postOrder(orderJson(insertPartner("100.00", "0"), 1, "10.00"))
                .andReturn().getResponse();
        String orderId = JsonPath.read(response.getContentAsString(), "$.id");

        assertTrue(response.getHeader("Location").endsWith("/orders/" + orderId));
    }

    @Test
    void approvesOrderAndDebitsCredit() throws Exception {
        UUID partnerId = insertPartner("100.00", "0");
        String orderId = createOrder(partnerId, 3, "10.00");

        mockMvc.perform(post("/orders/{id}/approve", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));

        assertEquals(new BigDecimal("30.00"), usedCredit(partnerId));
    }

    @Test
    void rejectsSecondApprovalWithConflict() throws Exception {
        String orderId = createOrder(insertPartner("100.00", "0"), 1, "10.00");
        mockMvc.perform(post("/orders/{id}/approve", orderId)).andExpect(status().isOk());

        mockMvc.perform(post("/orders/{id}/approve", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot transition from APROVADO to APROVADO"));
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get("/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/orders/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsCreationAboveAvailableCreditShowingAvailableCredit() throws Exception {
        UUID partnerId = insertPartner("100.00", "80.00");

        postOrder(orderJson(partnerId, 3, "10.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.availableCredit").value(20.0))
                .andExpect(jsonPath("$.requestedAmount").value(30.0));
    }

    @Test
    void rejectsApprovalAboveAvailableCreditShowingAvailableCredit() throws Exception {
        UUID partnerId = insertPartner("100.00", "0");
        String orderId = createOrder(partnerId, 6, "10.00");
        jdbcTemplate.update("UPDATE partner SET used_credit = 50.00 WHERE id = ?", partnerId);

        mockMvc.perform(post("/orders/{id}/approve", orderId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.availableCredit").value(50.0))
                .andExpect(jsonPath("$.requestedAmount").value(60.0));
    }

    @Test
    void rejectsUnknownPartnerWithUnprocessableContent() throws Exception {
        postOrder(orderJson(UUID.randomUUID(), 1, "10.00"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void rejectsInvalidRequestListingFieldErrors() throws Exception {
        postOrder("""
                {"partnerId": "%s", "items": [{"productId": "%s", "quantity": 0, "unitPrice": "1.001"}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'items[0].quantity')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'items[0].unitPrice')]").exists());
    }

    private ResultActions postOrder(String json) throws Exception {
        return mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createOrder(UUID partnerId, int quantity, String unitPrice) throws Exception {
        String body = postOrder(orderJson(partnerId, quantity, unitPrice))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String orderJson(UUID partnerId, int quantity, String unitPrice) {
        return """
                {"partnerId": "%s", "items": [{"productId": "%s", "quantity": %d, "unitPrice": %s}]}
                """.formatted(partnerId, UUID.randomUUID(), quantity, unitPrice);
    }

    private UUID insertPartner(String creditLimit, String usedCredit) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, ?, ?)",
                id, new BigDecimal(creditLimit), new BigDecimal(usedCredit));
        return id;
    }

    private BigDecimal usedCredit(UUID partnerId) {
        return jdbcTemplate.queryForObject("SELECT used_credit FROM partner WHERE id = ?", BigDecimal.class, partnerId);
    }
}
