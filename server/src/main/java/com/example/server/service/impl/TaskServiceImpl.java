package com.example.server.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import com.example.server.entity.SubTask;
import com.example.server.entity.Task;
import com.example.server.mapper.SubTaskMapper;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.SubTaskService;
import com.example.server.service.TaskService;

@Service
public class TaskServiceImpl implements TaskService {
    @Autowired
    private SubTaskService subTaskService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private SubTaskMapper subTaskMapper;

    @Override
    public void splitAndDispatchSubTasks(Task task) {
        // 1.保存task到数据库
        taskMapper.insert(task);

        // 2.将task拆分为subtasks
        List<SubTask> subTasks = buildSubTasks(task);

        Long taskId = task.getTaskId();
        String taskKeyword = task.getKeyword();

        // 3.保存所有 SubTask到数据库中
        for (SubTask subTask : subTasks) {
            subTask.setTaskId(taskId);
            subTask.setKeyword(taskKeyword);
            subTaskMapper.insert(subTask);
        }

        // 4.发放子任务对象到MQ
        subTaskService.dispatchSubTasks(subTasks);
    }

    @Override
    public List<SubTask> buildSubTasks(Task task) {
        // url的格式：url列表使用换行分隔开
        if (task == null || task.getUrl() == null) {
            throw new IllegalArgumentException("task 或 url 不能为空");
        }
        // 拆分url
        String[] urls = task.getUrl().split("[,;\\n\\r]+");
        List<SubTask> subTasks = new ArrayList<>();
        for (String url : urls) {
            url = url.trim();
            if (url.isEmpty())
                continue;
            SubTask subTask = new SubTask();
            subTask.setUrl(url);
            subTasks.add(subTask);
        }
        return subTasks;
    }

}
