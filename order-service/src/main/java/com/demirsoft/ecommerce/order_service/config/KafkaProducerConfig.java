package com.demirsoft.ecommerce.order_service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.event.OrderCreated;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Autowired
    PropertiesConfig propertiesConfig;

    @Bean
    public ProducerFactory<String, OrderCreated> producerFactoryForOrderCreated() {
        return this.<OrderCreated>producerFactory();

    }

    @Bean
    public KafkaTemplate<String, OrderCreated> kafkaTemplateForOrderCreated() {
        return this.<OrderCreated>kafkaTemplate();
    }

    @Bean
    public <E> ProducerFactory<String, E> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getKafkaAddress());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public <E> KafkaTemplate<String, E> kafkaTemplate() {
        return new KafkaTemplate<String, E>(this.<E>producerFactory());
    }
}
