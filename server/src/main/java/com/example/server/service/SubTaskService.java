package com.example.server.service;

import com.example.server.entity.SubTask;
// ...existing code...
import com.example.server.entity.Client;
import com.example.server.mapper.SubTaskMapper;
import com.example.server.mapper.ClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
// ...existing code...
import java.util.List;

@Service
public class SubTaskService {
    @Autowired
    private SubTaskMapper subTaskMapper;
    @Autowired
    private ClientMapper clientMapper;

    /*分发子任务到可用客户端*/
    /**
     * 分发子任务到可用客户端，互斥访问，失败则回滚
     */
    public synchronized boolean dispatchSubTasks(List<SubTask> subTasks) {
        List<Client> clients = clientMapper.selectList(null);
        int clientCount = clients.size();
        if (clientCount == 0) {
            return false;
        }
        List<Long> insertedIds = new ArrayList<>();
        try {
            for (int i = 0; i < subTasks.size(); i++) {
                SubTask subTask = subTasks.get(i);
                // 简单轮询分配
                Client assigned = clients.get(i % clientCount);
                subTask.setAssigned_client(assigned.getId());
                subTask.setStatus("PENDING");
                subTaskMapper.insert(subTask);
                insertedIds.add(subTask.getId());
            }
            return true;
        } catch (Exception e) {
            // 回滚已插入的子任务
            for (Long id : insertedIds) {
                subTaskMapper.deleteById(id);
            }
            return false;
        }
    }

    // 其他子任务相关方法...
}
