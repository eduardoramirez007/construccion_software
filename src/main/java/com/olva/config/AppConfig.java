package com.olva.config;

import com.olva.notification.config.NotificationProperties;
import com.olva.notification.config.RipleyNotificationProperties;
import com.olva.notification.config.SerhafenNotificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({
        RipleyAuthProperties.class,
        NotificationProperties.class,
        RipleyNotificationProperties.class,
        SerhafenNotificationProperties.class
})
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
