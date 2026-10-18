package com.example.demo.controller;

import com.example.demo.entity.TrainingProject;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实训项目接口（内存模拟数据，用于接口测试 / Apifox 调试）
 */
@RestController
@RequestMapping("/api/projects")
public class TrainingProjectController {

    /** 内存存储，模拟数据库 */
    private final Map<Long, TrainingProject> store = new ConcurrentHashMap<>();

    /** 自增主键 */
    private final AtomicLong idGenerator = new AtomicLong(0);

    public TrainingProjectController() {
        // 初始化示例数据，便于列表、详情与分页测试
        add("校园导览小程序", "基于微信小程序实现校园地图导览", "进行中", "张三", 7);
        add("数据库课程设计", "完成学生信息管理系统的数据库设计", "待开始", "李四", 14);
        add("智能图书推荐系统", "基于协同过滤算法实现图书推荐", "已完成", "王五", 3);
        add("实验室预约平台", "实现实验室在线预约与审批流程", "进行中", "赵六", 10);
        add("毕业设计选题系统", "完成选题、审核、双向选择流程", "待开始", "孙七", 21);
        add("校园二手交易平台", "实现闲置物品发布与交易", "已完成", "周八", 5);
    }

    /** 1. 查询实训项目列表（模拟数据，可选按状态筛选） */
    @GetMapping
    public List<TrainingProject> list(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return new ArrayList<>(store.values());
        }
        return store.values().stream()
                .filter(p -> status.equals(p.getStatus()))
                .toList();
    }

    /** 2. 按 id 查询单个实训项目详情（@PathVariable） */
    @GetMapping("/{id}")
    public TrainingProject getById(@PathVariable Long id) {
        return store.get(id);
    }

    /** 3. 分页查询（@RequestParam 接收页码、每页条数） */
    @GetMapping("/page")
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        List<TrainingProject> all = store.values().stream()
                .sorted(Comparator.comparing(TrainingProject::getId))
                .toList();
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<TrainingProject> records = from < total ? all.subList(from, to) : List.of();

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (total + size - 1) / size);
        result.put("records", records);
        return result;
    }

    /** 新增项目：POST /api/projects */
    @PostMapping
    public TrainingProject create(@RequestBody TrainingProject project) {
        project.setId(idGenerator.incrementAndGet());
        project.setCreateTime(LocalDateTime.now());
        store.put(project.getId(), project);
        return project;
    }

    /** 修改项目：PUT /api/projects/{id} */
    @PutMapping("/{id}")
    public TrainingProject update(@PathVariable Long id, @RequestBody TrainingProject project) {
        project.setId(id);
        store.put(id, project);
        return project;
    }

    /** 删除项目：DELETE /api/projects/{id} */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        TrainingProject removed = store.remove(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", removed != null);
        result.put("deletedId", id);
        return result;
    }

    /** 统计信息：GET /api/projects/stats */
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        long total = store.size();
        long ongoing = store.values().stream().filter(p -> "进行中".equals(p.getStatus())).count();
        long done = store.values().stream().filter(p -> "已完成".equals(p.getStatus())).count();
        return Map.of("total", total, "ongoing", ongoing, "done", done);
    }

    /** 初始化示例数据 */
    private void add(String title, String description, String status, String assignee, long days) {
        TrainingProject p = new TrainingProject();
        p.setId(idGenerator.incrementAndGet());
        p.setTitle(title);
        p.setDescription(description);
        p.setStatus(status);
        p.setAssignee(assignee);
        p.setDeadline(LocalDateTime.now().plusDays(days));
        p.setCreateTime(LocalDateTime.now());
        store.put(p.getId(), p);
    }
}
