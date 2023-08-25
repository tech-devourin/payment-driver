package com.devourin.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class SocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
      config.enableSimpleBroker("/terminal/queue");
      config.setApplicationDestinationPrefixes("/app");
      config.setUserDestinationPrefix("/terminal");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
      registry.addEndpoint("/ws")
      .setAllowedOriginPatterns("*").withSockJS();
  }
}
