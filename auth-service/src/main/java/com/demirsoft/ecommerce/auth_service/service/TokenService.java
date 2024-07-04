
package com.demirsoft.ecommerce.auth_service.service;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final RSAPublicKey publicKey;
    private final JwtEncoder jwtEncoder;

    public TokenService(RSAPublicKey publicKey, JwtEncoder jwtEncoder) {
        this.publicKey = publicKey;
        this.jwtEncoder = jwtEncoder;
    }

    public String getPublicKey() {
        return new String(Base64.getEncoder().encode(publicKey.getEncoded()));
    }

    public String generateToken(Authentication authentication) {
        var scope = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(90, ChronoUnit.MINUTES))
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
