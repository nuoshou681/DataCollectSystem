// ServerWebSocketClient.java
package com.example.crawlernode.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import org.springframework.stereotype.Component;
import java.net.URI;

@Component
public class ServerWebSocketClient {

    @PostConstruct
    public void init() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            CrawlerNodeClientEndpoint endpoint = new CrawlerNodeClientEndpoint();
            container.connectToServer(endpoint, URI.create("ws://localhost:8080/ws/crawler"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}