package com.healthrx.service;

import com.healthrx.config.AppConfig;
import com.healthrx.config.HttpConfig;
import com.healthrx.dto.WebhookRequest;
import com.healthrx.dto.WebhookResponse;
import com.healthrx.dto.FinalQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HiringService {
    
    private static final Logger logger = LoggerFactory.getLogger(HiringService.class);
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private HttpConfig httpConfig;
    
    @Autowired
    private RestTemplate restTemplate;

    public void executeStartupFlow() {
        logger.info("Starting Java Hiring Challenge application...");
        
        try {
            // Step 1: Read runtime config (already loaded via @ConfigurationProperties)
            logger.info("Configuration loaded - Name: {}, RegNo: {}, Email: {}", 
                appConfig.getName(), appConfig.getRegNo(), appConfig.getEmail());
            
            // Step 2: Send generateWebhook request
            WebhookResponse webhookResponse = generateWebhook();
            
            // Step 3: Validate response
            if (webhookResponse == null || webhookResponse.getWebhook() == null || 
                webhookResponse.getAccessToken() == null) {
                logger.error("Invalid webhook response received");
                System.exit(1);
            }
            
            logger.info("Webhook generated successfully. Webhook URL: {}", webhookResponse.getWebhook());
            logger.info("Access token received: [REDACTED]");
            
            // Step 4: Decide which SQL problem applies
            int questionNumber = determineQuestionNumber(appConfig.getRegNo());
            logger.info("Registration number {} maps to Question {}", appConfig.getRegNo(), questionNumber);
            
            // Step 5: Load final SQL query text
            String sqlQuery = loadSqlQuery(questionNumber);
            if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
                logger.error("Failed to load SQL query for Question {}", questionNumber);
                System.exit(1);
            }
            
            logger.info("Loaded SQL query for Question {}: {}", questionNumber, sqlQuery.substring(0, Math.min(100, sqlQuery.length())) + "...");
            
            // Step 6 & 7: Submit final SQL
            submitFinalQuery(webhookResponse.getWebhook(), webhookResponse.getAccessToken(), sqlQuery);
            
            // Step 8: Verify submission
            logger.info("Java Hiring Challenge completed successfully!");
            
        } catch (Exception e) {
            logger.error("Fatal error during startup flow", e);
            System.exit(1);
        }
    }

    private WebhookResponse generateWebhook() {
        WebhookRequest request = new WebhookRequest(
            appConfig.getName(), 
            appConfig.getRegNo(), 
            appConfig.getEmail()
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);
        
        for (int attempt = 1; attempt <= httpConfig.getRetry().getMaxAttempts(); attempt++) {
            try {
                logger.info("Attempting to generate webhook (attempt {}/{})", attempt, httpConfig.getRetry().getMaxAttempts());
                
                ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                    appConfig.getGenerateWebhookUrl(), 
                    entity, 
                    WebhookResponse.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("Webhook generation successful on attempt {}", attempt);
                    return response.getBody();
                } else {
                    logger.warn("Webhook generation returned non-2xx status: {}", response.getStatusCode());
                }
                
            } catch (HttpClientErrorException e) {
                logger.error("Client error during webhook generation (attempt {}): {} - {}", 
                    attempt, e.getStatusCode(), e.getResponseBodyAsString());
                if (attempt == 1) {
                    // Don't retry on client errors
                    break;
                }
            } catch (HttpServerErrorException e) {
                logger.warn("Server error during webhook generation (attempt {}): {} - {}", 
                    attempt, e.getStatusCode(), e.getResponseBodyAsString());
            } catch (ResourceAccessException e) {
                logger.warn("Network error during webhook generation (attempt {}): {}", attempt, e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error during webhook generation (attempt {}): {}", attempt, e.getMessage());
            }
            
            if (attempt < httpConfig.getRetry().getMaxAttempts()) {
                long backoffDelay = httpConfig.getRetry().getBackoffBase() * (long) Math.pow(2, attempt - 1);
                logger.info("Waiting {} ms before retry...", backoffDelay);
                try {
                    Thread.sleep(backoffDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.error("Failed to generate webhook after {} attempts", httpConfig.getRetry().getMaxAttempts());
        return null;
    }

    private int determineQuestionNumber(String regNo) {
        // Extract all digit characters from regNo
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(regNo);
        
        StringBuilder allDigits = new StringBuilder();
        while (matcher.find()) {
            allDigits.append(matcher.group());
        }
        
        String digits = allDigits.toString();
        logger.info("Extracted digits from registration number: {}", digits);
        
        if (digits.length() == 0) {
            logger.warn("No digits found in registration number, defaulting to Question 1");
            return 1;
        }
        
        // Take last two digits if available, otherwise take whatever digits exist
        String lastDigits = digits.length() >= 2 ? digits.substring(digits.length() - 2) : digits;
        int number = Integer.parseInt(lastDigits);
        
        // Determine question based on parity
        int questionNumber = (number % 2 == 1) ? 1 : 2;
        logger.info("Last digits: {}, Number: {}, Parity: {}, Question: {}", 
            lastDigits, number, (number % 2 == 1 ? "odd" : "even"), questionNumber);
        
        return questionNumber;
    }

    private String loadSqlQuery(int questionNumber) {
        String resourcePath = String.format("queries/Q%d.sql", questionNumber);
        logger.info("Loading SQL query from resource: {}", resourcePath);
        
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                logger.error("Resource file does not exist: {}", resourcePath);
                return null;
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String trimmedContent = content.trim();
            
            if (trimmedContent.isEmpty()) {
                logger.error("SQL query file is empty: {}", resourcePath);
                return null;
            }
            
            logger.info("Successfully loaded SQL query from {}", resourcePath);
            return trimmedContent;
            
        } catch (IOException e) {
            logger.error("Failed to read SQL query file: {}", resourcePath, e);
            return null;
        }
    }

    private void submitFinalQuery(String webhookUrl, String accessToken, String sqlQuery) {
        FinalQueryRequest request = new FinalQueryRequest(sqlQuery);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Try Bearer token first, then fallback to direct token
        String[] authFormats = {
            "Bearer " + accessToken,
            accessToken
        };
        
        for (int authAttempt = 0; authAttempt < authFormats.length; authAttempt++) {
            String authHeader = authFormats[authAttempt];
            headers.set("Authorization", authHeader);
            
            logger.info("Attempting to submit final query with authorization format: {}", 
                authAttempt == 0 ? "Bearer" : "Direct");
            
            for (int attempt = 1; attempt <= httpConfig.getRetry().getMaxAttempts(); attempt++) {
                try {
                    logger.info("Submitting final query (attempt {}/{})", attempt, httpConfig.getRetry().getMaxAttempts());
                    
                    HttpEntity<FinalQueryRequest> entity = new HttpEntity<>(request, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(
                        webhookUrl, 
                        entity, 
                        String.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("Final query submitted successfully! Response: {}", 
                            response.getBody() != null ? response.getBody().replaceAll(accessToken, "[REDACTED]") : "No body");
                        return;
                    } else {
                        logger.warn("Final query submission returned non-2xx status: {}", response.getStatusCode());
                    }
                    
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                        logger.warn("Authorization failed with format: {}", authAttempt == 0 ? "Bearer" : "Direct");
                        if (authAttempt == 0) {
                            // Try next auth format
                            break;
                        } else {
                            // Both auth formats failed
                            logger.error("All authorization formats failed");
                            throw e;
                        }
                    } else {
                        logger.error("Client error during final query submission (attempt {}): {} - {}", 
                            attempt, e.getStatusCode(), e.getResponseBodyAsString());
                        if (attempt == 1) {
                            // Don't retry on client errors
                            break;
                        }
                    }
                } catch (HttpServerErrorException e) {
                    logger.warn("Server error during final query submission (attempt {}): {} - {}", 
                        attempt, e.getStatusCode(), e.getResponseBodyAsString());
                } catch (ResourceAccessException e) {
                    logger.warn("Network error during final query submission (attempt {}): {}", attempt, e.getMessage());
                } catch (Exception e) {
                    logger.error("Unexpected error during final query submission (attempt {}): {}", attempt, e.getMessage());
                }
                
                if (attempt < httpConfig.getRetry().getMaxAttempts()) {
                    long backoffDelay = httpConfig.getRetry().getBackoffBase() * (long) Math.pow(2, attempt - 1);
                    logger.info("Waiting {} ms before retry...", backoffDelay);
                    try {
                        Thread.sleep(backoffDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("Failed to submit final query after all attempts and authorization formats");
        throw new RuntimeException("Final query submission failed");
    }
}
