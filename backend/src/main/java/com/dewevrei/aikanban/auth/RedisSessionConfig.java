package com.dewevrei.aikanban.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration(proxyBeanMethods = false)
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400, redisNamespace = "ai-kanban:session")
public class RedisSessionConfig {
}
