package com.demirsoft.ecommerce.api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class JwtUtil {

    public Claims getALlClaims(String token, String encodedPublicKeyBase64)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        log.info("getAllClaims called with token: " + token);
        log.info("getAllClaims called with secret: " + encodedPublicKeyBase64);
        log.info("getAllClaims called with secret bytes: "
                + new String(encodedPublicKeyBase64.getBytes(StandardCharsets.US_ASCII)));

        // return
        // Jwts.parserBuilder().setSigningKey(secret).build().parseClaimsJws(token).getBody();

        // CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // Certificate cert = cf
        // .generateCertificate(new
        // java.io.ByteArrayInputStream(secret.getBytes(StandardCharsets.US_ASCII)));

        byte[] encodedPublicKeyBytes = Decoders.BASE64.decode(encodedPublicKeyBase64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKeyBytes));

        // Now verify:
        return Jwts.parserBuilder()
                .setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token, String secret)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        return this.getALlClaims(token, secret).getExpiration().before(new Date());
    }

    public boolean isInValid(String token, String secret) {
        try {
            return this.isTokenExpired(token, secret);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return true;
        }
    }

}