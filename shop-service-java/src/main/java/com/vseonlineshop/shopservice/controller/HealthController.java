package com.vseonlineshop.shopservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "shop-service",
                "status", "healthy"
        );
    }

    @GetMapping("/health/database")
    public ResponseEntity<Map<String, String>> databaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            boolean healthy = connection.isValid(2);
            return ResponseEntity
                    .status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "service", "shop-db",
                            "status", healthy ? "healthy" : "unreachable"
                    ));
        } catch (SQLException exception) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "service", "shop-db",
                            "status", "unreachable"
                    ));
        }
    }
}
