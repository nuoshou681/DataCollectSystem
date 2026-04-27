package com.example.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.Task;
import com.example.server.entity.TaskDetailView;
import com.example.server.entity.TaskEvent;
import com.example.server.entity.TaskFile;
import com.example.server.entity.TaskRuntime;
import com.example.server.entity.Message.CrawlerTaskMessage;
import com.example.server.mapper.CrawlerPageResultMapper;
import com.example.server.service.TaskBatchService;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.TaskTagBindingService;
import com.example.server.service.TaskEventService;
import com.example.server.service.TaskFileService;
import com.example.server.service.TaskRuntimeService;
import com.example.server.service.TaskService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskMapper taskMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TaskRuntimeService taskRuntimeService;
    private final TaskEventService taskEventService;
    private final TaskFileService taskFileService;
    private final CrawlerPageResultMapper crawlerPageResultMapper;
    private final TaskBatchService taskBatchService;
    private final TaskTagBindingService taskTagBindingService;

    public TaskServiceImpl(
            TaskMapper taskMapper,
            RabbitTemplate rabbitTemplate,
            TaskRuntimeService taskRuntimeService,
            TaskEventService taskEventService,
            TaskFileService taskFileService,
            CrawlerPageResultMapper crawlerPageResultMapper,
            TaskBatchService taskBatchService,
            TaskTagBindingService taskTagBindingService) {
        this.taskMapper = taskMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.taskRuntimeService = taskRuntimeService;
        this.taskEventService = taskEventService;
        this.taskFileService = taskFileService;
        this.crawlerPageResultMapper = crawlerPageResultMapper;
        this.taskBatchService = taskBatchService;
        this.taskTagBindingService = taskTagBindingService;
    }

    @Override
    public List<Task> dispatchTasks(DispatchTaskRequest request) {
        List<Task> tasks = buildTasks(request);
        taskBatchService.createBatchIfAbsent(resolveBatchId(request), request.getBatchName(), request.getUserId(), tasks.size(), request.getBatchNotes());
        for (Task task : tasks) {
            taskMapper.insert(task);
            taskTagBindingService.bindTagsToTask(task.getTaskId(), task.getTagIds(), task.getUserId());
            taskRuntimeService.createQueuedRuntime(task);
            taskEventService.recordEvent(task.getTaskId(), task.getNodeId(), "TASK_CREATED", "INFO", "任务已创建", null);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CRAWLER_EXCHANGE,
                    RabbitMQConfig.ROUTING_TASK,
                    toCrawlerTaskMessage(task));
            taskEventService.recordEvent(task.getTaskId(), task.getNodeId(), "TASK_DISPATCHED", "INFO", "任务已派发到消息队列", null);
        }
        return tasks;
    }

    @Override
    public List<Task> dispatchBatchTasks(DispatchTaskRequest request) {
        List<Task> tasks = buildTasksForKeywords(request);
        taskBatchService.createBatchIfAbsent(resolveBatchId(request), request.getBatchName(), request.getUserId(), tasks.size(), request.getBatchNotes());
        for (Task task : tasks) {
            taskMapper.insert(task);
            taskTagBindingService.bindTagsToTask(task.getTaskId(), task.getTagIds(), task.getUserId());
            taskRuntimeService.createQueuedRuntime(task);
            taskEventService.recordEvent(task.getTaskId(), task.getNodeId(), "TASK_CREATED", "INFO", "批量任务已创建", null);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CRAWLER_EXCHANGE,
                    RabbitMQConfig.ROUTING_TASK,
                    toCrawlerTaskMessage(task));
            taskEventService.recordEvent(task.getTaskId(), task.getNodeId(), "TASK_DISPATCHED", "INFO", "批量任务已派发到消息队列", null);
        }
        return tasks;
    }

    @Override
    public List<Task> queryTasks(Long userId, boolean isAdmin) {
        LambdaQueryWrapper<Task> query = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            query.eq(Task::getUserId, userId);
        }
        query.orderByDesc(Task::getTaskId);
        List<Task> tasks = taskMapper.selectList(query);
        tasks.forEach(task -> task.setRuntime(taskRuntimeService.getByTaskId(task.getTaskId())));
        return tasks;
    }

    @Override
    public TaskDetailView queryTaskDetail(Long taskId, Long userId, boolean isAdmin) {
        if (taskId == null) {
            return null;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        if (!isAdmin && (userId == null || !userId.equals(task.getUserId()))) {
            return null;
        }

        TaskRuntime runtime = taskRuntimeService.getByTaskId(taskId);
        List<TaskEvent> events = taskEventService.queryByTaskId(taskId, userId, isAdmin);
        List<TaskFile> files = taskFileService.queryByTaskId(taskId, userId, isAdmin);
        List<CrawlerPageResultRecord> pageResults = crawlerPageResultMapper.selectList(new LambdaQueryWrapper<CrawlerPageResultRecord>()
                .eq(CrawlerPageResultRecord::getTaskId, taskId)
                .orderByAsc(CrawlerPageResultRecord::getPageIndex)
                .orderByAsc(CrawlerPageResultRecord::getPageResultId));
        return new TaskDetailView(task, runtime, events, files, pageResults);
    }

    @Override
    public boolean updateArchived(Long taskId, boolean archived, Long userId, boolean isAdmin) {
        if (taskId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        if (!isAdmin && (userId == null || !userId.equals(task.getUserId()))) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        task.setArchived(archived);
        task.setArchivedAt(archived ? now : null);
        task.setUpdatedAt(now);
        taskMapper.updateById(task);
        taskEventService.recordEvent(
                taskId,
                task.getNodeId(),
                archived ? "TASK_ARCHIVED" : "TASK_UNARCHIVED",
                "INFO",
                archived ? "任务已归档" : "任务已取消归档",
                null);
        return true;
    }

    private List<Task> buildTasks(DispatchTaskRequest request) {
        if (request == null || request.getUrl() == null) {
            throw new IllegalArgumentException("task 或 url 不能为空");
        }

        String[] urls = request.getUrl().split("[,;\\n\\r]+");
        List<Task> tasks = new ArrayList<>();
        String batchId = resolveBatchId(request);

        for (String rawUrl : urls) {
            String url = rawUrl.trim();
            if (url.isEmpty()) {
                continue;
            }

            Task task = new Task();
            LocalDateTime now = LocalDateTime.now();
            task.setUserId(request.getUserId());
            task.setUrl(url);
            task.setKeyword(blankToNull(request.getKeyword()));
            task.setSiteType(blankToNull(request.getSiteType()));
            task.setTaskStatus("PENDING");
            task.setTaskProgress(0);
            task.setTotalPages(0);
            task.setMaxLinksPerLevel(request.getMaxLinksPerLevel() == null ? 10 : request.getMaxLinksPerLevel());
            task.setPriority(request.getPriority() == null ? 0 : request.getPriority());
            task.setSource(blankToNull(request.getSource()) == null ? "manual" : request.getSource());
            task.setBatchId(batchId);
            task.setIdempotencyKey(blankToNull(request.getIdempotencyKey()));
            task.setRetryCount(0);
            task.setCancelRequested(false);
            task.setArchived(false);
            task.setArchivedAt(null);
            task.setTagIds(request.getTagIds());
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            tasks.add(task);
        }
        return tasks;
    }

    private List<Task> buildTasksForKeywords(DispatchTaskRequest request) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            throw new IllegalArgumentException("批量任务种子链接不能为空");
        }
        if (request.getKeywordsText() == null || request.getKeywordsText().isBlank()) {
            throw new IllegalArgumentException("批量关键词不能为空");
        }

        String[] keywords = request.getKeywordsText().split("[,;\\n\\r]+");
        List<Task> tasks = new ArrayList<>();
        String batchId = resolveBatchId(request);
        for (String rawKeyword : keywords) {
            String keyword = rawKeyword.trim();
            if (keyword.isEmpty()) {
                continue;
            }
            Task task = new Task();
            LocalDateTime now = LocalDateTime.now();
            task.setUserId(request.getUserId());
            task.setUrl(request.getUrl().trim());
            task.setKeyword(keyword);
            task.setSiteType(blankToNull(request.getSiteType()));
            task.setTaskStatus("PENDING");
            task.setTaskProgress(0);
            task.setTotalPages(0);
            task.setMaxLinksPerLevel(request.getMaxLinksPerLevel() == null ? 10 : request.getMaxLinksPerLevel());
            task.setPriority(request.getPriority() == null ? 0 : request.getPriority());
            task.setSource(blankToNull(request.getSource()) == null ? "manual" : request.getSource());
            task.setBatchId(batchId);
            task.setIdempotencyKey(UUID.randomUUID().toString());
            task.setRetryCount(0);
            task.setCancelRequested(false);
            task.setArchived(false);
            task.setArchivedAt(null);
            task.setTagIds(request.getTagIds());
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            tasks.add(task);
        }
        return tasks;
    }

    private String resolveBatchId(DispatchTaskRequest request) {
        String batchId = blankToNull(request.getBatchId());
        if (batchId == null) {
            batchId = UUID.randomUUID().toString().replace("-", "");
        }
        return batchId;
    }

    private CrawlerTaskMessage toCrawlerTaskMessage(Task task) {
        return new CrawlerTaskMessage(
                task.getTaskId(),
                task.getUserId(),
                task.getNodeId(),
                task.getBatchId(),
                task.getUrl(),
                task.getKeyword(),
                task.getSiteType(),
                task.getMaxLinksPerLevel(),
                task.getPriority(),
                task.getSource(),
                task.getIdempotencyKey(),
                task.getTagIds());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
