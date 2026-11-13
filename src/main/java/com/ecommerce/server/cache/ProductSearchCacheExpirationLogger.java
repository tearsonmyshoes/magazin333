package com.ecommerce.server.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ProductSearchCacheExpirationLogger extends KeyExpirationEventMessageListener {
    private static final Logger log = LoggerFactory.getLogger(ProductSearchCacheExpirationLogger.class);

    public ProductSearchCacheExpirationLogger(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
        setKeyspaceNotificationsConfigParameter("Ex");
    }

    @Override
    protected void doHandleMessage(Message message) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (expiredKey.startsWith(ProductSearchCache.SEARCH_PATH + ":")) {
            log.info("Product search cache expired: key={}", expiredKey);
        }
    }
}