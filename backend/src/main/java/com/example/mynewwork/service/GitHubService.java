package com.example.mynewwork.service;

import com.example.mynewwork.model.dto.GitHubConfigDTO;
import com.example.mynewwork.model.dto.GitHubIssueDTO;
import com.example.mynewwork.model.dto.GitHubIssueDetailDTO;
import com.example.mynewwork.model.dto.GitHubPromptResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GitHub 指派任务服务
 * 拉取指派给当前账号的 Issue 列表/详情，下载正文图片并生成 AI 提示词
 *
 * @author jiangyuan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_IMAGES = 10;
    private static final int MAX_BODY_CHARS = 20000;
    private static final int MAX_COMMENTS_CHARS = 20000;
    private static final int MAX_PAGES = 10;
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\(([^)\\s]+)\\)");

    private static final String KEY_BASE_URL = "github.base-url";
    private static final String KEY_OWNER = "github.owner";
    private static final String KEY_REPO = "github.repo";
    private static final String KEY_TOKEN = "github.token";
    private static final String KEY_PROXY_HOST = "github.proxy-host";
    private static final String KEY_PROXY_PORT = "github.proxy-port";
    private static final String KEY_BUG_LABELS = "github.bug-labels";
    private static final String CUSTOM_PROMPT_PREFIX = "github.prompt-custom.";

    private final SystemConfigService configService;
    private final StringEncryptor stringEncryptor;
    private final ObjectMapper objectMapper;

    @Value("${app.file-storage.upload-dir:./uploads}")
    private String uploadDir;

    /** HttpClient 按代理配置缓存，代理变更时重建 */
    private volatile HttpClient httpClient;
    private volatile String clientProxyKey = "";

    // ========== 配置管理 ==========

    public GitHubConfigDTO getConfig() {
        GitHubConfigDTO dto = new GitHubConfigDTO();
        dto.setBaseUrl(cfg(KEY_BASE_URL, "https://api.github.com"));
        dto.setOwner(cfg(KEY_OWNER, "LeaderrunTeam"));
        dto.setRepo(cfg(KEY_REPO, "pm"));
        dto.setTokenConfigured(StringUtils.hasText(cfg(KEY_TOKEN, "")));
        dto.setProxyHost(cfg(KEY_PROXY_HOST, ""));
        dto.setProxyPort(cfg(KEY_PROXY_PORT, ""));
        dto.setBugLabels(cfg(KEY_BUG_LABELS, "bug"));
        return dto;
    }

    public GitHubConfigDTO saveConfig(GitHubConfigDTO dto) {
        // token 留空表示不修改
        Optional.ofNullable(dto.getToken()).filter(StringUtils::hasText)
                .ifPresent(token -> setCfg(KEY_TOKEN, stringEncryptor.encrypt(token.trim()), "GitHub Classic PAT（加密存储）"));
        saveIfPresent(dto.getBaseUrl(), KEY_BASE_URL, "GitHub API 基地址");
        saveIfPresent(dto.getOwner(), KEY_OWNER, "GitHub 组织");
        saveIfPresent(dto.getRepo(), KEY_REPO, "GitHub 仓库");
        saveIfPresent(dto.getProxyHost(), KEY_PROXY_HOST, "GitHub 访问代理主机");
        saveIfPresent(dto.getProxyPort(), KEY_PROXY_PORT, "GitHub 访问代理端口");
        saveIfPresent(dto.getBugLabels(), KEY_BUG_LABELS, "bug 标签名（逗号分隔）");
        return getConfig();
    }

    // ========== GitHub 拉取 ==========

    /**
     * 测试连接：调 /user 验证 Token 并返回当前登录名
     */
    public Map<String, String> testConnection() {
        String login = getJson("/user").path("login").asText("");
        return Map.of("login", login);
    }

    /**
     * 指派给当前账号的 Issue 列表（过滤 PR），open 排前、closed 排后，组内按更新时间倒序
     */
    public List<GitHubIssueDTO> listIssues() {
        String login = currentLogin();
        List<GitHubIssueDTO> issues = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            String path = String.format("/repos/%s/issues?assignee=%s&state=all&per_page=100&page=%d&sort=updated&direction=desc",
                    repoPath(), login, page);
            JsonNode array = getJson(path);
            if (array.isEmpty()) {
                break;
            }
            for (JsonNode node : array) {
                // issues 接口混含 PR，pull_request 字段存在即为 PR，需过滤
                if (node.has("pull_request")) {
                    continue;
                }
                issues.add(parseIssue(node));
            }
            if (array.size() < 100) {
                break;
            }
        }
        issues.sort(Comparator
                .comparingInt((GitHubIssueDTO i) -> "open".equals(i.getState()) ? 0 : 1)
                .thenComparing(GitHubIssueDTO::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return issues;
    }

    /**
     * Issue 详情：正文 + 评论 + 图片下载
     */
    public GitHubIssueDetailDTO getDetail(int number) {
        JsonNode node = getJson(String.format("/repos/%s/issues/%d", repoPath(), number));
        if (node.has("pull_request")) {
            throw new IllegalArgumentException("#" + number + " 是 Pull Request，不是 Issue");
        }

        GitHubIssueDetailDTO dto = new GitHubIssueDetailDTO();
        dto.setNumber(node.path("number").asInt());
        dto.setTitle(node.path("title").asText(""));
        dto.setState(node.path("state").asText(""));
        List<String> labels = new ArrayList<>();
        node.path("labels").forEach(label -> labels.add(label.path("name").asText("")));
        dto.setLabels(labels);
        dto.setBug(isBugIssue(labels));
        dto.setHtmlUrl(node.path("html_url").asText(""));
        dto.setAssignee(node.path("assignee").path("login").asText(""));
        dto.setCreatedAt(node.path("created_at").asText(""));
        dto.setUpdatedAt(node.path("updated_at").asText(""));

        List<String> warnings = new ArrayList<>();
        String originalBody = Optional.ofNullable(node.path("body").asText("")).orElse("");
        if (originalBody.length() > MAX_BODY_CHARS) {
            warnings.add("正文超过 " + MAX_BODY_CHARS + " 字符已截断");
        }
        dto.setBody(truncate(originalBody, MAX_BODY_CHARS));

        // 图片警告单独存放：提示词已不含图片，只需在详情里提示
        List<String> imageWarnings = new ArrayList<>();
        dto.setComments(loadComments(number, warnings));
        // 图片解析用未截断的原文，避免正文后部的图片丢失
        dto.setImages(downloadImages(originalBody, number, imageWarnings));
        dto.setWarnings(warnings);
        dto.setImageWarnings(imageWarnings);
        return dto;
    }

    /**
     * 生成提示词，仅带正文/评论类警告
     */
    public GitHubPromptResultDTO buildPrompt(int number) {
        GitHubIssueDetailDTO detail = getDetail(number);
        GitHubPromptResultDTO result = new GitHubPromptResultDTO();
        result.setPrompt(GitHubPromptBuilder.build(detail));
        result.setNumber(detail.getNumber());
        result.setTitle(detail.getTitle());
        result.setHtmlUrl(detail.getHtmlUrl());
        result.setWarnings(detail.getWarnings());
        return result;
    }

    /**
     * 读取 Issue 的自定义提示词；未编辑过返回 null
     */
    public String getCustomPrompt(int number) {
        return configService.getConfigValue(CUSTOM_PROMPT_PREFIX + number, null);
    }

    /**
     * 保存 Issue 的自定义提示词（upsert）
     */
    public void saveCustomPrompt(int number, String content) {
        configService.setConfig(CUSTOM_PROMPT_PREFIX + number, content, "GitHub Issue #" + number + " 自定义提示词");
    }

    // ========== 内部实现 ==========

    /**
     * 加载评论，总量超限截断
     */
    private List<GitHubIssueDetailDTO.CommentDTO> loadComments(int number, List<String> warnings) {
        List<GitHubIssueDetailDTO.CommentDTO> comments = new ArrayList<>();
        JsonNode array = getJson(String.format("/repos/%s/issues/%d/comments?per_page=100", repoPath(), number));
        int total = 0;
        for (JsonNode node : array) {
            int remaining = MAX_COMMENTS_CHARS - total;
            if (remaining <= 0) {
                warnings.add("评论过多，部分较早评论未展示");
                break;
            }
            String body = node.path("body").asText("");
            if (body.length() > remaining) {
                body = truncate(body, remaining);
                warnings.add("评论区过长已截断");
            }
            total += body.length();
            GitHubIssueDetailDTO.CommentDTO comment = new GitHubIssueDetailDTO.CommentDTO();
            comment.setAuthor(node.path("user").path("login").asText(""));
            comment.setCreatedAt(node.path("created_at").asText(""));
            comment.setBody(body);
            comments.add(comment);
        }
        return comments;
    }

    /**
     * 解析正文中的图片并下载到本地；仅 GitHub 域名图片下载，其余保留原链接
     * 下载类警告写入 imageWarnings，与提示词警告分开
     */
    private List<GitHubIssueDetailDTO.ImageDTO> downloadImages(String markdown, int issueNumber, List<String> imageWarnings) {
        Matcher matcher = IMAGE_PATTERN.matcher(markdown);
        List<String> urls = new ArrayList<>();
        while (matcher.find()) {
            urls.add(matcher.group(1).trim());
        }
        if (urls.size() > MAX_IMAGES) {
            imageWarnings.add("图片共 " + urls.size() + " 张，仅处理前 " + MAX_IMAGES + " 张");
            urls = urls.subList(0, MAX_IMAGES);
        }

        Path dir = Paths.get(uploadDir, "github", cfg(KEY_REPO, "pm").trim(), "issue-" + issueNumber)
                .toAbsolutePath().normalize();
        List<GitHubIssueDetailDTO.ImageDTO> images = new ArrayList<>();
        int index = 1;
        for (String url : urls) {
            GitHubIssueDetailDTO.ImageDTO image = new GitHubIssueDetailDTO.ImageDTO();
            image.setSourceUrl(url);

            boolean githubHosted = isGitHubHost(url);
            if (!githubHosted) {
                image.setDownloaded(false);
                image.setNote("外部图床图片，未下载");
                images.add(image);
                index++;
                continue;
            }

            // 已存在同序号的图片则直接复用，避免重复下载
            String extFromUrl = extractExt(url);
            if (StringUtils.hasText(extFromUrl)) {
                Path existing = dir.resolve(String.format("img-%d.%s", index, extFromUrl));
                if (Files.exists(existing)) {
                    image.setLocalPath(existing.toAbsolutePath().toString().replace("\\", "/"));
                    image.setPreviewUrl(previewUrlOf(issueNumber, existing.getFileName().toString()));
                    image.setDownloaded(true);
                    images.add(image);
                    index++;
                    continue;
                }
            }

            try {
                HttpResponse<byte[]> response = download(url);
                if (response.statusCode() != 200) {
                    image.setDownloaded(false);
                    image.setNote("下载失败：HTTP " + response.statusCode());
                } else if (response.body().length > MAX_IMAGE_BYTES) {
                    image.setDownloaded(false);
                    image.setNote("超过 5MB 限制");
                } else {
                    Files.createDirectories(dir);
                    Path file = dir.resolve(String.format("img-%d.%s", index, resolveExt(url, response)));
                    Files.write(file, response.body());
                    image.setLocalPath(file.toAbsolutePath().toString().replace("\\", "/"));
                    image.setPreviewUrl(previewUrlOf(issueNumber, file.getFileName().toString()));
                    image.setDownloaded(true);
                }
            } catch (IOException e) {
                log.warn("GitHub Issue 图片下载失败: url={}, 原因={}", url, e.getMessage());
                image.setDownloaded(false);
                image.setNote("下载失败");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                image.setDownloaded(false);
                image.setNote("下载被中断");
            }

            if (!image.isDownloaded()) {
                imageWarnings.add("第 " + index + " 张图片未下载（" + image.getNote() + "），仅保留原链接");
            }
            images.add(image);
            index++;
        }
        return images;
    }

    /**
     * 页面预览相对地址
     */
    private String previewUrlOf(int issueNumber, String fileName) {
        return String.format("/uploads-github/%s/issue-%d/%s", cfg(KEY_REPO, "pm").trim(), issueNumber, fileName);
    }

    /**
     * 带 Token 下载图片字节
     */
    private HttpResponse<byte[]> download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token())
                .header("Accept", "image/*")
                .GET()
                .build();
        return client().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * GET 请求 GitHub API 并解析 JSON，非 200 转友好业务异常
     */
    private JsonNode getJson(String path) {
        String url = normalizeBaseUrl(cfg(KEY_BASE_URL, "https://api.github.com")) + path;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + token())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                try {
                    return objectMapper.readTree(response.body());
                } catch (IOException e) {
                    throw new IllegalArgumentException("GitHub 返回内容解析失败");
                }
            }
            switch (response.statusCode()) {
                case 401 -> throw new IllegalArgumentException("GitHub Token 无效或已过期，请更新配置");
                case 403 -> throw new IllegalArgumentException("Token 无权访问该仓库，请检查 PAT 权限范围（需目标仓库的 Issues 和 Bugs 只读权限）");
                case 404 -> throw new IllegalArgumentException("GitHub 资源不存在（404）：" + path);
                default -> throw new IllegalArgumentException("GitHub 返回异常状态码 " + response.statusCode());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            String hint = isSslError(e) ? "；若流量经公司防火墙/代理拦截，请在配置中填写本地代理地址" : "";
            throw new IllegalArgumentException("无法连接 GitHub，请检查网络或代理设置：" + e.getMessage() + hint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("GitHub 请求被中断");
        }
    }

    /**
     * 是否为 GitHub 图片资产域名（仅 GitHub 托管的图片才带 Token 下载）
     */
    private boolean isGitHubHost(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return false;
            }
            return host.equals("github.com") || host.endsWith(".github.com")
                    || host.equals("user-images.githubusercontent.com") || host.endsWith(".githubusercontent.com");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 是否为 SSL 证书类错误（常见于公司防火墙劫持场景）
     */
    private boolean isSslError(IOException e) {
        String message = e.getMessage();
        return message != null && (message.contains("PKIX") || message.contains("SSL") || message.contains("certificate"));
    }

    /**
     * 按代理配置构建 HttpClient（代理变更时重建）
     */
    private HttpClient client() {
        String host = Optional.ofNullable(cfg(KEY_PROXY_HOST, "")).map(String::trim).orElse("");
        String port = Optional.ofNullable(cfg(KEY_PROXY_PORT, "")).map(String::trim).orElse("");
        String key = host + ":" + port;
        if (httpClient == null || !key.equals(clientProxyKey)) {
            HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(TIMEOUT);
            if (StringUtils.hasText(host)) {
                int proxyPort;
                try {
                    proxyPort = StringUtils.hasText(port) ? Integer.parseInt(port) : 80;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("代理端口配置错误：" + port);
                }
                builder.proxy(ProxySelector.of(new InetSocketAddress(host, proxyPort)));
            }
            httpClient = builder.build();
            clientProxyKey = key;
        }
        return httpClient;
    }

    /**
     * 当前 Token 对应的登录名
     */
    private String currentLogin() {
        return testConnection().get("login");
    }

    /**
     * 列表行解析
     */
    private GitHubIssueDTO parseIssue(JsonNode node) {
        GitHubIssueDTO dto = new GitHubIssueDTO();
        dto.setNumber(node.path("number").asInt());
        dto.setTitle(node.path("title").asText(""));
        dto.setState(node.path("state").asText(""));
        List<String> labels = new ArrayList<>();
        node.path("labels").forEach(label -> labels.add(label.path("name").asText("")));
        dto.setLabels(labels);
        dto.setBug(isBugIssue(labels));
        dto.setHtmlUrl(node.path("html_url").asText(""));
        dto.setCreatedAt(node.path("created_at").asText(""));
        dto.setUpdatedAt(node.path("updated_at").asText(""));
        dto.setAssignee(node.path("assignee").path("login").asText(""));
        return dto;
    }

    /**
     * 是否命中配置的 bug 标签
     */
    private boolean isBugIssue(List<String> labels) {
        if (labels.isEmpty()) {
            return false;
        }
        Set<String> bugLabels = Arrays.stream(cfg(KEY_BUG_LABELS, "bug").split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return labels.stream().anyMatch(bugLabels::contains);
    }

    /**
     * 解密并返回 Token，未配置时抛友好异常
     */
    private String token() {
        String encrypted = cfg(KEY_TOKEN, "");
        if (!StringUtils.hasText(encrypted)) {
            throw new IllegalArgumentException("请先在「配置」中填写 GitHub Token");
        }
        return stringEncryptor.decrypt(encrypted.trim());
    }

    /**
     * 仓库路径 owner/repo
     */
    private String repoPath() {
        String owner = Optional.ofNullable(cfg(KEY_OWNER, "LeaderrunTeam")).map(String::trim).orElse("");
        String repo = Optional.ofNullable(cfg(KEY_REPO, "pm")).map(String::trim).orElse("");
        if (!StringUtils.hasText(owner) || !StringUtils.hasText(repo)) {
            throw new IllegalArgumentException("请先在「配置」中填写组织名与仓库名");
        }
        return owner + "/" + repo;
    }

    /**
     * 图片扩展名：优先响应 Content-Type，其次 URL 后缀，兜底 png
     */
    private String resolveExt(String url, HttpResponse<byte[]> response) {
        Map<String, String> typeMap = Map.of(
                "image/png", "png",
                "image/jpeg", "jpg",
                "image/gif", "gif",
                "image/webp", "webp",
                "image/bmp", "bmp");
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
            if (contentType.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        String fromUrl = extractExt(url);
        return StringUtils.hasText(fromUrl) ? fromUrl : "png";
    }

    /**
     * 从 URL 提取图片扩展名，取不到返回空串
     */
    private String extractExt(String url) {
        String path = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        if (dot <= slash) {
            return "";
        }
        String ext = path.substring(dot + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,5}") ? ext : "";
    }

    /**
     * 字符串截断
     */
    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    /**
     * 读配置值
     */
    private String cfg(String key, String defaultValue) {
        return Optional.ofNullable(configService.getConfigValue(key, defaultValue)).orElse(defaultValue);
    }

    /**
     * 写配置值
     */
    private void setCfg(String key, String value, String description) {
        configService.setConfig(key, value, description);
    }

    /**
     * 非空值才保存，避免请求体未携带的字段被清空
     */
    private void saveIfPresent(String value, String key, String description) {
        if (value != null) {
            setCfg(key, value, description);
        }
    }

    /**
     * 规范化 API 基地址：去尾部斜杠
     */
    private String normalizeBaseUrl(String baseUrl) {
        String url = Optional.ofNullable(baseUrl).map(String::trim).orElse("");
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}