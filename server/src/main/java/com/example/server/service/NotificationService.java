package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.server.entity.Notification;
import com.example.server.mapper.NotificationMapper;
import com.example.server.mapper.TaskMapper;
import com.example.server.entity.Task;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final TaskMapper taskMapper;

    public NotificationService(NotificationMapper notificationMapper, TaskMapper taskMapper) {
        this.notificationMapper = notificationMapper;
        this.taskMapper = taskMapper;
    }

    @PostConstruct
    void seedStartupNotification() {
        try {
            Notification latest = notificationMapper.selectOne(new QueryWrapper<Notification>()
                    .eq("type", "SYSTEM").orderByDesc("created_at").last("LIMIT 1"));
            String version = getClass().getPackage().getImplementationVersion();
            String v = version != null ? " v" + version : "";
            create(1L, "SYSTEM", "INFO", "系统已启动" + v, "DataCollectSystem 服务端已就绪，通知中心正常工作", null);
        } catch (Exception ignored) {
            // Notification table may not exist yet on first startup
        }
    }

    public void create(Long userId, String type, String level, String title, String content, String link) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setLevel(level != null ? level : "INFO");
        n.setTitle(title);
        n.setContent(content);
        n.setLink(link);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    public void create(Long userId, String type, String title, String content) {
        create(userId, type, "INFO", title, content, null);
    }

    public void createForTask(Long taskId, String type, String level, String title, String content, String link) {
        Task task = taskMapper.selectById(taskId);
        if (task != null && task.getUserId() != null) {
            create(task.getUserId(), type, level, title, content, link);
        }
    }

    public void createForTask(Long taskId, String type, String title, String content) {
        createForTask(taskId, type, "INFO", title, content, null);
    }

    public void notifyTaskOwners(List<Long> taskIds, String type, String level, String titleTemplate, String linkTemplate) {
        if (taskIds == null || taskIds.isEmpty()) return;
        List<Task> tasks = taskMapper.selectBatchIds(taskIds);
        Set<Long> notified = new java.util.HashSet<>();
        for (Task task : tasks) {
            if (task != null && task.getUserId() != null && notified.add(task.getUserId())) {
                String title = titleTemplate.replace("{taskId}", String.valueOf(task.getTaskId()));
                String link = linkTemplate != null ? linkTemplate.replace("{taskId}", String.valueOf(task.getTaskId())) : null;
                create(task.getUserId(), type, level, title, "任务 #" + task.getTaskId(), link);
            }
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

    public List<Notification> listAll(Boolean unreadOnly) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq("is_read", false);
        }
        wrapper.orderByDesc("created_at");
        return notificationMapper.selectList(wrapper);
    }

    public int unreadCountAll() {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("is_read", false);
        return Math.toIntExact(notificationMapper.selectCount(wrapper));
    }

    public boolean markRead(Long notificationId, Long userId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("notification_id", notificationId).eq("user_id", userId).set("is_read", true);
        return notificationMapper.update(null, wrapper) > 0;
    }

    public boolean markReadAny(Long notificationId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("notification_id", notificationId).set("is_read", true);
        return notificationMapper.update(null, wrapper) > 0;
    }

    public int markAllRead(Long userId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId).eq("is_read", false).set("is_read", true);
        return notificationMapper.update(null, wrapper);
    }

    public int markAllReadAny() {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("is_read", false).set("is_read", true);
        return notificationMapper.update(null, wrapper);
    }

    public boolean delete(Long notificationId, Long userId) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("notification_id", notificationId).eq("user_id", userId);
        return notificationMapper.delete(wrapper) > 0;
    }

    public boolean deleteAny(Long notificationId) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("notification_id", notificationId);
        return notificationMapper.delete(wrapper) > 0;
    }
}
