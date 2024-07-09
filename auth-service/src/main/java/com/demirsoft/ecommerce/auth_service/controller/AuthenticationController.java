
package com.demirsoft.ecommerce.auth_service.controller;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.demirsoft.ecommerce.auth_service.config.OpenApiConfig;
import com.demirsoft.ecommerce.auth_service.dto.UserAuthenticationRequest;
import com.demirsoft.ecommerce.auth_service.dto.UserAuthenticationResponse;
import com.demirsoft.ecommerce.auth_service.dto.UserRegistrationRequest;
import com.demirsoft.ecommerce.auth_service.dto.UserUpdateRequest;
import com.demirsoft.ecommerce.auth_service.entity.User;
import com.demirsoft.ecommerce.auth_service.service.TokenService;
import com.demirsoft.ecommerce.auth_service.service.UserService;
import com.nimbusds.jose.jwk.JWKSet;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
public class AuthenticationController {

        @Autowired
        private UserService userService;

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private TokenService tokenService;

        @Autowired
        private JWKSet jwkSet;

        @Autowired
        @Qualifier("UserRegistrationRequestToUser")
        private ModelMapper modelMapperUserRegistrationToUser;

        @Autowired
        @Qualifier("UserUpdateRequestToUser")
        private ModelMapper modelMapperUserUpdateRequestToUser;

        @GetMapping("/public-key")
        @Hidden
        public ResponseEntity<String> getPublicKey() {
                return ResponseEntity.ok(tokenService.getPublicKey());
        }

        @GetMapping("/.well-known/jwks.json")
        @Hidden
        public Map<String, Object> keys() {
                return this.jwkSet.toJSONObject();
        }

        @PostMapping("/login")
        public ResponseEntity<UserAuthenticationResponse> login(
                        @Valid @RequestBody UserAuthenticationRequest authenticationRequest) {

                var authenticationToken = new UsernamePasswordAuthenticationToken(
                                authenticationRequest.getUsername(),
                                authenticationRequest.getPassword());

                var authentication = authenticationManager.authenticate(authenticationToken);

                authentication.getAuthorities().forEach(a -> log.info("authority: " + a.getAuthority()));
                var token = tokenService.generateToken(authentication);

                return ResponseEntity.ok(new UserAuthenticationResponse(token));
        }

        @PostMapping("/users")
        public ResponseEntity<UserAuthenticationResponse> createUser(
                        @Valid @RequestBody UserRegistrationRequest registrationRequest) {

                User newUser = modelMapperUserRegistrationToUser.map(registrationRequest, User.class);
                userService.save(newUser);

                return this.login(new UserAuthenticationRequest(registrationRequest.getUsername(),
                                registrationRequest.getPassword()));
        }

        @PutMapping("/users")
        @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
        @Tag(name = OpenApiConfig.ADD_AUTH_HEADER_TO_SWAGGER_DOC)
        @PreAuthorize("#updateRequest.username == authentication.name or  hasAuthority('SCOPE_ADMIN')")
        public ResponseEntity<User> updateUser(@Valid @RequestBody UserUpdateRequest updateRequest) {

                User user = modelMapperUserUpdateRequestToUser.map(updateRequest, User.class);

                User updatedUser = userService.update(user);

                return ResponseEntity.ok().body(updatedUser);
        }
}
