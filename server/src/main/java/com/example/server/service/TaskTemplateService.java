package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.entity.TaskTemplate;
import com.example.server.mapper.TaskTemplateMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskTemplateService {
    private final TaskTemplateMapper taskTemplateMapper;

    public TaskTemplateService(TaskTemplateMapper taskTemplateMapper) {
        this.taskTemplateMapper = taskTemplateMapper;
    }

    public List<TaskTemplate> listByUser(Long userId) {
        QueryWrapper<TaskTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("updated_at");
        return taskTemplateMapper.selectList(wrapper);
    }

    public TaskTemplate create(TaskTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        taskTemplateMapper.insert(template);
        return template;
    }

    public TaskTemplate update(TaskTemplate template, Long userId) {
        TaskTemplate existing = taskTemplateMapper.selectById(template.getTemplateId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            return null;
        }
        existing.setTemplateName(template.getTemplateName());
        existing.setKeyword(template.getKeyword());
        existing.setUrl(template.getUrl());
        existing.setSiteType(template.getSiteType());
        existing.setMaxLinksPerLevel(template.getMaxLinksPerLevel());
        existing.setTagIdsJson(template.getTagIdsJson());
        existing.setUpdatedAt(LocalDateTime.now());
        taskTemplateMapper.updateById(existing);
        return existing;
    }

    public boolean delete(Long templateId, Long userId) {
        QueryWrapper<TaskTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_id", templateId).eq("user_id", userId);
        return taskTemplateMapper.delete(wrapper) > 0;
    }
}
