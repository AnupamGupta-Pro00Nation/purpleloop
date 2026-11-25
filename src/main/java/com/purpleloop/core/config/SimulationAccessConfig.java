package com.purpleloop.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SimulationAccessConfig implements WebMvcConfigurer {

    private final SimulationAccessInterceptor simulationAccessInterceptor;

    public SimulationAccessConfig(SimulationAccessInterceptor simulationAccessInterceptor) {
        this.simulationAccessInterceptor = simulationAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(simulationAccessInterceptor)
                .addPathPatterns("/api/simulate/**");
    }
}
