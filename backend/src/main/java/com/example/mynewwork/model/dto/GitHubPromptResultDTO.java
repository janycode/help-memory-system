package com.example.mynewwork.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Issue 提示词生成结果
 *
 * @author jiangyuan
 */
@Data
public class GitHubPromptResultDTO {

    /** 完整提示词文本（可直接复制发送给 AI Agent） */
    private String prompt;

    /** Issue 编号 */
    private Integer number;

    /** 标题 */
    private String title;

    /** GitHub 页面链接 */
    private String htmlUrl;

    /** 生成过程中的警告（截断/图片未下载等） */
    private List<String> warnings = new ArrayList<>();
}