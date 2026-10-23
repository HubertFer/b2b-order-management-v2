package com.humberto.b2b.ordermanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class B2bOrderManagementV2ApplicationTests {

    @Test
    void contextLoads() {
    }

}
