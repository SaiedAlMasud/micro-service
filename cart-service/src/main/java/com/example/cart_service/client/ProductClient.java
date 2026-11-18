package com.example.cart_service.client;

import com.example.cart_service.dto.BulkProductRequest;
import com.example.cart_service.dto.BulkProductResponse;
import com.example.cart_service.dto.ProductDto;
import com.example.cart_service.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private final RestClient restClient;

    public ProductClient(
            @Value("${product.service.url:http://localhost:8082}") String baseUrl,
            @Value("${product.service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${product.service.read-timeout-ms:3000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<ProductDto> getProduct(UUID productId) {
        int maxAttempts = 3; // 1 initial + 2 retries
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ProductDto product = restClient.get()
                        .uri("/api/v1/products/{id}", productId)
                        .retrieve()
                        .body(ProductDto.class);
                return Optional.ofNullable(product);
            } catch (HttpClientErrorException.NotFound ex) {
                log.warn("Product {} not found in Product Service", productId);
                return Optional.empty();
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return Optional.empty();
                }
                log.error("Client error from Product Service: status {}", ex.getStatusCode(), ex);
                throw new ServiceUnavailableException("Product Service returned client error: " + ex.getStatusCode(), ex);
            } catch (HttpServerErrorException ex) {
                log.warn("Server error from Product Service on attempt {}/{}: status {}", attempt, maxAttempts, ex.getStatusCode());
                if (attempt == maxAttempts) {
                    throw new ServiceUnavailableException("Product Service is temporarily unavailable: " + ex.getStatusCode(), ex);
                }
                backoff(attempt);
            } catch (ResourceAccessException ex) {
                log.warn("Connection/timeout error contacting Product Service on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
                if (attempt == maxAttempts) {
                    throw new ServiceUnavailableException("Product Service is unreachable or timed out: " + ex.getMessage(), ex);
                }
                backoff(attempt);
            } catch (Exception ex) {
                log.error("Unexpected error contacting Product Service", ex);
                throw new ServiceUnavailableException("Failed to communicate with Product Service: " + ex.getMessage(), ex);
            }
        }
        return Optional.empty();
    }

    public BulkProductResponse getBulkProducts(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new BulkProductResponse(List.of(), List.of());
        }

        try {
            BulkProductResponse response = restClient.post()
                    .uri("/api/v1/products/bulk")
                    .body(new BulkProductRequest(productIds))
                    .retrieve()
                    .body(BulkProductResponse.class);

            return response != null ? response : new BulkProductResponse(List.of(), productIds);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("HTTP error from Product Service bulk endpoint: status {}", ex.getStatusCode(), ex);
            throw new ServiceUnavailableException("Product Service bulk lookup failed: " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Connection/timeout contacting Product Service bulk endpoint: {}", ex.getMessage(), ex);
            throw new ServiceUnavailableException("Product Service is unreachable: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Failed to fetch bulk products from Product Service", ex);
            throw new ServiceUnavailableException("Failed to communicate with Product Service: " + ex.getMessage(), ex);
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(attempt * 100L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

