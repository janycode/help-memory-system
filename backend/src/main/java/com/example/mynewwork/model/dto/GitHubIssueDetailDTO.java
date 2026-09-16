package com.example.mynewwork.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Issue 详情 DTO（含评论与图片）
 *
 * @author jiangyuan
 */
@Data
public class GitHubIssueDetailDTO {

    /** Issue 编号 */
    private Integer number;

    /** 标题 */
    private String title;

    /** 状态：open / closed */
    private String state;

    /** 标签名列表 */
    private List<String> labels = new ArrayList<>();

    /** 是否 bug */
    @JsonProperty("isBug")
    private boolean isBug;

    /** GitHub 页面链接 */
    private String htmlUrl;

    /** 正文 Markdown */
    private String body = "";

    /** 指派人登录名 */
    private String assignee;

    /** 创建时间（ISO 格式原文） */
    private String createdAt;

    /** 更新时间（ISO 格式原文） */
    private String updatedAt;

    /** 评论列表（时间升序） */
    private List<CommentDTO> comments = new ArrayList<>();

    /** 图片清单（含本地路径） */
    private List<ImageDTO> images = new ArrayList<>();

    /** 处理警告（正文/评论截断等） */
    private List<String> warnings = new ArrayList<>();

    /** 图片处理警告（下载失败/超量等），提示词不包含图片故单独返回 */
    private List<String> imageWarnings = new ArrayList<>();

    /**
     * 评论
     */
    @Data
    public static class CommentDTO {

        /** 评论人登录名 */
        private String author;

        /** 评论时间（ISO 格式原文） */
        private String createdAt;

        /** 评论内容 Markdown */
        private String body = "";
    }

    /**
     * 图片
     */
    @Data
    public static class ImageDTO {

        /** 原始 URL */
        private String sourceUrl;

        /** 下载后本地绝对路径（未下载为空） */
        private String localPath;

        /** 页面预览相对路径（如 /uploads-github/pm/issue-1/img-1.png，未下载为空） */
        private String previewUrl;

        /** 是否已下载到本地 */
        private boolean downloaded;

        /** 未下载原因 */
        private String note;
    }
}