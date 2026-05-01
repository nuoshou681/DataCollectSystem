package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.server.entity.Notification;
import com.example.server.mapper.NotificationMapper;
import com.example.server.mapper.TaskMapper;
import com.example.server.entity.Task;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final TaskMapper taskMapper;

    public NotificationService(NotificationMapper notificationMapper, TaskMapper taskMapper) {
        this.notificationMapper = notificationMapper;
        this.taskMapper = taskMapper;
    }

    public void create(Long userId, String type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    public void createForTask(Long taskId, String type, String title, String content) {
        Task task = taskMapper.selectById(taskId);
        if (task != null && task.getUserId() != null) {
            create(task.getUserId(), type, title, content);
        }
    }

    public List<Notification> listByUser(Long userId, Boolean unreadOnly) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq("is_read", false);
        }
        wrapper.orderByDesc("created_at");
        return notificationMapper.selectList(wrapper);
    }

    public int unreadCount(Long userId) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_read", false);
        return Math.toIntExact(notificationMapper.selectCount(wrapper));
    }

    public boolean markRead(Long notificationId, Long userId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("notification_id", notificationId).eq("user_id", userId).set("is_read", true);
        return notificationMapper.update(null, wrapper) > 0;
    }

    public int markAllRead(Long userId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId).eq("is_read", false).set("is_read", true);
        return notificationMapper.update(null, wrapper);
    }

    public boolean delete(Long notificationId, Long userId) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("notification_id", notificationId).eq("user_id", userId);
        return notificationMapper.delete(wrapper) > 0;
    }
}
