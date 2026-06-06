package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.TaskGroup;
import com.example.server.entity.TaskGroupBinding;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskGroupBindingMapper;
import com.example.server.mapper.TaskGroupMapper;
import com.example.server.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskGroupService {
    private final TaskGroupMapper taskGroupMapper;
    private final TaskGroupBindingMapper taskGroupBindingMapper;
    private final CrawlerPageResultService crawlerPageResultService;
    private final TaskMapper taskMapper;

    public TaskGroupService(
            TaskGroupMapper taskGroupMapper,
            TaskGroupBindingMapper taskGroupBindingMapper,
            CrawlerPageResultService crawlerPageResultService,
            TaskMapper taskMapper) {
        this.taskGroupMapper = taskGroupMapper;
        this.taskGroupBindingMapper = taskGroupBindingMapper;
        this.crawlerPageResultService = crawlerPageResultService;
        this.taskMapper = taskMapper;
    }

    public List<TaskGroup> list(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return taskGroupMapper.selectList(new LambdaQueryWrapper<TaskGroup>()
                .eq(TaskGroup::getUserId, userId)
                .orderByAsc(TaskGroup::getGroupName));
    }

    public TaskGroup create(TaskGroup group, Long userId) {
        if (group == null || userId == null || group.getGroupName() == null || group.getGroupName().isBlank()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        group.setGroupId(null);
        group.setUserId(userId);
        group.setGroupName(group.getGroupName().trim());
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        taskGroupMapper.insert(group);
        return taskGroupMapper.selectById(group.getGroupId());
    }

    public boolean bind(Long taskId, Long groupId, Long userId, boolean isAdmin) {
        if (!crawlerPageResultService.canAccessTask(taskId, userId, isAdmin)) {
            return false;
        }
        TaskGroup group = taskGroupMapper.selectById(groupId);
        if (group == null || !userId.equals(group.getUserId())) {
            return false;
        }
        TaskGroupBinding existing = taskGroupBindingMapper.selectOne(new LambdaQueryWrapper<TaskGroupBinding>()
                .eq(TaskGroupBinding::getTaskId, taskId)
                .eq(TaskGroupBinding::getGroupId, groupId));
        if (existing != null) {
            return true;
        }
        TaskGroupBinding binding = new TaskGroupBinding();
        binding.setTaskId(taskId);
        binding.setGroupId(groupId);
        binding.setCreatedBy(userId);
        binding.setCreatedAt(LocalDateTime.now());
        taskGroupBindingMapper.insert(binding);
        return true;
    }

    public boolean unbind(Long taskId, Long groupId, Long userId, boolean isAdmin) {
        if (!crawlerPageResultService.canAccessTask(taskId, userId, isAdmin)) {
            return false;
        }
        TaskGroup group = taskGroupMapper.selectById(groupId);
        if (group == null || !userId.equals(group.getUserId())) {
            return false;
        }
        return taskGroupBindingMapper.delete(new LambdaQueryWrapper<TaskGroupBinding>()
                .eq(TaskGroupBinding::getTaskId, taskId)
                .eq(TaskGroupBinding::getGroupId, groupId)) > 0;
    }

    public boolean updateGroup(Long groupId, TaskGroup update, Long userId, boolean isAdmin) {
        if (update == null || groupId == null || userId == null) return false;
        TaskGroup group = taskGroupMapper.selectById(groupId);
        if (group == null || !userId.equals(group.getUserId())) return false;
        if (update.getGroupName() != null && !update.getGroupName().isBlank()) {
            group.setGroupName(update.getGroupName().trim());
        }
        if (update.getGroupColor() != null) {
            group.setGroupColor(update.getGroupColor());
        }
        if (update.getDescription() != null) {
            group.setDescription(update.getDescription());
        }
        group.setUpdatedAt(LocalDateTime.now());
        taskGroupMapper.updateById(group);
        return true;
    }

    public boolean deleteGroup(Long groupId, Long userId, boolean isAdmin) {
        if (groupId == null || userId == null) return false;
        TaskGroup group = taskGroupMapper.selectById(groupId);
        if (group == null || !userId.equals(group.getUserId())) return false;
        taskGroupBindingMapper.delete(new LambdaQueryWrapper<TaskGroupBinding>()
                .eq(TaskGroupBinding::getGroupId, groupId));
        taskGroupMapper.deleteById(groupId);
        return true;
    }

    public boolean batchBind(List<Long> taskIds, Long groupId, Long userId, boolean isAdmin) {
        if (taskIds == null || taskIds.isEmpty() || groupId == null) return false;
        boolean anyBound = false;
        for (Long taskId : taskIds) {
            if (bind(taskId, groupId, userId, isAdmin)) anyBound = true;
        }
        return anyBound;
    }

    public List<TaskGroupBinding> bindings(Long taskId, Long userId, boolean isAdmin) {
        if (!crawlerPageResultService.canAccessTask(taskId, userId, isAdmin)) {
            return List.of();
        }
        return taskGroupBindingMapper.selectList(new LambdaQueryWrapper<TaskGroupBinding>()
                .eq(TaskGroupBinding::getTaskId, taskId));
    }

    public List<TaskGroupBinding> allBindings(Long userId, boolean isAdmin) {
        List<TaskGroupBinding> bindings = taskGroupBindingMapper.selectList(new LambdaQueryWrapper<TaskGroupBinding>()
                .orderByDesc(TaskGroupBinding::getCreatedAt));
        if (isAdmin) {
            return bindings;
        }
        List<TaskGroupBinding> accessible = new ArrayList<>();
        for (TaskGroupBinding binding : bindings) {
            Task task = taskMapper.selectById(binding.getTaskId());
            if (task != null && userId != null && userId.equals(task.getUserId())) {
                accessible.add(binding);
            }
        }
        return accessible;
    }
}
