package com.purpleloop.core;

import java.time.Clock;

import com.purpleloop.core.config.DetectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DetectionProperties.class)
public class PurpleLoopApplication {

    @Bean
    public Clock detectionClock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(PurpleLoopApplication.class, args);
    }
}
