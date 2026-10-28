package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchOrdersServiceTest {

    private final SearchOrdersService service =
            new SearchOrdersService((criteria, page, size) -> new OrderPage(List.of(), page, size, 0, 0));

    @Test
    void delegatesValidSearch() {
        OrderPage result = service.search(new OrderSearchCriteria(OrderStatus.PENDENTE, null, null), 2, 100);

        assertEquals(2, result.page());
        assertEquals(100, result.size());
    }

    @Test
    void rejectsInvalidPagination() {
        OrderSearchCriteria criteria = new OrderSearchCriteria(null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.search(criteria, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.search(criteria, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.search(criteria, 0, 101));
    }

    @Test
    void rejectsPeriodThatDoesNotMoveForward() {
        Instant instant = Instant.parse("2026-10-15T12:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> new OrderSearchCriteria(null, instant, instant));
        assertThrows(IllegalArgumentException.class, () -> new OrderSearchCriteria(null, instant, instant.minusSeconds(1)));
        assertDoesNotThrow(() -> new OrderSearchCriteria(null, instant, null));
    }
}
