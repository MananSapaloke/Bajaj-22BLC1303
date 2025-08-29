package com.healthrx.runner;

import com.healthrx.service.HiringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class);
    
    @Autowired
    private HiringService hiringService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Application startup detected. Executing Java Hiring Challenge flow...");
        
        try {
            hiringService.executeStartupFlow();
            logger.info("Startup flow completed successfully. Application will continue running.");
        } catch (Exception e) {
            logger.error("Startup flow failed with fatal error", e);
            // Exit with non-zero status code as specified in requirements
            System.exit(1);
        }
    }
}
