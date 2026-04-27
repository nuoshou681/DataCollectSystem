package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.ResultTag;
import com.example.server.entity.TaskTagBinding;
import com.example.server.mapper.ResultTagMapper;
import com.example.server.mapper.TaskTagBindingMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskTagBindingService {
    private final TaskTagBindingMapper taskTagBindingMapper;
    private final ResultTagMapper resultTagMapper;
    private final ResultTagService resultTagService;

    public TaskTagBindingService(
            TaskTagBindingMapper taskTagBindingMapper,
            ResultTagMapper resultTagMapper,
            ResultTagService resultTagService) {
        this.taskTagBindingMapper = taskTagBindingMapper;
        this.resultTagMapper = resultTagMapper;
        this.resultTagService = resultTagService;
    }

    public void bindTagsToTask(Long taskId, List<Long> tagIds, Long userId) {
        if (taskId == null || userId == null || tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> distinctTagIds = tagIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        for (Long tagId : distinctTagIds) {
            ResultTag tag = resultTagMapper.selectById(tagId);
            if (tag == null || !userId.equals(tag.getUserId())) {
                continue;
            }
            TaskTagBinding existing = taskTagBindingMapper.selectOne(new LambdaQueryWrapper<TaskTagBinding>()
                    .eq(TaskTagBinding::getTaskId, taskId)
                    .eq(TaskTagBinding::getTagId, tagId));
            if (existing != null) {
                continue;
            }
            TaskTagBinding binding = new TaskTagBinding();
            binding.setTaskId(taskId);
            binding.setTagId(tagId);
            binding.setCreatedBy(userId);
            binding.setCreatedAt(LocalDateTime.now());
            taskTagBindingMapper.insert(binding);
        }
    }

    public void inheritTaskTagsToPageResult(Long taskId, Long pageResultId, Long userId) {
        if (taskId == null || pageResultId == null || userId == null) {
            return;
        }
        List<TaskTagBinding> taskBindings = taskTagBindingMapper.selectList(new LambdaQueryWrapper<TaskTagBinding>()
                .eq(TaskTagBinding::getTaskId, taskId));
        for (TaskTagBinding taskBinding : taskBindings) {
            resultTagService.bind(taskBinding.getTagId(), pageResultId, userId, false);
        }
    }
}
