package com.example.server.websocket;


import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class CrawlerWebSocketHandler extends TextWebSocketHandler {
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("节点已连接: " + session.getId());
        // 可以记录session，后续推送任务
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到节点消息: " + payload);
        // 1、解析信息，是结果还是心跳还是更新爬虫节点状态
        // 解析节点状态/结果，分配任务
        // 示例：推送任务
        // session.sendMessage(new TextMessage("请执行任务xxx"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("节点断开: " + session.getId());
        // 清理资源
    }
}
