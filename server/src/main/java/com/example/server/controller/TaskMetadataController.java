package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.TaskGroup;
import com.example.server.entity.TaskGroupBinding;
import com.example.server.entity.TaskNote;
import com.example.server.service.TaskGroupService;
import com.example.server.service.TaskNoteService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/metadata")
public class TaskMetadataController {
    private final TaskNoteService taskNoteService;
    private final TaskGroupService taskGroupService;

    public TaskMetadataController(TaskNoteService taskNoteService, TaskGroupService taskGroupService) {
        this.taskNoteService = taskNoteService;
        this.taskGroupService = taskGroupService;
    }

    @GetMapping("/{taskId}/notes")
    public ApiResponse<List<TaskNote>> notes(@PathVariable Long taskId) {
        return ApiResponse.success(taskNoteService.list(taskId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }

    @PostMapping("/{taskId}/notes")
    public ApiResponse<?> createNote(@PathVariable Long taskId, @RequestBody TaskNote note) {
        TaskNote created = taskNoteService.create(taskId, note == null ? null : note.getNoteContent(), SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
        if (created == null) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限添加任务备注");
        }
        return ApiResponse.success(created);
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<?> updateNote(@PathVariable Long noteId, @RequestBody TaskNote note) {
        TaskNote updated = taskNoteService.update(noteId, note == null ? null : note.getNoteContent(), SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
        if (updated == null) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限修改任务备注");
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/notes/{noteId}")
    public ApiResponse<?> deleteNote(@PathVariable Long noteId) {
        if (!taskNoteService.delete(noteId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限删除任务备注");
        }
        return ApiResponse.success(true);
    }

    @GetMapping("/groups")
    public ApiResponse<List<TaskGroup>> groups() {
        return ApiResponse.success(taskGroupService.list(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/groups")
    public ApiResponse<?> createGroup(@RequestBody TaskGroup group) {
        TaskGroup created = taskGroupService.create(group, SecurityUtils.getCurrentUserId());
        if (created == null) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "任务分组名称不能为空");
        }
        return ApiResponse.success(created);
    }

    @GetMapping("/{taskId}/groups")
    public ApiResponse<List<TaskGroupBinding>> taskGroups(@PathVariable Long taskId) {
        return ApiResponse.success(taskGroupService.bindings(taskId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }

    @GetMapping("/groups/bindings")
    public ApiResponse<List<TaskGroupBinding>> allGroupBindings() {
        return ApiResponse.success(taskGroupService.allBindings(SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }

    @PostMapping("/{taskId}/groups")
    public ApiResponse<?> bindGroup(@PathVariable Long taskId, @RequestParam Long groupId) {
        if (!taskGroupService.bind(taskId, groupId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限绑定任务分组");
        }
        return ApiResponse.success(true);
    }

    @DeleteMapping("/{taskId}/groups/{groupId}")
    public ApiResponse<?> unbindGroup(@PathVariable Long taskId, @PathVariable Long groupId) {
        if (!taskGroupService.unbind(taskId, groupId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限移除任务分组");
        }
        return ApiResponse.success(true);
    }

    @PutMapping("/groups/{groupId}")
    public ApiResponse<?> updateGroup(@PathVariable Long groupId, @RequestBody TaskGroup group) {
        if (!taskGroupService.updateGroup(groupId, group, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限修改分组");
        }
        return ApiResponse.success(true);
    }

    @DeleteMapping("/groups/{groupId}")
    public ApiResponse<?> deleteGroup(@PathVariable Long groupId) {
        if (!taskGroupService.deleteGroup(groupId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限删除分组");
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/groups/batch-bind")
    public ApiResponse<?> batchBind(@RequestBody java.util.Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idsRaw = (List<Integer>) body.get("taskIds");
        Integer gid = body.get("groupId") instanceof Integer ? (Integer) body.get("groupId") : null;
        if (idsRaw == null || idsRaw.isEmpty() || gid == null) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "参数错误");
        }
        List<Long> taskIds = idsRaw.stream().map(Long::valueOf).toList();
        if (!taskGroupService.batchBind(taskIds, gid.longValue(), SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限批量绑定分组");
        }
        return ApiResponse.success(Map.of("count", taskIds.size()));
    }
}
