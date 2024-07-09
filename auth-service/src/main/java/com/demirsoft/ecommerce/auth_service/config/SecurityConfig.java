
package com.demirsoft.ecommerce.auth_service.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.UUID;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.spi.MappingContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.demirsoft.ecommerce.auth_service.dto.UserRegistrationRequest;
import com.demirsoft.ecommerce.auth_service.dto.UserUpdateRequest;
import com.demirsoft.ecommerce.auth_service.entity.Role;
import com.demirsoft.ecommerce.auth_service.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.extern.log4j.Log4j2;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Log4j2
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, HandlerMappingIntrospector introspector)
            throws Exception {

        log.info("security filter chain being visited");
        return httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/users").permitAll()
                        .requestMatchers("/public-key").permitAll()
                        .requestMatchers("/.well-known/jwks.json").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll().anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .httpBasic(Customizer.withDefaults())
                .headers(header -> {
                    header.frameOptions((frameOptions) -> frameOptions.sameOrigin());
                })
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOrigin("http://localhost:8080");
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        var authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public JWKSet jwkSet() {
        return new JWKSet(rsaKey());
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = jwkSet();
        return (((jwkSelector, securityContext) -> jwkSelector.select(jwkSet)));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        log.info("jwt nimbus encoder is being created");
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder() throws JOSEException {
        log.info("jwt nimbus decoder is being created");
        return NimbusJwtDecoder.withPublicKey(rsaKey().toRSAPublicKey()).build();
    }

    @Bean
    public RSAPublicKey publicKey() throws JOSEException {
        return rsaKey().toRSAPublicKey();
    }

    @Bean
    public RSAKey rsaKey() {

        KeyPair keyPair = keyPair();
        log.info("rsa key public: " + new String(Base64.getEncoder().encode(keyPair.getPublic().getEncoded())));
        log.info("rsa key private: " + new String(Base64.getEncoder().encode(keyPair.getPrivate().getEncoded())));

        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString()).build();
    }

    @Bean
    public KeyPair keyPair() {
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            log.info("rsa key pair called: ");
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate an RSA Key Pair", e);
        }
    }

    @Bean
    @Qualifier("UserRegistrationRequestToUser")
    public ModelMapper modelMapperForUserRegistrationRequestToUser() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<UserRegistrationRequest, User>() {
            protected void configure() {
                Converter<String, Role> stringToRoleConverter = new Converter<String, Role>() {
                    public Role convert(MappingContext<String, Role> context) {
                        return Role.valueOf(context.getSource());
                    }
                };
                map().setUsername(source.getUsername());
                map().setPassword(source.getPassword());
                map().setEmail(source.getEmail());
                map(source.getAddress().getState(), destination.getAddress().getState());
                map(source.getAddress().getCity(), destination.getAddress().getCity());
                map(source.getAddress().getStreet(), destination.getAddress().getStreet());

                using(stringToRoleConverter).map(source.getRole(), destination.getRole());
            }
        });

        return modelMapper;
    }

    @Bean
    @Qualifier("UserUpdateRequestToUser")
    public ModelMapper modelMapperForUserUpdateRequestToUser() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<UserUpdateRequest, User>() {
            protected void configure() {
                Converter<String, Role> stringToRoleConverter = new Converter<String, Role>() {
                    public Role convert(MappingContext<String, Role> context) {
                        return Role.valueOf(context.getSource());
                    }
                };
                map().setUsername(source.getUsername());
                map().setPassword(source.getPassword());
                map().setEmail(source.getEmail());
                map(source.getAddress().getState(), destination.getAddress().getState());
                map(source.getAddress().getCity(), destination.getAddress().getCity());
                map(source.getAddress().getStreet(), destination.getAddress().getStreet());

                using(stringToRoleConverter).map(source.getRole(), destination.getRole());
            }
        });

        return modelMapper;
    }

}
