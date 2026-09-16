package com.example.mynewwork.model.dto;

import lombok.Data;

/**
 * GitHub 配置 DTO
 * token 仅用于保存入参，读取时通过 tokenConfigured 标识是否已配置
 *
 * @author jiangyuan
 */
@Data
public class GitHubConfigDTO {

    /** GitHub API 基地址 */
    private String baseUrl;

    /** 组织/用户 */
    private String owner;

    /** 仓库名 */
    private String repo;

    /** Classic PAT（仅保存用，留空表示不修改） */
    private String token;

    /** Token 是否已配置（仅读取时返回） */
    private Boolean tokenConfigured;

    /** 代理主机（可选） */
    private String proxyHost;

    /** 代理端口（可选） */
    private String proxyPort;

    /** bug 标签名，多个逗号分隔 */
    private String bugLabels;
}