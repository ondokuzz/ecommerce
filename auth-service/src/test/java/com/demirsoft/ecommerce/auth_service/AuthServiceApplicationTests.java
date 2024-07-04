package com.demirsoft.ecommerce.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceApplicationTests {

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
