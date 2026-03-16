package com.example.crawlernode.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.example.crawlernode.entity.result.CrawlerResult;
import com.example.crawlernode.entity.task.SubTask;
import com.example.crawlernode.enums.ClientStatusType;
import com.example.crawlernode.service.TaskExecutor;

import jakarta.websocket.*;
import tools.jackson.databind.ObjectMapper;

@ClientEndpoint
public class CrawlerNodeClientEndpoint {
    private Session session;
    private Timer heartbeatTimer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("WebSocket连接已建立");
        // 客户端在线消息
        sendStatus(ClientStatusType.ONLINE);        
        // 开启心跳检测
        startHeartbeat();
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            SubTask task = objectMapper.readValue(message, SubTask.class);
            System.out.println("收到服务端消息: " + task);
            // 发送客户端BUSY信息
            sendStatus(ClientStatusType.BUSY);
            // 爬虫运行，返回结果
            CrawlerResult result = TaskExecutor.execute(task);
            sendResult(result.getData());
            // 发送客户端在线消息
            sendStatus(ClientStatusType.ONLINE);
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("WebSocket连接已关闭: ");
        // 关闭心跳检测
        if (heartbeatTimer != null) heartbeatTimer.cancel();
        // 客户端下线消息
        sendStatus(ClientStatusType.OFFLINE);
    }

    // 开启心跳计时
    public void startHeartbeat(){
        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run(){
                if (session != null && session.isOpen()) {
                    // 发送心跳信息{"type":"heartbeat"}
                    session.getAsyncRemote().sendText("{\"type\":\"heartbeat\"}");
                }
            }
        }, 0, 15000); //每隔15秒发一次
    }

    // 发送爬虫结果
    public void sendResult(String result){
        if(session != null && session.isOpen()){
            Map<String,Object> msg = new HashMap<>();
            msg.put("type", "result");
            msg.put("data", result);
            String json = objectMapper.writeValueAsString(msg);
            session.getAsyncRemote().sendText(json);           
        }
    }

    // 发送爬虫节点状态
    public void sendStatus(ClientStatusType type) {
        if (session != null && session.isOpen()) {
            // 发送爬虫节点状态信息 {"type":"status","data":"ONLINE/BUSY/OFFLINE"}
            Map<String,Object> msg = new HashMap<>();
            msg.put("type", "status");
            msg.put("data", type.name());
            String json = objectMapper.writeValueAsString(msg);
            session.getAsyncRemote().sendText(json);
        }
    }
}
