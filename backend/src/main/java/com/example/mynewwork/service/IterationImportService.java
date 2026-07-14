package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.Iteration;
import com.example.mynewwork.model.entity.IterationImportConfig;
import com.example.mynewwork.model.entity.IterationSyncHistory;
import com.example.mynewwork.repository.IterationImportConfigRepository;
import com.example.mynewwork.repository.IterationRepository;
import com.example.mynewwork.repository.IterationSyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IterationImportService {

    private final IterationImportConfigRepository configRepository;
    private final IterationRepository iterationRepository;
    private final IterationSyncHistoryRepository syncHistoryRepository;

    public Optional<IterationImportConfig> getConfig() {
        return configRepository.findByActiveTrue();
    }

    @Transactional
    public IterationImportConfig saveConfig(String dirPath) {
        IterationImportConfig config = configRepository.findByActiveTrue()
                .orElse(new IterationImportConfig());
        config.setBaseDirPath(dirPath);
        config.setActive(true);
        return configRepository.save(config);
    }

    @Transactional
    public List<Iteration> importFromDirectory(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("目录不存在: " + dirPath);
        }

        saveConfig(dirPath);

        List<Iteration> imported = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !shouldSkipDirectory(entry)) {
                    Iteration iteration = parseFolderToIteration(entry);
                    if (iteration != null) {
                        // 只用 issueNumber 去重，同一个 issue 的多个项目合并到一条记录
                        Optional<Iteration> existing = iterationRepository
                                .findFirstByIssueNumberAndActiveTrue(iteration.getIssueNumber());
                        if (existing.isEmpty()) {
                            iteration.setLocalDirPath(entry.toString());
                            imported.add(iterationRepository.save(iteration));
                            log.info("导入需求: {} - {}", iteration.getIssueNumber(), iteration.getTitle());
                        } else {
                            // 更新已存在的需求，合并项目代码
                            Iteration exist = existing.get();
                            exist.setLocalDirPath(entry.toString());

                            // 合并项目代码（去重）
                            String existCodes = exist.getProjectCode() != null ? exist.getProjectCode() : "";
                            String newCode = iteration.getProjectCode() != null ? iteration.getProjectCode() : "";
                            if (!existCodes.contains(newCode) && !newCode.isEmpty()) {
                                String mergedCodes = existCodes.isEmpty() ? newCode : existCodes + "," + newCode;
                                exist.setProjectCode(mergedCodes);
                            }

                            if (iteration.getDevelopmentNotes() != null && !iteration.getDevelopmentNotes().isEmpty()) {
                                exist.setDevelopmentNotes(iteration.getDevelopmentNotes());
                            }
                            if (iteration.getReleaseNotes() != null && !iteration.getReleaseNotes().isEmpty()) {
                                exist.setReleaseNotes(iteration.getReleaseNotes());
                            }
                            iterationRepository.save(exist);
                            log.info("更新已存在的需求: {} - {}", exist.getIssueNumber(), exist.getTitle());
                        }
                    }
                }
            }
        }

        return imported;
    }

    private boolean shouldSkipDirectory(Path entry) {
        String name = entry.getFileName().toString();
        return name.startsWith(".");
    }

    private Iteration parseFolderToIteration(Path folder) {
        String folderName = folder.getFileName().toString();
        Iteration iteration = new Iteration();

        // 格式1: #665-om订单中心商品字段5位小数
        // 格式2: 665-om订单中心商品字段5位小数
        // 格式3: #665-om-订单中心商品字段5位小数
        // 格式4: 665-om-订单中心商品字段5位小数
        String cleanedName = folderName;
        if (cleanedName.startsWith("#")) {
            cleanedName = cleanedName.substring(1);
        }

        // 尝试匹配 数字-项目简称-标题
        String[] parts = cleanedName.split("-", 3);
        if (parts.length >= 2) {
            String issueNum = parts[0].trim();
            if (issueNum.matches("\\d+")) {
                iteration.setIssueNumber(issueNum);
                if (parts.length == 3) {
                    iteration.setProjectCode(parts[1].trim());
                    iteration.setTitle(parts[2].trim());
                } else {
                    // 尝试从第二部分分离项目名和标题
                    String secondPart = parts[1].trim();
                    String[] subParts = separateProjectAndTitle(secondPart);
                    iteration.setProjectCode(subParts[0]);
                    iteration.setTitle(subParts[1]);
                }
            } else {
                iteration.setIssueNumber(String.valueOf(System.currentTimeMillis() % 100000));
                iteration.setProjectCode("imported");
                iteration.setTitle(folderName);
            }
        } else {
            iteration.setIssueNumber(String.valueOf(System.currentTimeMillis() % 100000));
            iteration.setProjectCode("imported");
            iteration.setTitle(folderName);
        }

        iteration.setStatus("TODO");
        iteration.setPriority("MEDIUM");
        iteration.setActive(true);

        // 设置默认关联文件路径：issue编号.txt 和 issue编号.md
        String notesFilePath = folder.resolve(iteration.getIssueNumber() + ".txt").toAbsolutePath().toString();
        String releaseFilePath = folder.resolve(iteration.getIssueNumber() + ".md").toAbsolutePath().toString();
        iteration.setDevelopmentNotes(notesFilePath);
        iteration.setReleaseNotes(releaseFilePath);

        readContentFiles(folder, iteration);

        return iteration;
    }

    private String[] separateProjectAndTitle(String text) {
        // 常见项目简称模式: om, wm, pda 等
        String[] knownProjects = {"om", "wm", "pda", "crm", "erp", "oms", "wms", "tms", "b2b", "b2c"};

        for (String project : knownProjects) {
            if (text.toLowerCase().startsWith(project)) {
                String title = text.substring(project.length()).trim();
                if (title.startsWith("-") || title.startsWith("_")) {
                    title = title.substring(1).trim();
                }
                return new String[]{project, title};
            }
        }

        // 如果没有匹配到已知项目，尝试找第一个中文字符作为分隔点
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fa5') {
                if (i > 0) {
                    return new String[]{text.substring(0, i).trim(), text.substring(i).trim()};
                }
                break;
            }
        }

        return new String[]{"unknown", text};
    }

    private void readContentFiles(Path folder, Iteration iteration) {
        StringBuilder notesContent = new StringBuilder();
        StringBuilder releaseContent = new StringBuilder();
        List<String> allFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String fileName = file.getFileName().toString().toLowerCase();
                    String filePath = file.toAbsolutePath().toString();

                    // 记录所有文件
                    allFiles.add(file.getFileName().toString() + "|" + filePath);

                    // 只读取文本文件内容，跳过二进制文件（如图片等）
                    if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
                        try {
                            String content = Files.readString(file);

                            if (fileName.endsWith(".txt")) {
                                if (notesContent.length() > 0) {
                                    notesContent.append("\n\n");
                                }
                                notesContent.append(content);
                            } else if (fileName.endsWith(".md")) {
                                if (releaseContent.length() > 0) {
                                    releaseContent.append("\n\n");
                                }
                                releaseContent.append(content);
                            }
                        } catch (IOException e) {
                            log.warn("读取文件失败: {}", file, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("读取目录内容失败: {}", folder, e);
        }

        if (notesContent.length() > 0) {
            iteration.setDevelopmentNotes(notesContent.toString());
        }
        if (releaseContent.length() > 0) {
            iteration.setReleaseNotes(releaseContent.toString());
        }

        // 将所有文件信息存储在 impactScope 字段
        if (!allFiles.isEmpty()) {
            iteration.setImpactScope(String.join("\n", allFiles));
        }
    }

    @Transactional
    public void syncIterationToLocal(Iteration iteration) throws IOException {
        IterationImportConfig config = configRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("未配置导入目录"));

        if (iteration.getLocalDirPath() == null || iteration.getLocalDirPath().isEmpty()) {
            String dirName = String.format("#%s-%s-%s",
                    iteration.getIssueNumber(),
                    iteration.getProjectCode(),
                    iteration.getTitle());
            Path newDir = Paths.get(config.getBaseDirPath(), dirName);
            Files.createDirectories(newDir);
            iteration.setLocalDirPath(newDir.toString());
        }

        Path dir = Paths.get(iteration.getLocalDirPath());
        Files.createDirectories(dir);

        writeContentFile(dir, "notes.txt", iteration.getDevelopmentNotes());
        writeContentFile(dir, "release.md", iteration.getReleaseNotes());

        iterationRepository.save(iteration);
    }

    private void writeContentFile(Path dir, String fileName, String content) throws IOException {
        Path file = dir.resolve(fileName);
        if (content != null && !content.isEmpty()) {
            Files.writeString(file, content);
        } else if (Files.exists(file)) {
            Files.deleteIfExists(file);
        }
    }

    @Transactional
    public List<Iteration> syncAllToLocal() throws IOException {
        List<Iteration> all = iterationRepository.findByActiveTrue();
        for (Iteration iteration : all) {
            syncIterationToLocal(iteration);
        }
        return all;
    }

    @Transactional
    public Map<String, Object> autoSync() throws IOException {
        IterationImportConfig config = configRepository.findByActiveTrue().orElse(null);
        if (config == null || config.getBaseDirPath() == null) {
            return Map.of("synced", 0, "message", "未配置导入目录");
        }

        Path dir = Paths.get(config.getBaseDirPath());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Map.of("synced", 0, "message", "导入目录不存在");
        }

        int syncedCount = 0;
        List<Iteration> allIterations = iterationRepository.findByActiveTrue();

        // 1. 从本地目录同步到页面
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !shouldSkipDirectory(entry)) {
                    Iteration localIteration = parseFolderToIteration(entry);
                    if (localIteration != null) {
                        Optional<Iteration> existing = iterationRepository
                                .findByIssueNumberAndProjectCodeAndActiveTrue(
                                        localIteration.getIssueNumber(),
                                        localIteration.getProjectCode());

                        if (existing.isPresent()) {
                            Iteration exist = existing.get();
                            // 检查本地文件是否有更新
                            if (hasLocalChanges(exist, entry)) {
                                syncLocalToPage(exist, entry);
                                syncedCount++;
                            }
                        }
                    }
                }
            }
        }

        // 2. 从页面同步到本地目录
        for (Iteration iteration : allIterations) {
            if (iteration.getLocalDirPath() != null && !iteration.getLocalDirPath().isEmpty()) {
                Path localDir = Paths.get(iteration.getLocalDirPath());
                if (Files.exists(localDir) && Files.isDirectory(localDir)) {
                    if (hasPageChanges(iteration, localDir)) {
                        syncPageToLocal(iteration, localDir);
                        syncedCount++;
                    }
                }
            }
        }

        return Map.of("synced", syncedCount, "timestamp", LocalDateTime.now().toString());
    }

    private boolean hasLocalChanges(Iteration iteration, Path localDir) {
        try {
            Path notesFile = localDir.resolve("notes.txt");
            Path releaseFile = localDir.resolve("release.md");

            String localNotes = Files.exists(notesFile) ? Files.readString(notesFile) : "";
            String localRelease = Files.exists(releaseFile) ? Files.readString(releaseFile) : "";

            String pageNotes = iteration.getDevelopmentNotes() != null ? iteration.getDevelopmentNotes() : "";
            String pageRelease = iteration.getReleaseNotes() != null ? iteration.getReleaseNotes() : "";

            return !localNotes.equals(pageNotes) || !localRelease.equals(pageRelease);
        } catch (IOException e) {
            return false;
        }
    }

    private void syncLocalToPage(Iteration iteration, Path localDir) {
        try {
            Path notesFile = localDir.resolve("notes.txt");
            Path releaseFile = localDir.resolve("release.md");

            String oldNotes = iteration.getDevelopmentNotes();
            String oldRelease = iteration.getReleaseNotes();

            String newNotes = Files.exists(notesFile) ? Files.readString(notesFile) : "";
            String newRelease = Files.exists(releaseFile) ? Files.readString(releaseFile) : "";

            iteration.setDevelopmentNotes(newNotes.isEmpty() ? null : newNotes);
            iteration.setReleaseNotes(newRelease.isEmpty() ? null : newRelease);

            // 记录同步历史
            if (!Objects.equals(oldNotes, newNotes)) {
                saveSyncHistory(iteration.getId(), "LOCAL_TO_PAGE", "developmentNotes", oldNotes, newNotes);
            }
            if (!Objects.equals(oldRelease, newRelease)) {
                saveSyncHistory(iteration.getId(), "LOCAL_TO_PAGE", "releaseNotes", oldRelease, newRelease);
            }

            iterationRepository.save(iteration);
            log.info("本地同步到页面: {} - {}", iteration.getIssueNumber(), iteration.getTitle());
        } catch (IOException e) {
            log.error("同步失败: {}", iteration.getTitle(), e);
        }
    }

    private boolean hasPageChanges(Iteration iteration, Path localDir) {
        try {
            Path notesFile = localDir.resolve("notes.txt");
            Path releaseFile = localDir.resolve("release.md");

            String localNotes = Files.exists(notesFile) ? Files.readString(notesFile) : "";
            String localRelease = Files.exists(releaseFile) ? Files.readString(releaseFile) : "";

            String pageNotes = iteration.getDevelopmentNotes() != null ? iteration.getDevelopmentNotes() : "";
            String pageRelease = iteration.getReleaseNotes() != null ? iteration.getReleaseNotes() : "";

            return !localNotes.equals(pageNotes) || !localRelease.equals(pageRelease);
        } catch (IOException e) {
            return false;
        }
    }

    private void syncPageToLocal(Iteration iteration, Path localDir) {
        try {
            String oldNotes = Files.exists(localDir.resolve("notes.txt")) ? Files.readString(localDir.resolve("notes.txt")) : "";
            String oldRelease = Files.exists(localDir.resolve("release.md")) ? Files.readString(localDir.resolve("release.md")) : "";

            String newNotes = iteration.getDevelopmentNotes() != null ? iteration.getDevelopmentNotes() : "";
            String newRelease = iteration.getReleaseNotes() != null ? iteration.getReleaseNotes() : "";

            writeContentFile(localDir, "notes.txt", newNotes);
            writeContentFile(localDir, "release.md", newRelease);

            // 记录同步历史
            if (!oldNotes.equals(newNotes)) {
                saveSyncHistory(iteration.getId(), "PAGE_TO_LOCAL", "developmentNotes", oldNotes, newNotes);
            }
            if (!oldRelease.equals(newRelease)) {
                saveSyncHistory(iteration.getId(), "PAGE_TO_LOCAL", "releaseNotes", oldRelease, newRelease);
            }

            log.info("页面同步到本地: {} - {}", iteration.getIssueNumber(), iteration.getTitle());
        } catch (IOException e) {
            log.error("同步失败: {}", iteration.getTitle(), e);
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

    public List<IterationSyncHistory> getSyncHistory(Long iterationId) {
        return syncHistoryRepository.findTop100ByIterationIdOrderBySyncedAtDesc(iterationId);
    }

    /**
     * 刷新所有需求的文件列表（impactScope）
     */
    @Transactional
    public Map<String, Object> refreshAllFileLists() {
        IterationImportConfig config = configRepository.findByActiveTrue().orElse(null);
        if (config == null || config.getBaseDirPath() == null) {
            return Map.of("refreshed", 0, "message", "未配置导入目录");
        }

        Path dir = Paths.get(config.getBaseDirPath());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Map.of("refreshed", 0, "message", "导入目录不存在");
        }

        int refreshedCount = 0;
        List<Iteration> allIterations = iterationRepository.findByActiveTrue();

        for (Iteration iteration : allIterations) {
            if (iteration.getLocalDirPath() != null && !iteration.getLocalDirPath().isEmpty()) {
                Path localDir = Paths.get(iteration.getLocalDirPath());
                // 直接调用已添加异常处理的updateImpactScope方法
                updateImpactScope(iteration, localDir);
                refreshedCount++;
            }
        }

        return Map.of(
                "refreshed", refreshedCount,
                "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * 更新 impactScope 文件列表
     */
    private void updateImpactScope(Iteration iteration, Path dirPath) {
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            log.warn("目录不存在，跳过更新: {}", dirPath);
            return;
        }

        try {
            StringBuilder impactScope = new StringBuilder();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                List<Path> files = new ArrayList<>();
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        files.add(file);
                    }
                }
                files.sort(Comparator.comparing(p -> p.getFileName().toString()));

                for (Path file : files) {
                    String fileName = file.getFileName().toString();
                    String filePath = file.toAbsolutePath().toString();
                    if (impactScope.length() > 0) {
                        impactScope.append("\n");
                    }
                    impactScope.append(fileName).append("|").append(filePath);
                }
            }
            iteration.setImpactScope(impactScope.toString());
            iterationRepository.save(iteration);
            log.info("更新文件列表: {} - {} 个文件", iteration.getIssueNumber(),
                    impactScope.length() > 0 ? impactScope.toString().split("\n").length : 0);
        } catch (IOException e) {
            log.error("更新文件列表失败: {}", iteration.getIssueNumber(), e);
        }
    }

    @Transactional
    public Map<String, Object> checkAndImportNewFolders() throws IOException {
        IterationImportConfig config = configRepository.findByActiveTrue().orElse(null);
        if (config == null || config.getBaseDirPath() == null) {
            return Map.of("imported", 0, "message", "未配置导入目录");
        }

        Path dir = Paths.get(config.getBaseDirPath());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Map.of("imported", 0, "message", "导入目录不存在");
        }

        int importedCount = 0;
        List<String> importedNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !shouldSkipDirectory(entry)) {
                    Iteration localIteration = parseFolderToIteration(entry);
                    if (localIteration != null) {
                        // 只用 issueNumber 去重
                        Optional<Iteration> existing = iterationRepository
                                .findFirstByIssueNumberAndActiveTrue(localIteration.getIssueNumber());

                        if (existing.isEmpty()) {
                            localIteration.setLocalDirPath(entry.toString());
                            iterationRepository.save(localIteration);
                            importedCount++;
                            importedNames.add(entry.getFileName().toString());
                            log.info("导入新需求: {} - {}", localIteration.getIssueNumber(), localIteration.getTitle());
                        }
                    }
                }
            }
        }

        return Map.of(
                "imported", importedCount,
                "names", importedNames,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
