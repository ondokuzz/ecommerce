package com.demirsoft.ecommerce.order_service.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.demirsoft.ecommerce.order_service.event.OrderCreated;

import lombok.extern.log4j.Log4j2;

@EnableKafka
@Configuration
@Log4j2
public class KafkaConsumerConfig {

    @Autowired
    PropertiesConfig propertiesConfig;

    public static final String CONSUMER_GROUP = "order_consumer_group";

    @Bean
    public ConsumerFactory<String, OrderCreated> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                propertiesConfig.getKafkaAddress());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.TYPE_MAPPINGS,
                "com.demirsoft.ecommerce.order_service.event.OrderCreated:com.demirsoft.ecommerce.product_service.event.OrderCreated");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>()));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreated> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreated> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setCommonErrorHandler(commonErrorHandler());
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    private CommonErrorHandler commonErrorHandler() {
        CommonErrorHandler commonErrorHandler = new CommonErrorHandler() {
            @Override
            public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer,
                    MessageListenerContainer container, boolean batchListener) {
                log.error("handleOtherException: kafka listener, error occured. reason: {}",
                        thrownException.getMessage());
            }

            @Override
            public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record,
                    Consumer<?, ?> consumer,
                    MessageListenerContainer container) {
                log.error("handleOne: kafka listener, error occured for topic: {}, reason: {}",
                        record.topic(),
                        thrownException.getMessage());
                return true;
            }

            @Override
            public void handleRemaining(Exception thrownException, List<ConsumerRecord<?, ?>> records,
                    Consumer<?, ?> consumer, MessageListenerContainer container) {
                log.error("handleRemaining: kafka listener, remaining error. reason: {}",
                        thrownException.getMessage());
            }
        };
        return commonErrorHandler;
    }
}
