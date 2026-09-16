package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.JenkinsJobConfig;
import com.example.mynewwork.repository.JenkinsJobConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Jenkins 监控服务
 *
 * 定时轮询 Jenkins 构建状态，构建成功时推送企业微信机器人通知
 *
 * @author jiangyuan
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsMonitorService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final JenkinsJobConfigRepository repository;
    private final ObjectMapper objectMapper;

    /** 仅用于历史密文密码的一次性迁移 */
    private final StringEncryptor stringEncryptor;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    /**
     * 每 30 秒轮询一次所有已启用的 job
     */
    @Scheduled(fixedDelay = 30000)
    public void checkBuilds() {
        List<JenkinsJobConfig> configs = repository.findByEnabledTrue();
        for (JenkinsJobConfig config : configs) {
            try {
                checkOne(config);
            } catch (Exception e) {
                log.error("Jenkins build check failed: job={}, reason={}", config.getJobName(), e.getMessage());
            }
        }
    }

    /**
     * 检查单个 job 的最新构建，成功且为新构建号时发送企业微信通知
     */
    private void checkOne(JenkinsJobConfig config) throws Exception {
        String baseUrl = normalizeBaseUrl(config.getJenkinsUrl());
        String apiUrl = baseUrl + "/job/" + encodePathSegment(config.getJobName()) + "/lastBuild/api/json";
        JsonNode node = readJson(apiUrl, config.getUsername(), config.getPassword());

        // 构建中或非成功状态，直接跳过
        if (node.path("building").asBoolean(false)
                || !"SUCCESS".equals(node.path("result").asText(""))) {
            return;
        }
        long number = node.path("number").asLong();
        Long lastNotified = config.getLastNotifiedBuild();
        // 首次轮询只记录基线，不发送，避免历史构建刷屏
        if (Objects.isNull(lastNotified)) {
            config.setLastNotifiedBuild(number);
            repository.save(config);
            log.info("Jenkins job {} set notify baseline: #{}", config.getJobName(), number);
            return;
        }
        if (number <= lastNotified) {
            return;
        }
        sendWecomSuccess(config, number, node);
        config.setLastNotifiedBuild(number);
        repository.save(config);
        log.info("Jenkins job {} build success notified: #{}", config.getJobName(), number);
    }

    /**
     * 发送企业微信 markdown 构建成功通知
     */
    private void sendWecomSuccess(JenkinsJobConfig config, long number, JsonNode node) throws Exception {
        StringBuilder content = new StringBuilder();
        content.append("**").append(config.getJobName()).append("【Jenkins 构建成功】**\n");
        content.append("> 环境：").append(StringUtils.hasText(config.getEnvironment()) ? config.getEnvironment() : "-").append("\n");
        String branch = resolveBranch(node);
        content.append("> 分支：").append(branch).append("\n");
        content.append("> 时间：").append(formatBuildTime(node.path("timestamp").asLong(0))).append("\n");
        content.append("> 构建号：#").append(number).append("\n");
        // 最近 3 条 git 提交（配置了本地目录时展示，失败自动省略）
        List<String> commits = fetchRecentCommits(config, "-".equals(branch) ? null : branch);
        if (!commits.isEmpty()) {
            content.append("> 最近提交：\n");
            commits.forEach(c -> content.append("> ").append(c).append("\n"));
        }
        long duration = node.path("duration").asLong(0);
        if (duration > 0) {
            content.append("> 用时：").append(formatDuration(duration)).append("\n");
        }
        String buildUrl = node.path("url").asText("");
        if (StringUtils.hasText(buildUrl)) {
            content.append("> [查看构建](").append(buildUrl).append(")\n");
        }

        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("content", content.toString());
        List<String> mobiles = Optional.ofNullable(config.getAtMobiles())
                .map(s -> java.util.Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList())
                .orElse(List.of());
        if (!mobiles.isEmpty()) {
            markdown.put("mentioned_mobile_list", mobiles);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);

        HttpRequest request = HttpRequest.newBuilder(URI.create(config.getWebhookUrl()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200 || !resp.body().contains("\"errcode\":0")) {
            throw new RuntimeException("企业微信返回异常: HTTP " + resp.statusCode() + ", " + resp.body());
        }
    }

    /**
     * 测试 Jenkins 连接，返回该账号可见的 job 列表
     * 密码未填时，优先按 id、其次按 地址+账号 匹配已保存配置并使用其密码；
     * 传入 jobName 时还会校验该 job 是否存在
     */
    public Map<String, Object> testConnection(String jenkinsUrl, String username, String password, Long configId, String jobName) throws Exception {
        JenkinsJobConfig matched = resolveConfigForTest(jenkinsUrl, username, configId);
        if (matched != null) {
            if (!StringUtils.hasText(jenkinsUrl)) {
                jenkinsUrl = matched.getJenkinsUrl();
            }
            if (!StringUtils.hasText(username)) {
                username = matched.getUsername();
            }
            if (!StringUtils.hasText(password)) {
                password = matched.getPassword();
            }
            if (!StringUtils.hasText(jobName)) {
                jobName = matched.getJobName();
            }
        }
        if (!StringUtils.hasText(jenkinsUrl)) {
            throw new IllegalArgumentException("缺少 Jenkins 地址");
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("缺少 Jenkins 账号");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("缺少 Jenkins 密码，且未匹配到已保存的配置，请先填写密码");
        }
        String baseUrl = normalizeBaseUrl(jenkinsUrl);
        // 1. 验证账号认证
        JsonNode node = readJson(baseUrl + "/api/json", username, password);
        // 2. 若传了 job 名称，校验该 job 是否存在，避免轮询时才发现 404
        if (StringUtils.hasText(jobName)) {
            readJson(baseUrl + "/job/" + encodePathSegment(jobName) + "/api/json", username, password);
        }
        List<String> jobs = new ArrayList<>();
        node.path("jobs").forEach(j -> jobs.add(j.path("name").asText()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("jobs", jobs);
        return result;
    }

    /**
     * 匹配测试用的已保存配置：优先 id，其次按规范化地址+账号匹配
     */
    private JenkinsJobConfig resolveConfigForTest(String jenkinsUrl, String username, Long configId) {
        if (configId != null) {
            JenkinsJobConfig config = repository.findById(configId).orElse(null);
            if (config != null && StringUtils.hasText(config.getPassword())) {
                return config;
            }
        }
        return repository.findAll().stream()
                .filter(c -> StringUtils.hasText(c.getPassword()))
                .filter(c -> !StringUtils.hasText(jenkinsUrl)
                        || Objects.equals(normalizeBaseUrl(c.getJenkinsUrl()), normalizeBaseUrl(jenkinsUrl)))
                .filter(c -> !StringUtils.hasText(username) || Objects.equals(c.getUsername(), username))
                .findFirst()
                .orElse(null);
    }

    /**
     * GET 请求并解析 JSON（Basic Auth 认证）
     */
    private JsonNode readJson(String url, String username, String password) throws Exception {
        String basic = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Authorization", "Basic " + basic)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new RuntimeException("Jenkins 认证失败（HTTP " + resp.statusCode()
                    + "），请检查账号密码是否正确（密码为空也会导致此错误）");
        }
        if (resp.statusCode() == 404) {
            throw new RuntimeException("Jenkins 路径不存在（HTTP 404），请检查 Jenkins 地址或 job 名称是否正确");
        }
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Jenkins 返回 HTTP " + resp.statusCode());
        }
        return objectMapper.readTree(resp.body());
    }

    /**
     * 规范化 Jenkins 地址：去掉尾部斜杠，去掉 /login 等子路径
     */
    private String normalizeBaseUrl(String jenkinsUrl) {
        String url = Optional.ofNullable(jenkinsUrl).map(String::trim).orElse("");
        int loginIdx = url.indexOf("/login");
        if (loginIdx > 0) {
            url = url.substring(0, loginIdx);
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * URL 编码路径段（job 名可能包含特殊字符）
     */
    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * 毫秒格式化为 分:秒 或 时:分:秒
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + " 秒";
        }
        if (seconds < 3600) {
            return seconds / 60 + " 分 " + seconds % 60 + " 秒";
        }
        return seconds / 3600 + " 时 " + seconds % 3600 / 60 + " 分";
    }

    /**
     * 从构建参数中解析分支（优先 BRANCH_NAME），取不到返回 -
     */
    private String resolveBranch(JsonNode node) {
        for (JsonNode action : node.path("actions")) {
            JsonNode params = action.get("parameters");
            if (params == null || !params.isArray()) {
                continue;
            }
            for (JsonNode param : params) {
                String name = param.path("name").asText("");
                if ("BRANCH_NAME".equals(name) || name.toUpperCase(Locale.ROOT).contains("BRANCH")) {
                    String value = param.path("value").asText("");
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        }
        return "-";
    }

    /**
     * 毫秒时间戳格式化为本地时间字符串
     */
    private String formatBuildTime(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 通过本地 git 仓库取最近 3 条提交（已格式化），未配置目录或执行失败返回空列表
     * 先 fetch 远程分支保证拿到最新提交，优先按构建分支查询，失败回退当前分支
     */
    private List<String> fetchRecentCommits(JenkinsJobConfig config, String branch) {
        if (!StringUtils.hasText(config.getLocalDir())) {
            return List.of();
        }
        // 预先拉取远程分支最新代码，避免拿到陈旧的提交日志
        fetchRemote(config.getLocalDir(), branch);
        List<String> commits = runGitLog(config.getLocalDir(), branch);
        if (commits.isEmpty() && StringUtils.hasText(branch)) {
            commits = runGitLog(config.getLocalDir(), null);
        }
        return commits;
    }

    /**
     * git fetch 拉取远程分支最新提交，失败仅记录日志不阻断（后续 log 仍会尝试）
     */
    private void fetchRemote(String dir, String branch) {
        List<String> args = StringUtils.hasText(branch)
                ? List.of("fetch", "origin", branch)
                : List.of("fetch", "origin");
        GitExecResult result = execGit(dir, args);
        if (result.exitCode() != 0) {
            log.warn("git fetch 失败: dir={}, branch={}", dir, branch);
        }
    }

    /**
     * 在指定目录执行 git log 取 3 条提交
     * 有分支时基于已 fetch 的远程引用 origin/{branch} 查询，确保是最新提交
     */
    private List<String> runGitLog(String dir, String branch) {
        List<String> args = new ArrayList<>();
        args.add("log");
        if (StringUtils.hasText(branch)) {
            args.add("origin/" + branch);
        }
        args.add("-3");
        args.add("--pretty=format:%h%x01%s%x01%an");
        GitExecResult result = execGit(dir, args);
        if (result.exitCode() != 0) {
            log.debug("git log 执行失败: dir={}, ref={}", dir, branch);
            return List.of();
        }
        return result.output().lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::formatCommitLine)
                .limit(3)
                .toList();
    }

    /**
     * git 命令执行结果
     */
    private record GitExecResult(int exitCode, String output) {
    }

    /**
     * 执行 git 命令（UTF-8 输出规避 Windows 编码问题，30 秒超时兜底）
     */
    private GitExecResult execGit(String dir, List<String> args) {
        try {
            List<String> cmd = new ArrayList<>(List.of("git", "-c", "i18n.logOutputEncoding=UTF-8"));
            cmd.addAll(args);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(dir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new GitExecResult(-1, output);
            }
            return new GitExecResult(process.exitValue(), output);
        } catch (Exception e) {
            log.warn("git 命令执行失败: dir={}, 原因={}", dir, e.getMessage());
            return new GitExecResult(-1, "");
        }
    }

    /**
     * git log 行（短hash|首行说明|作者）格式化为消息展示文本，提交说明截断防刷屏
     */
    private String formatCommitLine(String line) {
        String[] parts = line.split("\u0001", -1);
        String hash = parts.length > 0 ? parts[0] : "";
        String msg = parts.length > 1 ? parts[1] : "";
        String author = parts.length > 2 ? parts[2] : "";
        if (msg.length() > 80) {
            msg = msg.substring(0, 80) + "…";
        }
        return "「" + hash + "」 " + msg + (StringUtils.hasText(author) ? " " + author : "");
    }

    // ========== 配置管理（密码明文存储） ==========

    /**
     * 查询全部配置
     */
    @Transactional(readOnly = true)
    public List<JenkinsJobConfig> listConfigs() {
        return repository.findAll();
    }

    /**
     * 新增配置
     */
    @Transactional
    public JenkinsJobConfig createConfig(JenkinsJobConfig config) {
        if (repository.existsByJobName(config.getJobName())) {
            throw new IllegalArgumentException("job " + config.getJobName() + " 已存在监控配置");
        }
        if (!StringUtils.hasText(config.getPassword())) {
            throw new IllegalArgumentException("Jenkins 密码不能为空");
        }
        config.setId(null);
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        return repository.save(config);
    }

    /**
     * 更新配置，密码为空表示不修改原密码
     */
    @Transactional
    public JenkinsJobConfig updateConfig(Long id, JenkinsJobConfig update) {
        JenkinsJobConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("监控配置不存在: " + id));
        Optional.ofNullable(update.getJobName()).filter(StringUtils::hasText).ifPresent(config::setJobName);
        Optional.ofNullable(update.getJenkinsUrl()).ifPresent(config::setJenkinsUrl);
        Optional.ofNullable(update.getUsername()).ifPresent(config::setUsername);
        Optional.ofNullable(update.getEnvironment()).ifPresent(config::setEnvironment);
        Optional.ofNullable(update.getLocalDir()).ifPresent(config::setLocalDir);
        Optional.ofNullable(update.getWebhookUrl()).ifPresent(config::setWebhookUrl);
        Optional.ofNullable(update.getAtMobiles()).ifPresent(config::setAtMobiles);
        Optional.ofNullable(update.getEnabled()).ifPresent(config::setEnabled);
        Optional.ofNullable(update.getRemark()).ifPresent(config::setRemark);
        if (StringUtils.hasText(update.getPassword())) {
            config.setPassword(update.getPassword());
        }
        return repository.save(config);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        repository.deleteById(id);
    }

    /**
     * 查询单个配置
     */
    @Transactional(readOnly = true)
    public JenkinsJobConfig getConfig(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("监控配置不存在: " + id));
    }

    /**
     * 一次性迁移：将历史 jasypt 密文密码解密为明文后落库，保证编辑弹窗可回显密码
     */
    @PostConstruct
    public void migratePasswordToPlain() {
        try {
            List<JenkinsJobConfig> changed = new ArrayList<>();
            for (JenkinsJobConfig config : repository.findAll()) {
                if (!StringUtils.hasText(config.getPassword())) {
                    continue;
                }
                String plain = tryDecryptLegacy(config.getPassword());
                if (plain != null) {
                    config.setPassword(plain);
                    changed.add(config);
                }
            }
            if (!changed.isEmpty()) {
                repository.saveAll(changed);
                log.info("Jenkins monitor passwords migrated to plaintext: {} items", changed.size());
            }
        } catch (Exception e) {
            log.warn("Jenkins password migration failed: {}", e.getMessage());
        }
    }

    /**
     * 尝试解密历史 jasypt 密文，解密失败（已是明文）返回 null
     */
    private String tryDecryptLegacy(String value) {
        try {
            return stringEncryptor.decrypt(value);
        } catch (Exception e) {
            return null;
        }
    }
}