package com.demirsoft.ecommerce.product_service.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaProducerConfig {

    @Autowired
    PropertiesConfig propertiesConfig;

    @Bean
    public ProducerFactory<Object, Object> producerFactory() {
        Map<String, Object> config = getKafkaProducerConfig();

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ReactiveKafkaProducerTemplate<String, Object> getReactiveKafkaTemplate() {
        var config = getKafkaProducerConfig();
        SenderOptions<String, Object> senderOptions = SenderOptions.create(config);
        return new ReactiveKafkaProducerTemplate<String, Object>(senderOptions);
    }

    private Map<String, Object> getKafkaProducerConfig() {
        var config = new HashMap<String, Object>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getKafkaAddress());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return config;
    }
}
