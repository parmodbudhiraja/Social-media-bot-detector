package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/health")
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Check DB
        try {
            jdbcTemplate.execute("SELECT 1");
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN: " + e.getMessage());
        }

        // Check RabbitMQ
        try {
            rabbitTemplate.execute(channel -> {
                health.put("rabbitmq", "UP");
                return null;
            });
        } catch (Exception e) {
            health.put("rabbitmq", "DOWN: " + e.getMessage());
        }

        return health;
    }
}
