package com.olva.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.olva.config.RipleyAuthProperties;
import com.olva.dto.TokenResponse;
import com.olva.util.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Service
public class RipleyAuthService {

    private static final Logger log = LoggerFactory.getLogger(RipleyAuthService.class);
    private static final long JWT_EXPIRATION_SECONDS = 3600;

    private final RipleyAuthProperties properties;
    private final RipleyTokenHolder tokenHolder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final RestTemplate restTemplate;

    public RipleyAuthService(RipleyAuthProperties properties,
                             RipleyTokenHolder tokenHolder) {
        this.properties = properties;
        this.tokenHolder = tokenHolder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate(); // 👈 aquí
    }

    public String getValidToken() {
        if (tokenHolder.isValid()) {
            String token = tokenHolder.getToken();
            log.info("TokenHolder válido. length={}, token={}", token != null ? token.length() : 0, token);
            return token;
        }
        log.info("tokenHolder no válido");
        refreshToken();
        return tokenHolder.getToken();
    }

    public synchronized void refreshToken() {
        if (tokenHolder.isValid()) {
            return;
        }

        tokenHolder.clear();

        String jwt = buildJwt();
        TokenResponse response = requestToken(jwt);

        validateTokenResponse(response);

        String token = response.getAccessToken();

        System.out.println("TOKEN COMPLETO:\n" + token);

        tokenHolder.update(
                response.getAccessToken(),
                Instant.now().plusSeconds(response.getExpiresIn() - 60)
        );
    }

    public synchronized String forceRefreshToken() {
        tokenHolder.clear();
        refreshToken();
        return tokenHolder.getToken();
    }

    private String buildJwt() {
        try {
            validateRequiredProperties();

            PrivateKey privateKey = PemUtils.readPrivateKey(properties.getPrivateKeyPath());

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(properties.getKid())
                    .build();

            Instant now = Instant.now();
            Date iat = Date.from(now);
            Date exp = Date.from(now.plusSeconds(JWT_EXPIRATION_SECONDS));
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(properties.getIssuer())
                    .audience(properties.getTokenUrl())
                    .issueTime(iat)
                    .expirationTime(exp);

            long nowEpoch = now.getEpochSecond();
            long iatEpoch = iat.toInstant().getEpochSecond();
            long expEpoch = exp.toInstant().getEpochSecond();
            log.info("JWT DEBUG -> now: {}, iat: {}, exp: {}", nowEpoch, iatEpoch, expEpoch);
            log.info("JWT DEBUG -> lifetime (seconds): {}", (expEpoch - iatEpoch));

            if (properties.getScope() != null && !properties.getScope().isBlank()) {
                claimsBuilder.claim("scope", properties.getScope());
            }

            SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);

            String jwt = signedJWT.serialize();
            log.info("JWT firmado generado para Ripley: {}", jwt);
            return jwt;

        } catch (Exception e) {
            throw new IllegalStateException("Error generando JWT firmado para Ripley", e);
        }
    }

    private TokenResponse requestToken(String jwt) {

        // 🔹 Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // 🔹 Body FORM (IMPORTANTE: no manual string)
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("assertion", jwt);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        // 🔹 Logs controlados (sin exponer JWT completo)
        log.info("REQUEST TOKEN -> URL: {}", properties.getTokenUrl());
        log.info("REQUEST TOKEN -> Content-Type: application/x-www-form-urlencoded");
        log.info("REQUEST TOKEN -> assertion length: {}", jwt.length());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getTokenUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("RESPONSE TOKEN -> HTTP STATUS: {}", response.getStatusCode());

            // 🔹 Parse dentro del try
            TokenResponse tokenResponse = objectMapper.readValue(
                    response.getBody(),
                    TokenResponse.class
            );

            return tokenResponse;

        } catch (RestClientResponseException e) {

            log.error("ERROR TOKEN -> HTTP STATUS: {}", e.getStatusCode().value());
            log.error("ERROR TOKEN -> BODY: {}", e.getResponseBodyAsString());

            throw new IllegalStateException(
                    "Error solicitando access token a Ripley. HTTP "
                            + e.getStatusCode().value()
                            + " - "
                            + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Error parseando respuesta de token Ripley",
                    e
            );
        }
    }

    /*private String buildTokenRequestBody(String jwt) {
        return "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer"
                + "&assertion=" + jwt;
    }*/
    /*private String buildTokenRequestBody(String jwt) {
        return "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
    }*/
    private String buildTokenRequestBody(String jwt) {
        return "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer"
                + "&assertion=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
    }

    private void validateTokenResponse(TokenResponse response) {
        if (response == null || response.getAccessToken() == null || response.getAccessToken().isBlank()) {
            throw new IllegalStateException("Ripley no devolvio un access_token valido");
        }

        if (response.getTokenType() != null
                && !response.getTokenType().isBlank()
                && !"bearer".equalsIgnoreCase(response.getTokenType().trim())) {
            throw new IllegalStateException("Ripley devolvio un token_type no soportado: " + response.getTokenType());
        }

        if (response.getExpiresIn() <= 0) {
            throw new IllegalStateException("Ripley devolvio un expires_in invalido: " + response.getExpiresIn());
        }
    }

    private void validateRequiredProperties() {
        requireProperty(properties.getTokenUrl(), "integration.ripley.auth.token-url");
        requireProperty(properties.getIssuer(), "integration.ripley.auth.issuer");
        requireProperty(properties.getKid(), "integration.ripley.auth.kid");
        requireProperty(properties.getPrivateKeyPath(), "integration.ripley.auth.private-key-path");
    }

    private void requireProperty(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la propiedad obligatoria: " + propertyName);
        }
    }
}
