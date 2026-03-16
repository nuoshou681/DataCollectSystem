package com.example.server.service;

import com.example.server.entity.UserTask;
import com.example.server.entity.SubTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserTaskService {
    @Autowired
    private SubTaskService subTaskService;

    /**
     * 拆分用户任务为子任务并分发
     */
    public void splitAndDispatchSubTasks(UserTask userTask) {
        // 示例：按逗号分割目标URL为多个子任务
        String[] urls = userTask.getTargetUrl().split(",");
        List<SubTask> subTasks = new ArrayList<>();
        for (String url : urls) {
            SubTask subTask = new SubTask();
            subTask.setTaskId(userTask.getId());
            subTask.setSubtaskName(userTask.getTaskName() + "-" + url.trim());
            subTask.setTargetUrl(url.trim());
            subTask.setStatus("INIT");
            subTask.setProgress(0);
            subTasks.add(subTask);
        }
        // 分发子任务，失败则可处理主任务状态
        boolean success = subTaskService.dispatchSubTasks(subTasks);
        if (!success) {
            // 可在此处回滚主任务或设置失败状态
            userTask.setStatus("DISPATCH_FAILED");
            // TODO: 持久化主任务状态
        }
    }
}
