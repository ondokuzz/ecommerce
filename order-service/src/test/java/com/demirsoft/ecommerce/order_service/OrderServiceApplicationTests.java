package com.demirsoft.ecommerce.order_service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;

import org.assertj.core.util.Arrays;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.demirsoft.ecommerce.order_service.dto.CartDto;
import com.demirsoft.ecommerce.order_service.dto.OrderDto;
import com.demirsoft.ecommerce.order_service.entity.Address;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.entity.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
		"spring.kafka.consumer.auto-offset-reset=earliest",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
		"spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
		"spring.kafka.consumer.group-id=order-service-test",
		"spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
		"spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
		"spring.kafka.consumer.properties.spring.json.trusted.packages=*"
})
@EnableKafka
public class OrderServiceApplicationTests {

	@Container
	public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10");
	@Autowired
	private MongoTemplate mongoTemplate;

	@ClassRule
	// public static KafkaContainer kafka = new
	// KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));
	// // @Autowired
	// // private KafkaTemplate<String, String> kafkaTemplate;

	// @Autowired
	// private KafkaConsumer consumer;

	// @Autowired
	// private KafkaProducer producer;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeAll
	public static void setUpClass() {
		mongoDBContainer.start();
		System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
		// System.setProperty("spring.kafka.bootstrap-servers",
		// kafkaContainer.getBootstrapServers());
	}

	@BeforeEach
	public void setUp(WebApplicationContext context) {
		mongoTemplate.getDb().drop();
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

	// private Cart createFullCart(Long customerId) {
	// OrderItem item1 = new OrderItem();
	// item1.setProductId("100");
	// item1.setPrice(11.0);

	// OrderItem item2 = new OrderItem();
	// item2.setProductId("101");
	// item2.setPrice(12.0);

	// var items = new LinkedList<OrderItem>();
	// items.add(item1);
	// items.add(item2);

	// Cart newCart = new Cart("1", customerId, items);

	// return newCart;
	// }

	private static final String CREDIT_CARD_NO = "555444666";

	private OrderDto createFullOrderDto(long customerId) {
		return new OrderDto(1L, new Address("turkiye", "istanbul", "kosuyolu"), CREDIT_CARD_NO);
	}

	@Test
	public void testKafkaSendAndReceive() throws Exception {
		// String testMessage = "Hello, Kafka!";
		// kafkaTemplate.send("test-topic", testMessage);

		// Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup",
		// "true",
		// kafkaContainer.getBootstrapServers());
		// consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
		// StringDeserializer.class);
		// consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
		// StringDeserializer.class);
		// consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		// DefaultKafkaConsumerFactory<String, String> consumerFactory = new
		// DefaultKafkaConsumerFactory<>(consumerProps);
		// ContainerProperties containerProperties = new
		// ContainerProperties("test-topic");

		// KafkaMessageListenerContainer<String, String> container = new
		// KafkaMessageListenerContainer<>(consumerFactory,
		// containerProperties);
		// container.setupMessageListener((MessageListener<String, String>) record -> {
		// log.info("kafka working");
		// });
		// container.start();

		// // wait for the listener to receive the message
		// Thread.sleep(5000);

		// container.stop();

		// String data = "Sending with our own simple KafkaProducer";

		// producer.send(new ProducerRecord("order-topic", data));

		// var messageConsumed = consumer.poll(Duration.ofSeconds(10));
		// log.info("aaa {}", messageConsumed);

	}

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
}
