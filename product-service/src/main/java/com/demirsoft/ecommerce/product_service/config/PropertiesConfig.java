package com.demirsoft.ecommerce.product_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "ecommerce.config")
@Getter
@Setter
@NoArgsConstructor
public class PropertiesConfig {

    @NotBlank
    private String kafkaAddress;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @NotBlank
    private String mongoDbName;

}