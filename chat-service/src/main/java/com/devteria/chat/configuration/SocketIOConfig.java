package com.devteria.chat.configuration;

import org.springframework.context.annotation.Bean;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

@org.springframework.context.annotation.Configuration
public class SocketIOConfig {
    @Bean
    public SocketIOServer socketIOServer() {
        Configuration configuration = new Configuration();
        configuration.setPort(8099);
        configuration.setOrigin("*");

        return new SocketIOServer(configuration);
    }
}
