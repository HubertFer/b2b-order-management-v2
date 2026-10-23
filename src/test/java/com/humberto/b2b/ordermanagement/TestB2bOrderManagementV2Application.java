package com.humberto.b2b.ordermanagement;

import org.springframework.boot.SpringApplication;

public class TestB2bOrderManagementV2Application {

    public static void main(String[] args) {
        SpringApplication.from(B2bOrderManagementV2Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
