package com.demirsoft.ecommerce.product_service.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.demirsoft.ecommerce.product_service.event.OrderCreated;

import lombok.extern.log4j.Log4j2;

@EnableKafka
@Configuration
@Log4j2
public class KafkaConsumerConfig {

        public class TypeMapping<E> {
                private final String value;

                public TypeMapping(String value) {
                        this.value = value;
                }

                public String getValue() {
                        return value;
                }
        }

        @Autowired
        PropertiesConfig propertiesConfig;

        public static final String CONSUMER_GROUP = "product_consumer_group";

        @Bean
        public TypeMapping<OrderCreated> orderCreatedTypeMappings() {
                String mappings[] = {
                                "com.demirsoft.ecommerce.order_service.event.OrderCreated:com.demirsoft.ecommerce.product_service.event.OrderCreated"
                };

                String joinedMappings = Arrays.asList(mappings).stream().collect(Collectors.joining(","));

                return new TypeMapping<>(joinedMappings);
        }

        @Bean
        public ConsumerFactory<String, OrderCreated> consumerFactoryForOrderCreated(
                        TypeMapping<OrderCreated> typeMapping) {
                return this.<OrderCreated>consumerFactory(typeMapping);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, OrderCreated> kafkaListenerContainerFactoryForOrderCreated(
                        TypeMapping<OrderCreated> typeMapping) {
                return this.<OrderCreated>kafkaListenerContainerFactory(typeMapping);
        }

        @Bean
        public <E> ConsumerFactory<String, E> consumerFactory(TypeMapping<E> typeMapping) {
                Map<String, Object> config = new HashMap<>();

                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                                propertiesConfig.getKafkaAddress());
                config.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                JsonDeserializer.class);
                config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
                config.put(JsonDeserializer.TYPE_MAPPINGS, typeMapping.getValue());
                config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

                return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                                new ErrorHandlingDeserializer<>(new JsonDeserializer<>()));
        }

        @Bean
        public <E> ConcurrentKafkaListenerContainerFactory<String, E> kafkaListenerContainerFactory(
                        TypeMapping<E> typeMapping) {
                ConcurrentKafkaListenerContainerFactory<String, E> factory = new ConcurrentKafkaListenerContainerFactory<>();
                // factory.setCommonErrorHandler(commonErrorHandler());
                factory.setConsumerFactory(consumerFactory(typeMapping));
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
                                log.error("handleOne: kafka listener, error occured for topic: {}, key: {}, reason: {}",
                                                record.topic(),
                                                record.key(),
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
