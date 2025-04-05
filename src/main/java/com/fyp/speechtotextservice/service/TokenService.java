package com.fyp.speechtotextservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private final WebClient webClient;

    public TokenService(@Value("${assemblyai.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.assemblyai.com/v2/realtime")
                .defaultHeader(HttpHeaders.AUTHORIZATION, apiKey)
                .build();
    }

    public Mono<String> generateTemporaryToken() {
        return webClient.post()
                .uri("/token")
                .bodyValue("{\"expires_in\": 3600}")
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::getToken)
                .doOnSuccess(token -> log.info("Generated temporary token: {}", token))
                .doOnError(error -> log.error("Error generating token: {}", error.getMessage()));
    }
}
class TokenResponse {
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}