package com.ecommerce.server.cache;

import com.ecommerce.server.dto.product.ProductFilterRequest;
import com.ecommerce.server.dto.product.ProductResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

/**
 * Stores only the responses of POST /api/products/search.
 *
 * <p>Redis key: {@code /api/products/search:<canonical request body>}.<br>
 * Redis value: JSON response body.<br>
 * TTL: 60 seconds by default.</p>
 */
@Component
public class ProductSearchCache {
    public static final String SEARCH_PATH = "/api/products/search";

    private static final Logger log = LoggerFactory.getLogger(ProductSearchCache.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JavaType productListType;
    private final Duration ttl;

    public ProductSearchCache(
            StringRedisTemplate redisTemplate,
            @Value("${app.cache.product-search.ttl:60s}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = JsonMapper.builder().build();
        this.productListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ProductResponse.class);
        this.ttl = ttl;
    }

    public List<ProductResponse> get(ProductFilterRequest request) {
        String key = buildKey(request);

        try {
            String cachedValue = redisTemplate.opsForValue().get(key);

            if (cachedValue == null) {
                log.debug("Product search cache miss: key={}", key);
                return null;
            }

            log.debug("Product search cache hit: key={}", key);
            return objectMapper.readValue(cachedValue, productListType);
        } catch (JacksonException exception) {
            log.warn("Cannot read product search cache, removing key={}", key, exception);
            redisTemplate.delete(key);
            return null;
        } catch (DataAccessException exception) {
            log.warn("Redis is unavailable while reading product search cache: key={}", key, exception);
            return null;
        }
    }

    public void put(ProductFilterRequest request, List<ProductResponse> response) {
        String key = buildKey(request);

        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, value, ttl);
            log.info("Product search response cached: key={}, ttl={}s", key, ttl.toSeconds());
        } catch (JacksonException exception) {
            log.warn("Cannot serialize product search response for caching: key={}", key, exception);
        } catch (DataAccessException exception) {
            log.warn("Redis is unavailable while writing product search cache: key={}", key, exception);
        }
    }

    public String buildKey(ProductFilterRequest request) {
        return SEARCH_PATH + ":" + toJson(request);
    }

    private String toJson(ProductFilterRequest request) {
        if (request == null) {
            return "";
        }

        try {
            return objectMapper.writeValueAsString(request);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize product search request", exception);
        }
    }
}