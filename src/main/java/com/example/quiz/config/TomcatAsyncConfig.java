package com.example.quiz.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatAsyncConfig {

    @Bean
    public TomcatConnectorCustomizer asyncTimeoutCustomize() {
        return new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                connector.setAsyncTimeout(0); // Unlimited timeout
            }
        };
    }
}
