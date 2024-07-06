package com.demirsoft.ecommerce.product_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
public class MongoReactiveConfig extends AbstractReactiveMongoConfiguration {
    @Bean
    ReactiveTransactionManager gReactiveTransactionManager(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory) {

        return new ReactiveMongoTransactionManager(reactiveMongoDatabaseFactory);
    }

    @Override
    @NonNull
    public MongoClient reactiveMongoClient() {
        return MongoClients.create();
    }

    @Override
    @NonNull
    protected String getDatabaseName() {
        return "ecomm";
    }

    @Bean
    public TransactionalOperator transactionalOperator(
            ReactiveTransactionManager reactiveTransactionManager) {
        return TransactionalOperator.create(reactiveTransactionManager);
    }
}