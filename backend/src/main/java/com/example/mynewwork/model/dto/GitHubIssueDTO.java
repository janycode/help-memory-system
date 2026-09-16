package com.example.mynewwork.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Issue 列表行 DTO
 *
 * @author jiangyuan
 */
@Data
public class GitHubIssueDTO {

    /** Issue 编号 */
    private Integer number;

    /** 标题 */
    private String title;

    /** 状态：open / closed */
    private String state;

    /** 标签名列表 */
    private List<String> labels = new ArrayList<>();

    /** 是否 bug（命中配置的 bug 标签） */
    @JsonProperty("isBug")
    private boolean isBug;

    /** GitHub 页面链接 */
    private String htmlUrl;

    /** 创建时间（ISO 格式原文） */
    private String createdAt;

    /** 更新时间（ISO 格式原文） */
    private String updatedAt;

    /** 指派人登录名 */
    private String assignee;
}