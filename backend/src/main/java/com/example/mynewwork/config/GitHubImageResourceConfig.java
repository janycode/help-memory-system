package com.example.mynewwork.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GitHub Issue 图片静态资源映射
 * 将 uploads/github 目录映射为 /uploads-github/**，供页面预览已下载的图片
 *
 * @author jiangyuan
 */
@Configuration
public class GitHubImageResourceConfig implements WebMvcConfigurer {

    @Value("${app.file-storage.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir, "github").toAbsolutePath().normalize();
        // toUri 对目录不保证尾部斜杠，必须补齐，否则资源处理器把最后一段当文件名
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads-github/**")
                .addResourceLocations(location);
    }
}