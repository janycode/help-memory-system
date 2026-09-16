package com.example.mynewwork.service;

import com.example.mynewwork.model.dto.GitHubIssueDetailDTO;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GitHub Issue 提示词生成器
 * 纯模板变量替换，不调用任何 AI 能力，产物供外部 AI Agent 使用
 *
 * @author jiangyuan
 */
public final class GitHubPromptBuilder {

    private static final String TEMPLATE = """
            # 任务：处理 GitHub Issue

            ## 任务目标
            {title}
            {issueUrl}
            标签：{labels}｜状态：{state}

            ## 输入与验收
            ### 正文
            {body}
            ### 评论时间线
            {comments}

            验收标准：以正文中「验收点」为准；无明确验收内容时，输出对应测试场景。

            ## 实现约束
            1. 修改范围：只改与 Issue 相关的文件，保持最小改动，不重构无关代码、不引入新依赖。
            2. 遵循该项目既有编码规范与目录约定。
            3. 全程使用简体中文。
            4. 先输出计划文档，不要做任何代码改动。

            ## 本轮交付要求（三步走）
            1. 第一步：分析问题 —— 定位根因（bug）或梳理需求要点，说明影响范围。
            2. 第二步：输出计划 Markdown 文档，需包含：
               - 技术方案与改动清单（涉及哪些文件、每个文件改什么）
               - 所有需要我确认的技术决策点，逐条列出：
                 · 推荐方案（默认执行）
                 · 可选方案（至少 1 个）
                 · 每个方案的优劣说明（收益 / 代价 / 风险）
               - 风险评估与回滚策略
            3. 第三步：等我确认计划文档后再开始代码实施。确认前不要动任何文件。
            """;

    /** 独立成行的图片标签：连同换行一并删除，避免残留空行 */
    private static final Pattern IMG_TAG_LINE = Pattern.compile("(?im)^[ \\t]*<img\\b[^>]*>[ \\t]*\\r?\\n?");

    /** 行内出现的图片标签 */
    private static final Pattern IMG_TAG_INLINE = Pattern.compile("(?is)<img\\b[^>]*>");

    /** 连续 3 行以上空行，用于收敛整行移除后的多余空行 */
    private static final Pattern EXTRA_BLANK_LINES = Pattern.compile("(?:[ \\t]*\\r?\\n){3,}");

    private GitHubPromptBuilder() {
    }

    /**
     * 按模板拼装提示词
     *
     * @param dto issue 详情
     */
    public static String build(GitHubIssueDetailDTO dto) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("issueUrl", dto.getHtmlUrl());
        vars.put("title", dto.getTitle());
        vars.put("labels", dto.getLabels().isEmpty() ? "（无）" : String.join("、", dto.getLabels()));
        vars.put("state", dto.getState());
        vars.put("body", stripHtmlImages(StringUtils.hasText(dto.getBody()) ? dto.getBody() : "（无正文）"));
        vars.put("comments", formatComments(dto));
        // 注意：正文/评论来自外部，用字面量 replace 而非 replaceAll，避免 $ 与正则语义干扰
        String result = TEMPLATE;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * 去掉正文/评论中的 HTML 图片标签，图片不进提示词
     */
    private static String stripHtmlImages(String text) {
        String withoutImgLines = IMG_TAG_LINE.matcher(text).replaceAll("");
        String withoutImgTags = IMG_TAG_INLINE.matcher(withoutImgLines).replaceAll("");
        return EXTRA_BLANK_LINES.matcher(withoutImgTags).replaceAll("\n\n");
    }

    /**
     * 从 Issue 标签中提取项目标签
     *
     * @deprecated 项目上下文已从提示词模板移除，不再需要
     */
    @Deprecated(forRemoval = true)
    private static String resolveProject(List<String> issueLabels, String projectLabels) {
        if (!StringUtils.hasText(projectLabels) || issueLabels.isEmpty()) {
            return "（未配置项目标签白名单，请在 GitHub 配置中填写项目标签列表）";
        }
        Set<String> whitelist = Arrays.stream(projectLabels.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        String matched = issueLabels.stream()
                .filter(whitelist::contains)
                .collect(Collectors.joining("、"));
        return StringUtils.hasText(matched) ? "所属项目：" + matched : "（未命中项目标签）";
    }

    /**
     * 评论时间线格式化
     */
    private static String formatComments(GitHubIssueDetailDTO dto) {
        if (dto.getComments().isEmpty()) {
            return "（无评论）";
        }
        StringBuilder sb = new StringBuilder();
        for (GitHubIssueDetailDTO.CommentDTO comment : dto.getComments()) {
            sb.append("### ").append(comment.getAuthor()).append("（").append(comment.getCreatedAt()).append("）\n\n")
                    .append(stripHtmlImages(comment.getBody())).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    /**
     * 图片清单格式化：已下载用本地路径，未下载保留原链接并注明原因
     *
     * @deprecated 图片已从提示词模板移除，仅详情抽屉仍展示图片
     */
    @Deprecated(forRemoval = true)
    private static String formatImages(GitHubIssueDetailDTO dto) {
        if (dto.getImages().isEmpty()) {
            return "（本 Issue 无图片）";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (GitHubIssueDetailDTO.ImageDTO image : dto.getImages()) {
            if (image.isDownloaded()) {
                sb.append(index++).append(". ").append(image.getLocalPath()).append("\n");
            } else {
                String note = StringUtils.hasText(image.getNote()) ? "：" + image.getNote() : "";
                sb.append(index++).append(". [未下载").append(note).append("] ").append(image.getSourceUrl()).append("\n");
            }
        }
        return sb.toString();
    }
}