package com.demirsoft.ecommerce.order_service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.util.Arrays;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.demirsoft.ecommerce.order_service.dto.CartDto;
import com.demirsoft.ecommerce.order_service.dto.OrderDto;
import com.demirsoft.ecommerce.order_service.entity.Address;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.entity.OrderStatus;
import com.demirsoft.ecommerce.order_service.event.OrderCreated;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Log4j2
public class OrderServiceApplicationTests {

	@Container
	public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10");
	@Autowired
	private MongoTemplate mongoTemplate;

	@SuppressWarnings("resource")
	@ClassRule
	public static KafkaContainer kafkaContainer = new KafkaContainer(
			DockerImageName.parse("confluentinc/cp-kafka:latest"))
			.withKraft()
			.withEnv("TOPIC_AUTO_CREATE", "true")
			.withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

	private static final Map<String, Object> producerProps = new HashMap<>();
	private static final Map<String, Object> consumerProps = new HashMap<>();
	private static final Map<String, KafkaProducer<String, ?>> producers = new HashMap<>();
	private static final Map<String, KafkaConsumer<String, ?>> consumers = new HashMap<>();
	private static Admin admin;
	private static final Map<String, NewTopic> topics = new HashMap<>();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeAll
	public static void setUpClass() {
		mongoDBContainer.start();
		System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());

		log.info("starting kafka container");
		kafkaContainer.start();
		System.setProperty("ecommerce.config.kafka-address", kafkaContainer.getBootstrapServers());
		System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());

		createKafkaAdmin();
	}

	@AfterAll
	private static void tearDownClass() {
		producers.entrySet().stream().forEach(pe -> pe.getValue().close());
		consumers.entrySet().stream().forEach(ce -> ce.getValue().close());
		admin.close();
		kafkaContainer.stop();
		kafkaContainer.close();
		mongoDBContainer.stop();
		mongoDBContainer.close();
	}

	@BeforeEach
	public void setUp(WebApplicationContext context) throws Exception {
		mongoTemplate.getDb().drop();
		recreateTopic(OrderCreated.class);
		subscribeForEvent(OrderCreated.class);
	}

	@AfterEach
	public void tearDown(WebApplicationContext context) throws Exception {
		unsubscribeFromEvent(OrderCreated.class);
	}

	private CartDto createEmptyCartDto(Long customerId) {
		var items = new LinkedList<OrderItem>();

		CartDto newCartDto = new CartDto(customerId, items);

		return newCartDto;
	}

	private CartDto createFullCartDto(Long customerId) {
		OrderItem item1 = new OrderItem();
		item1.setProductId("100");
		item1.setQuantity(3);
		item1.setPrice(11.0);

		OrderItem item2 = new OrderItem();
		item2.setProductId("101");
		item2.setQuantity(4);
		item2.setPrice(12.0);

		var items = new LinkedList<OrderItem>();
		items.add(item1);
		items.add(item2);

		CartDto newCartDto = new CartDto(customerId, items);

		return newCartDto;
	}

	private static final String CREDIT_CARD_NO = "555444666";

	private OrderDto createFullOrderDto(long customerId) {
		return new OrderDto(1L, new Address("turkiye", "istanbul", "kosuyolu"), CREDIT_CARD_NO);
	}

	// @Test
	// public void kafkaTest() throws Exception {
	// var consumer = getKafkaConsumer(OrderCreated.class);
	// var producer = getKafkaProducer(OrderCreated.class);

	// subscribeForEvent(OrderCreated.class);

	// producer.send(
	// new ProducerRecord<String, OrderCreated>(OrderCreated.class.getName(),
	// new OrderCreated("234", Long.valueOf(1L),
	// List.of(new OrderItem("123", 23, 98.0)), 345.0, "2323",
	// new Address("tr", "ist", "kosuyolu"))));

	// producer.send(
	// new ProducerRecord<String, OrderCreated>(OrderCreated.class.getName(),
	// new OrderCreated("234", Long.valueOf(1L),
	// List.of(new OrderItem("123", 23, 98.0)), 345.0, "2323",
	// new Address("tr", "ist", "kosuyolu"))));

	// var records = waitForEventInstance(OrderCreated.class);
	// records.forEach(r -> {
	// log.info("kafka key: {}", r.key());
	// log.info("kafka value: {}", r.value());
	// });

	// }

	@Test
	public void givenCustomerId_whenGetCartOrCreateCalledTwice_thenReturnTheSameCart() throws Exception {
		Long customerId = 1L;
		mockMvc.perform(get("/carts/{customerId}", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

		mockMvc.perform(get("/carts/{customerId}", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));
	}

	@Test
	public void givenExistingCart_whenUpdateCart_thenCartUpdated() throws Exception {
		CartDto cartDto = createFullCartDto(1L);

		// create an empty cart first
		mockMvc.perform(get("/carts/{customerId}", cartDto.getCustomerId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(cartDto.getCustomerId()))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

		// update it
		mockMvc.perform(put("/carts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(cartDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(cartDto.getCustomerId()))
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))))
				.andExpect(jsonPath("$.items[0].productId").value(cartDto.getItems().get(0).getProductId()))
				.andExpect(jsonPath("$.items[0].quantity").value(cartDto.getItems().get(0).getQuantity()))
				.andExpect(jsonPath("$.items[0].price").value(cartDto.getItems().get(0).getPrice()))
				.andExpect(jsonPath("$.items[1].productId").value(cartDto.getItems().get(1).getProductId()))
				.andExpect(jsonPath("$.items[1].quantity").value(cartDto.getItems().get(1).getQuantity()))
				.andExpect(jsonPath("$.items[1].price").value(cartDto.getItems().get(1).getPrice()));

		// update cart
		cartDto.getItems().get(0).setProductId("200");
		cartDto.getItems().get(0).setQuantity(5);
		cartDto.getItems().get(0).setPrice(13.0);
		cartDto.getItems().get(1).setProductId("201");
		cartDto.getItems().get(1).setQuantity(6);
		cartDto.getItems().get(1).setPrice(14.0);
		mockMvc.perform(put("/carts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(cartDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(cartDto.getCustomerId()))
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))))
				.andExpect(jsonPath("$.items[0].productId").value(cartDto.getItems().get(0).getProductId()))
				.andExpect(jsonPath("$.items[0].quantity").value(cartDto.getItems().get(0).getQuantity()))
				.andExpect(jsonPath("$.items[0].price").value(cartDto.getItems().get(0).getPrice()))
				.andExpect(jsonPath("$.items[1].productId").value(cartDto.getItems().get(1).getProductId()))
				.andExpect(jsonPath("$.items[1].quantity").value(cartDto.getItems().get(1).getQuantity()))
				.andExpect(jsonPath("$.items[1].price").value(cartDto.getItems().get(1).getPrice()));
	}

	@Test
	public void givenEmptyCart_whenCreateOrder_thenReturnFailure() throws Exception {
		Long customerId = 1L;
		OrderDto orderDto = createFullOrderDto(customerId);

		CartDto cartDto = createEmptyCartDto(customerId);

		// create an empty cart first
		mockMvc.perform(get("/carts/{customerId}", cartDto.getCustomerId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(cartDto.getCustomerId()))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

		// make order request and get error
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(orderDto)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void givenFullCart_whenCreateOrder_thenReturnSuccess() throws Exception {
		Long customerId = 1L;
		OrderDto orderDto = createFullOrderDto(customerId);

		CartDto cartDto = createFullCartDto(customerId);

		// create an empty cart first
		mockMvc.perform(get("/carts/{customerId}", cartDto.getCustomerId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(cartDto.getCustomerId()))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

		// fill the cart
		mockMvc.perform(put("/carts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(cartDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))));

		// make order request
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(orderDto)))
				.andDo(h -> {
					OrderCreated orderCreated = createOrderCreatedFromDtos(orderDto, cartDto);

					var receivedRecords = waitForEventInstance(OrderCreated.class);

					assertNotNull(receivedRecords);
					receivedRecords.forEach(r -> log.info("received OrderCreated instance: {}", r.value()));
					assertEquals(1, receivedRecords.count());
					assertEquals(receivedRecords.iterator().hasNext(), true);
					OrderCreated received = receivedRecords.iterator().next().value();
					assertNotNull(received.getId());
					assertEquals(orderCreated.getCustomerId(), received.getCustomerId());
					assertEquals(orderCreated.getItems(), received.getItems());
					assertEquals(orderCreated.getPrice(), received.getPrice());
					assertEquals(orderCreated.getItems(), received.getItems());
					assertEquals(orderCreated.getCreditCardInfo(), received.getCreditCardInfo());
					assertEquals(orderCreated.getShippingAddress(), received.getShippingAddress());
				})
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(orderDto.getCustomerId()))
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))))
				.andExpect(jsonPath("$.items[0].productId").value(cartDto.getItems().get(0).getProductId()))
				.andExpect(jsonPath("$.items[0].quantity").value(cartDto.getItems().get(0).getQuantity()))
				.andExpect(jsonPath("$.items[0].price").value(cartDto.getItems().get(0).getPrice()))
				.andExpect(jsonPath("$.items[1].productId").value(cartDto.getItems().get(1).getProductId()))
				.andExpect(jsonPath("$.items[1].quantity").value(cartDto.getItems().get(1).getQuantity()))
				.andExpect(jsonPath("$.items[1].price").value(cartDto.getItems().get(1).getPrice()))
				.andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
				.andExpect(jsonPath("$.price").value(23.0))
				.andExpect(jsonPath("$.creditCardInfo").value(CREDIT_CARD_NO))
				.andExpect(jsonPath("$.shippingAddress.state").value("turkiye"))
				.andExpect(jsonPath("$.shippingAddress.city").value("istanbul"))
				.andExpect(jsonPath("$.shippingAddress.street").value("kosuyolu"));

		// make sure cart is cleared
		mockMvc.perform(get("/carts/{customerId}", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

	}

	private OrderCreated createOrderCreatedFromDtos(OrderDto orderDto, CartDto cartDto) {
		OrderCreated orderCreated = new OrderCreated(null,
				1L,
				cartDto.getItems().stream().toList(),
				cartDto.getItems().stream().mapToDouble(OrderItem::getPrice).sum(),
				orderDto.getCreditCardInfo(),
				orderDto.getShippingAddress());
		return orderCreated;
	}

	@Test
	public void givenNonExistingOrder_whenGetOrder_thenReturnFailure() throws Exception {
		String invalidOrderId = "5678765";

		// get order with the invalid id
		mockMvc.perform(get("/orders/{id}", invalidOrderId))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void givenExistingOrder_whenGetOrder_thenReturnOrder() throws Exception {
		Long customerId = 1L;
		OrderDto orderDto = createFullOrderDto(customerId);

		CartDto cartDto = createFullCartDto(customerId);

		// create an empty cart first
		mockMvc.perform(get("/carts/{customerId}", cartDto.getCustomerId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(cartDto.getCustomerId()))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

		// fill the cart
		mockMvc.perform(put("/carts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(cartDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))));

		// make order request
		String response = mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(orderDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		Order order = objectMapper.readValue(response, Order.class);

		// get order with the obtained id
		mockMvc.perform(get("/orders/{id}", order.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(order.getId()))
				.andExpect(jsonPath("$.customerId").value(orderDto.getCustomerId()))
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))))
				.andExpect(jsonPath("$.items[0].productId").value(cartDto.getItems().get(0).getProductId()))
				.andExpect(jsonPath("$.items[0].quantity").value(cartDto.getItems().get(0).getQuantity()))
				.andExpect(jsonPath("$.items[0].price").value(cartDto.getItems().get(0).getPrice()))
				.andExpect(jsonPath("$.items[1].productId").value(cartDto.getItems().get(1).getProductId()))
				.andExpect(jsonPath("$.items[1].quantity").value(cartDto.getItems().get(1).getQuantity()))
				.andExpect(jsonPath("$.items[1].price").value(cartDto.getItems().get(1).getPrice()))
				.andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
				.andExpect(jsonPath("$.price").value(23.0))
				.andExpect(jsonPath("$.creditCardInfo").value(CREDIT_CARD_NO))
				.andExpect(jsonPath("$.shippingAddress.state").value("turkiye"))
				.andExpect(jsonPath("$.shippingAddress.city").value("istanbul"))
				.andExpect(jsonPath("$.shippingAddress.street").value("kosuyolu"));
	}

	@Test
	public void givenMultipleOrdersForACustomer_whenGetOrdersByCustomerId_thenReturnOrdersForThatCustomer()
			throws Exception {
		Long customerId = 1L;

		OrderDto orderDto = createFullOrderDto(customerId);

		// create an empty cart first
		mockMvc.perform(get("/carts/{customerId}", customerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andExpect(jsonPath("$.items", hasSize(equalTo(0))));

		// fill the cart
		// create 2 orders for the same customer
		CartDto cartDto = createFullCartDto(customerId);
		mockMvc.perform(put("/carts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(cartDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(2))));

		// make order request
		String response = mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(orderDto)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Order order1 = objectMapper.readValue(response, Order.class);

		// fill the cart again this time with 1 element
		cartDto = createFullCartDto(customerId);
		cartDto.getItems().removeLast();

		mockMvc.perform(put("/carts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(cartDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items", hasSize(equalTo(1))));

		// make order request
		response = mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(orderDto)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Order order2 = objectMapper.readValue(response, Order.class);

		response = mockMvc.perform(get("/orders/customer/{customerId}", customerId))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Order[] orders = objectMapper.readValue(response, Order[].class);

		Arrays.asList(orders).stream().forEach(System.out::println);

		assertEquals(2, orders.length);
		assertEquals(order1, orders[0]);
		assertEquals(order2, orders[1]);

	}

	private static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String, Object> getProducerProps() {
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

		return producerProps;
	}

	private static Map<String, Object> getConsumerProps() {
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
		return consumerProps;
	}

	private static void createKafkaAdmin() {
		Properties properties = new Properties();
		properties.put(
				AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
				kafkaContainer.getBootstrapServers());
		admin = Admin.create(properties);
		log.info("kafka admin created");
	}

	private static <T> void createTopicAsSingleton(Class<T> clazz) throws InterruptedException, ExecutionException {
		createAsSingleton(
				topics,
				clazz.getCanonicalName(),
				() -> createTopic(clazz));
	}

	private static <T> void recreateTopic(Class<T> clazz) throws InterruptedException, ExecutionException {
		deleteTopicFromSingleton(clazz);
		createTopicAsSingleton(clazz);
	}

	private static <T> void deleteTopicFromSingleton(Class<T> clazz) {
		log.info("deleting topic for: {}", clazz.getSimpleName());

		try {
			admin.deleteTopics(List.of(clazz.getSimpleName())).all().get();
			topics.remove(clazz.getCanonicalName());
			log.info("deleted topic");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static <T> NewTopic createTopic(Class<T> clazz) {
		log.info("creating topic for: {}", clazz.getSimpleName());
		NewTopic newTopic = TopicBuilder
				.name(clazz.getSimpleName())
				.build();

		try {
			admin.createTopics(List.of(newTopic)).all().get();
			log.info("created topic");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return newTopic;
	}

	@SuppressWarnings("unchecked")
	private <E> KafkaConsumer<String, E> getKafkaConsumer(Class<E> clazz) throws Exception {
		createTopicAsSingleton(clazz);

		return (KafkaConsumer<String, E>) createAsSingleton(
				consumers,
				clazz.getCanonicalName(),
				() -> new KafkaConsumer<String, E>(getConsumerProps()));
	}

	@SuppressWarnings("unchecked")
	private <E> KafkaProducer<String, E> getKafkaProducer(Class<E> clazz) throws Exception {
		createTopicAsSingleton(clazz);

		return (KafkaProducer<String, E>) createAsSingleton(
				producers,
				clazz.getCanonicalName(),
				() -> new KafkaProducer<String, E>(getProducerProps()));
	}

	private static <T, M extends Map<String, T>> T createAsSingleton(M map, String className,
			Supplier<T> supplier) {
		return (T) map.computeIfAbsent(className, k -> supplier.get());
	}

	@SuppressWarnings("unchecked")
	private <E> RecordMetadata sendEvent(E instanceToSend) throws Exception {
		Class<E> eventClass = (Class<E>) instanceToSend.getClass();
		KafkaProducer<String, E> producer = getKafkaProducer(eventClass);
		return producer.send(new ProducerRecord<String, E>(eventClass.getSimpleName(), instanceToSend))
				.get();
	}

	private <E extends Object> void subscribeForEvent(Class<E> clazz) throws Exception {
		createTopicAsSingleton(clazz);

		getKafkaConsumer(clazz).subscribe(List.of(clazz.getSimpleName()));
	}

	private <E extends Object> ConsumerRecords<String, E> waitForEventInstance(Class<E> clazz)
			throws Exception {

		return getKafkaConsumer(clazz).poll(Duration.ofSeconds(30));
	}

	private <E extends Object> void unsubscribeFromEvent(Class<E> clazz) throws Exception {
		getKafkaConsumer((Class<E>) clazz).unsubscribe();
	}

}
