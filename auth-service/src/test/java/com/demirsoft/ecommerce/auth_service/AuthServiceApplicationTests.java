package com.demirsoft.ecommerce.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.demirsoft.ecommerce.auth_service.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
class AuthServiceApplicationTests {

	@MockBean
	UserRepository repository;

	@Test
	void contextLoads() {
	}

	// a regular USER can update only its own info
	// a regular USER cant update its own role
	// a ADMIN user can update its own info
	// a ADMIN user can update another USERs info
	// a ADMIN user cant update its own role
	// a ADMIN user can update other regular USERs role
	// updating a non existing user throws UserNotFoundException

}
