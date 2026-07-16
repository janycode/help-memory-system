package com.example.mynewwork.service;

import com.example.mynewwork.exception.EntityNotFoundException;
import com.example.mynewwork.model.entity.Iteration;
import com.example.mynewwork.model.entity.IterationImportConfig;
import com.example.mynewwork.model.entity.IterationSyncHistory;
import com.example.mynewwork.repository.IterationImportConfigRepository;
import com.example.mynewwork.repository.IterationRepository;
import com.example.mynewwork.repository.IterationSyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IterationService {

    private final IterationRepository iterationRepository;
    private final IterationImportConfigRepository importConfigRepository;
    private final IterationSyncHistoryRepository syncHistoryRepository;

    private static final Map<String, Integer> STATUS_ORDER = Map.of(
            "TODO", 1,
            "IN_PROGRESS", 2,
            "TESTING", 3,
            "DONE", 4
    );

    public Optional<Iteration> findById(Long id) {
        log.debug("根据ID查询迭代: {}", id);
        return iterationRepository.findByIdAndActiveTrue(id);
    }

    public Page<Iteration> findAll(Pageable pageable) {
        return findAll(null, null, pageable);
    }

    public Page<Iteration> findAll(String status, String priority, Pageable pageable) {
        log.debug("分页查询迭代, status={}, priority={}", status, priority);

        Page<Iteration> page;
        if (status != null && !status.isEmpty()) {
            page = iterationRepository.findByStatusAndActiveTrue(status, pageable);
        } else if (priority != null && !priority.isEmpty()) {
            page = iterationRepository.findByPriorityAndActiveTrue(priority, pageable);
        } else {
            page = iterationRepository.findByActiveTrue(pageable);
        }

        if (status == null || status.isEmpty()) {
            List<Iteration> sorted = page.getContent().stream()
                    .sorted(Comparator.comparingInt(i -> STATUS_ORDER.getOrDefault(i.getStatus(), 99)))
                    .toList();
            return new PageImpl<>(sorted, pageable, page.getTotalElements());
        }

        return page;
    }

    public List<Iteration> findAllActive() {
        log.debug("查询所有活跃的迭代");
        return iterationRepository.findByActiveTrue();
    }

    public List<Iteration> searchByKeyword(String keyword) {
        log.debug("根据关键字搜索迭代: {}", keyword);
        return iterationRepository.findActiveByKeyword(keyword);
    }

    public List<Iteration> findByStatus(String status) {
        log.debug("按状态查询迭代: {}", status);
        return iterationRepository.findByStatusAndActiveTrue(status);
    }

    public List<Iteration> findByPriority(String priority) {
        log.debug("按优先级查询迭代: {}", priority);
        return iterationRepository.findByPriorityAndActiveTrue(priority);
    }

    public long countActive() {
        return iterationRepository.countByActiveTrue();
    }

    @Transactional
    public Iteration createIteration(Iteration iteration, Long userId) {
        log.info("创建迭代: {}", iteration.getTitle());

        iteration.setCreatedBy(userId);
        iteration.setCreatedAt(LocalDateTime.now());
        iteration.setUpdatedAt(LocalDateTime.now());
        iteration.setActive(true);

        Iteration saved = iterationRepository.save(iteration);

        // 创建本地目录和文件（会修改 localDirPath 和 releaseNotes）
        createLocalDirAndFiles(saved);

        // 二次保存以持久化 localDirPath 等变更
        return iterationRepository.save(saved);
    }

    @Transactional
    public Iteration updateIteration(Long id, Iteration iterationDetails, Long userId) {
        log.info("更新迭代信息, ID: {}", id);

        Iteration iteration = iterationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("迭代", id));

        String oldStatus = iteration.getStatus();
        String newStatus = iterationDetails.getStatus();

        // 保存旧内容用于同步历史比较
        String oldNotesContent = iteration.getDevelopmentNotes();
        String oldReleaseContent = iteration.getReleaseNotes();
        String newNotesRaw = iterationDetails.getDevelopmentNotes();
        String newReleaseRaw = iterationDetails.getReleaseNotes();

        if (iterationDetails.getIssueNumber() != null) iteration.setIssueNumber(iterationDetails.getIssueNumber());
        if (iterationDetails.getProjectCode() != null) iteration.setProjectCode(iterationDetails.getProjectCode());
        if (iterationDetails.getTitle() != null) iteration.setTitle(iterationDetails.getTitle());
        if (iterationDetails.getIssueUrl() != null) iteration.setIssueUrl(iterationDetails.getIssueUrl());
        if (newStatus != null) iteration.setStatus(newStatus);
        if (iterationDetails.getPriority() != null) iteration.setPriority(iterationDetails.getPriority());
        // 直接存内容到数据库
        if (newNotesRaw != null) iteration.setDevelopmentNotes(newNotesRaw);
        if (newReleaseRaw != null) iteration.setReleaseNotes(newReleaseRaw);
        // 流程图自动关联：当 localDirPath 不为空时，自动关联到 {issueNumber}.excalidraw
        if (iteration.getLocalDirPath() != null && !iteration.getLocalDirPath().isEmpty()) {
            String excalidrawPath = iteration.getLocalDirPath() + "/" + iteration.getIssueNumber() + ".excalidraw";
            iteration.setFlowchartPath(excalidrawPath);
        } else if (iterationDetails.getFlowchartPath() != null) {
            iteration.setFlowchartPath(iterationDetails.getFlowchartPath());
        }
        if (iterationDetails.getAssigneeId() != null) iteration.setAssigneeId(iterationDetails.getAssigneeId());
        if (iterationDetails.getEstimatedTime() != null) iteration.setEstimatedTime(iterationDetails.getEstimatedTime());
        if (iterationDetails.getActualTime() != null) iteration.setActualTime(iterationDetails.getActualTime());
        if (iterationDetails.getApiDocUrl() != null) iteration.setApiDocUrl(iterationDetails.getApiDocUrl());
        if (iterationDetails.getImpactScope() != null) iteration.setImpactScope(iterationDetails.getImpactScope());
        if (iterationDetails.getTodos() != null) iteration.setTodos(iterationDetails.getTodos());
        iteration.setUpdatedAt(LocalDateTime.now());

        if (newStatus != null && !newStatus.equals(oldStatus)) {
            iteration.setStatusChangedAt(LocalDateTime.now());
            if ("DONE".equals(newStatus)) {
                iteration.setCompletedAt(LocalDateTime.now());
            }
        }

        Iteration saved = iterationRepository.save(iteration);

        // 内容变化时：记录同步历史 + 写入本地文件
        if (newNotesRaw != null && !newNotesRaw.equals(oldNotesContent)) {
            saveSyncHistory(saved.getId(), "PAGE_TO_LOCAL", "developmentNotes", oldNotesContent, newNotesRaw);
            writeLocalFile(saved, saved.getIssueNumber() + ".txt", newNotesRaw);
        }
        if (newReleaseRaw != null && !newReleaseRaw.equals(oldReleaseContent)) {
            saveSyncHistory(saved.getId(), "PAGE_TO_LOCAL", "releaseNotes", oldReleaseContent, newReleaseRaw);
            writeLocalFile(saved, saved.getIssueNumber() + ".md", newReleaseRaw);
        }
        updateHasTodos(saved);

        return saved;
    }

    @Transactional
    public void deleteIteration(Long id) {
        log.info("删除迭代, ID: {}", id);

        Iteration iteration = iterationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("迭代", id));

        iteration.setActive(false);
        iteration.setUpdatedAt(LocalDateTime.now());

        iterationRepository.save(iteration);
    }

    @Transactional
    public void permanentlyDeleteIteration(Long id) {
        log.info("永久删除迭代, ID: {}", id);

        if (!iterationRepository.existsById(id)) {
            throw new EntityNotFoundException("迭代", id);
        }

        iterationRepository.deleteById(id);
    }

    /**
     * 新建需求时创建本地目录和文件
     */
    private void createLocalDirAndFiles(Iteration iteration) {
        IterationImportConfig config = importConfigRepository.findByActiveTrue().orElse(null);
        if (config == null || config.getBaseDirPath() == null) {
            log.debug("未配置导入目录，跳过创建本地目录");
            return;
        }

        try {
            String dirName = String.format("%s-%s-%s",
                    iteration.getIssueNumber(),
                    iteration.getProjectCode(),
                    iteration.getTitle());
            Path dirPath = Paths.get(config.getBaseDirPath(), dirName);

            Files.createDirectories(dirPath);
            iteration.setLocalDirPath(dirPath.toString());

            // 更新 impactScope 文件列表
            updateImpactScope(iteration, dirPath);

            // 自动关联 .excalidraw 文件到流程图
            if (iteration.getFlowchartPath() == null || iteration.getFlowchartPath().isEmpty()) {
                String excalidrawPath = dirPath.resolve(iteration.getIssueNumber() + ".excalidraw").toString();
                if (Files.exists(Paths.get(excalidrawPath))) {
                    iteration.setFlowchartPath(excalidrawPath.replace("\\", "/"));
                }
            }

            iterationRepository.save(iteration);
        } catch (IOException e) {
            log.error("创建本地目录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成发布文档默认模板
     */
    private String generateReleaseTemplate(String issueNumber) {
        return String.format("""
                # [%s](https://github.com/LeaderrunTeam/pm/issues/%s)

                ## 更新说明

                ```text

                ```

                ## 数据库脚本

                ```sql
                alter table xxx...
                ```

                ## 配置文件

                ```yaml

                ```
                """, issueNumber, issueNumber);
    }

    /**
     * 更新 impactScope 文件列表
     */
    private void updateImpactScope(Iteration iteration, Path dirPath) {
        try {
            StringBuilder impactScope = new StringBuilder();
            Files.list(dirPath)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        String filePath = file.toAbsolutePath().toString();
                        if (impactScope.length() > 0) {
                            impactScope.append("\n");
                        }
                        impactScope.append(fileName).append("|").append(filePath);

                        // 自动关联 .excalidraw 文件到流程图字段
                        if (fileName.toLowerCase().endsWith(".excalidraw")) {
                            String normalizedPath = filePath.replace("\\", "/");
                            iteration.setFlowchartPath(normalizedPath);
                        }
                    });
            iteration.setImpactScope(impactScope.toString());
            iterationRepository.save(iteration);
            log.info("更新 impactScope: {}", iteration.getIssueNumber());
        } catch (IOException e) {
            log.error("更新 impactScope 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查并更新 hasTodos 字段
     */
    private void updateHasTodos(Iteration iteration) {
        String content = iteration.getDevelopmentNotes();

        if (content != null && content.contains("待办")) {
            if (!Boolean.TRUE.equals(iteration.getHasTodos())) {
                iteration.setHasTodos(true);
                iterationRepository.save(iteration);
                log.info("标记有待办: {}", iteration.getIssueNumber());
            }
        } else {
            if (Boolean.TRUE.equals(iteration.getHasTodos())) {
                iteration.setHasTodos(false);
                iterationRepository.save(iteration);
            }
        }
    }

    private void writeLocalFile(Iteration iteration, String fileName, String content) {
        if (iteration.getLocalDirPath() == null || iteration.getLocalDirPath().isEmpty()) return;
        try {
            Path dirPath = Paths.get(iteration.getLocalDirPath());
            if (!Files.exists(dirPath)) Files.createDirectories(dirPath);
            Files.writeString(dirPath.resolve(fileName), content);
        } catch (IOException e) {
            log.warn("写入本地文件失败: {}/{}", iteration.getLocalDirPath(), fileName);
        }
    }

    private void saveSyncHistory(Long iterationId, String syncType, String fieldName, String oldValue, String newValue) {
        IterationSyncHistory history = new IterationSyncHistory();
        history.setIterationId(iterationId);
        history.setSyncType(syncType);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setSyncedAt(LocalDateTime.now());
        syncHistoryRepository.save(history);
    }
}
