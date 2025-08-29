package com.healthrx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    private String name;
    private String regNo;
    private String email;
    private String generateWebhookUrl;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGenerateWebhookUrl() {
        return generateWebhookUrl;
    }

    public void setGenerateWebhookUrl(String generateWebhookUrl) {
        this.generateWebhookUrl = generateWebhookUrl;
    }
}
