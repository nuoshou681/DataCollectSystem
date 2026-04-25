package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.ResultTag;
import com.example.server.entity.ResultTagBinding;
import com.example.server.entity.ResultTagView;
import com.example.server.mapper.ResultTagBindingMapper;
import com.example.server.mapper.ResultTagMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResultTagService {
    private final ResultTagMapper resultTagMapper;
    private final ResultTagBindingMapper resultTagBindingMapper;
    private final CrawlerPageResultService crawlerPageResultService;

    public ResultTagService(
            ResultTagMapper resultTagMapper,
            ResultTagBindingMapper resultTagBindingMapper,
            CrawlerPageResultService crawlerPageResultService) {
        this.resultTagMapper = resultTagMapper;
        this.resultTagBindingMapper = resultTagBindingMapper;
        this.crawlerPageResultService = crawlerPageResultService;
    }

    public List<ResultTagView> list(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<ResultTag> tags = resultTagMapper.selectList(new LambdaQueryWrapper<ResultTag>()
                .eq(ResultTag::getUserId, userId)
                .orderByAsc(ResultTag::getCategoryName)
                .orderByAsc(ResultTag::getTagName));
        if (tags.isEmpty()) {
            return List.of();
        }

        List<Long> tagIds = tags.stream().map(ResultTag::getTagId).toList();
        List<ResultTagBinding> bindings = resultTagBindingMapper.selectList(new LambdaQueryWrapper<ResultTagBinding>()
                .in(ResultTagBinding::getTagId, tagIds));

        Map<Long, List<ResultTagBinding>> bindingMap = bindings.stream()
                .collect(Collectors.groupingBy(ResultTagBinding::getTagId, LinkedHashMap::new, Collectors.toList()));

        List<ResultTagView> result = new ArrayList<>();
        for (ResultTag tag : tags) {
            List<ResultTagBinding> tagBindings = bindingMap.getOrDefault(tag.getTagId(), List.of());
            result.add(new ResultTagView(
                    tag.getTagId(),
                    tag.getTagName(),
                    tag.getTagColor(),
                    tag.getCategoryName(),
                    tag.getDescription(),
                    tagBindings.size(),
                    tag.getCreatedAt(),
                    tag.getUpdatedAt(),
                    tagBindings.stream().map(ResultTagBinding::getPageResultId).toList()));
        }
        return result;
    }

    public ResultTag create(ResultTag tag, Long userId) {
        if (tag == null || userId == null || tag.getTagName() == null || tag.getTagName().isBlank()) {
            return null;
        }
        ResultTag existing = resultTagMapper.selectOne(new LambdaQueryWrapper<ResultTag>()
                .eq(ResultTag::getUserId, userId)
                .eq(ResultTag::getTagName, tag.getTagName().trim()));
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        tag.setTagId(null);
        tag.setUserId(userId);
        tag.setTagName(tag.getTagName().trim());
        tag.setTagColor(blankToNull(tag.getTagColor()));
        tag.setCategoryName(blankToNull(tag.getCategoryName()));
        tag.setDescription(blankToNull(tag.getDescription()));
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);
        resultTagMapper.insert(tag);
        return resultTagMapper.selectById(tag.getTagId());
    }

    public ResultTag update(ResultTag tag, Long userId) {
        if (tag == null || tag.getTagId() == null || userId == null) {
            return null;
        }
        ResultTag existing = resultTagMapper.selectById(tag.getTagId());
        if (existing == null || !userId.equals(existing.getUserId())) {
            return null;
        }
        existing.setTagName(tag.getTagName() == null || tag.getTagName().isBlank() ? existing.getTagName() : tag.getTagName().trim());
        existing.setTagColor(blankToNull(tag.getTagColor()));
        existing.setCategoryName(blankToNull(tag.getCategoryName()));
        existing.setDescription(blankToNull(tag.getDescription()));
        existing.setUpdatedAt(LocalDateTime.now());
        resultTagMapper.updateById(existing);
        return resultTagMapper.selectById(existing.getTagId());
    }

    public boolean delete(Long tagId, Long userId) {
        if (tagId == null || userId == null) {
            return false;
        }
        ResultTag existing = resultTagMapper.selectById(tagId);
        if (existing == null || !userId.equals(existing.getUserId())) {
            return false;
        }
        return resultTagMapper.deleteById(tagId) > 0;
    }

    public boolean bind(Long tagId, Long pageResultId, Long userId, boolean isAdmin) {
        if (tagId == null || pageResultId == null || userId == null) {
            return false;
        }
        ResultTag tag = resultTagMapper.selectById(tagId);
        if (tag == null || !userId.equals(tag.getUserId())) {
            return false;
        }
        if (!crawlerPageResultService.canAccessPageResult(pageResultId, userId, isAdmin)) {
            return false;
        }
        ResultTagBinding existing = resultTagBindingMapper.selectOne(new LambdaQueryWrapper<ResultTagBinding>()
                .eq(ResultTagBinding::getTagId, tagId)
                .eq(ResultTagBinding::getPageResultId, pageResultId));
        if (existing != null) {
            return true;
        }
        ResultTagBinding binding = new ResultTagBinding();
        binding.setTagId(tagId);
        binding.setPageResultId(pageResultId);
        binding.setCreatedBy(userId);
        binding.setCreatedAt(LocalDateTime.now());
        resultTagBindingMapper.insert(binding);
        return true;
    }

    public boolean unbind(Long tagId, Long pageResultId, Long userId) {
        if (tagId == null || pageResultId == null || userId == null) {
            return false;
        }
        ResultTag tag = resultTagMapper.selectById(tagId);
        if (tag == null || !userId.equals(tag.getUserId())) {
            return false;
        }
        return resultTagBindingMapper.delete(new LambdaQueryWrapper<ResultTagBinding>()
                .eq(ResultTagBinding::getTagId, tagId)
                .eq(ResultTagBinding::getPageResultId, pageResultId)) > 0;
    }

    public Map<Long, List<ResultTagView>> queryPageResultTags(Long userId, List<Long> pageResultIds) {
        if (userId == null || pageResultIds == null || pageResultIds.isEmpty()) {
            return Map.of();
        }
        List<ResultTag> tags = resultTagMapper.selectList(new LambdaQueryWrapper<ResultTag>()
                .eq(ResultTag::getUserId, userId));
        if (tags.isEmpty()) {
            return Map.of();
        }
        Map<Long, ResultTag> tagMap = tags.stream().collect(Collectors.toMap(ResultTag::getTagId, item -> item));
        List<ResultTagBinding> bindings = resultTagBindingMapper.selectList(new LambdaQueryWrapper<ResultTagBinding>()
                .in(ResultTagBinding::getPageResultId, pageResultIds)
                .in(ResultTagBinding::getTagId, tagMap.keySet()));

        Map<Long, List<ResultTagView>> result = new LinkedHashMap<>();
        for (ResultTagBinding binding : bindings) {
            ResultTag tag = tagMap.get(binding.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(binding.getPageResultId(), key -> new ArrayList<>())
                    .add(new ResultTagView(
                            tag.getTagId(),
                            tag.getTagName(),
                            tag.getTagColor(),
                            tag.getCategoryName(),
                            tag.getDescription(),
                            null,
                            tag.getCreatedAt(),
                            tag.getUpdatedAt(),
                            List.of(binding.getPageResultId())));
        }
        return result;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
