package com.utkarsh.ordermgmt.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for clients. Routes are declared in application.yml and
 * resolved through Eureka (lb://service-name), so clients only ever call port 8080.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
