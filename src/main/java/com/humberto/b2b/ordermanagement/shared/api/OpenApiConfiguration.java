package com.humberto.b2b.ordermanagement.shared.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI orderManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("B2B Order Management API")
                .version("v2")
                .description("B2B orders with partner credit control. Status changes are published to Kafka "
                        + "through a transactional outbox. Docker Compose seeds the demo partners "
                        + "11111111-1111-1111-1111-111111111111 (limit 10000.00) and "
                        + "22222222-2222-2222-2222-222222222222 (limit 500.00)."));
    }
}
