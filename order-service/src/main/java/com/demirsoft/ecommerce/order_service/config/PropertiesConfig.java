package com.demirsoft.ecommerce.order_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "com.demirsoft.ecommerce.config")
@Getter
@Setter
@NoArgsConstructor
public class PropertiesConfig {
    @NotBlank
    private String kafkaAddress;

}