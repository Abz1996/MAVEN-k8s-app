
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @GetMapping("/")
    public Map home() {
        Map response = new HashMap<>();
        response.put("message", "Welcome to DevOps CI/CD Pipeline with JFrog!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        return response;
    }
    
    @GetMapping("/health")
    public Map health() {
        Map response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "DevOps Demo App");
        return response;
    }
    
    @GetMapping("/info")
    public Map info() {
        Map response = new HashMap<>();
        response.put("app", "DevOps Demo Application");
        response.put("tools", "GitHub, Jenkins, Docker, JFrog, Kubernetes");
        response.put("description", "Complete CI/CD Pipeline Demo");
        return response;
    }
}