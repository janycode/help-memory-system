package com.example.mynewwork.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        if (isApiPath(resourcePath) || isStaticAsset(resourcePath)) {
                            return null;
                        }
                        
                        // 处理登录页面等前端路由
                        return new ClassPathResource("/static/index.html");
                    }
                    
                    private boolean isApiPath(String path) {
                        return path.startsWith("api/") ||
                               path.startsWith("auth/") ||
                               path.startsWith("debug/") ||
                               path.startsWith("admin/") ||
                               path.startsWith("dashboard/");
                    }
                    
                    private boolean isStaticAsset(String path) {
                        return path.startsWith("assets/") || 
                               path.startsWith("favicon") ||
                               path.endsWith(".js") ||
                               path.endsWith(".css") ||
                               path.endsWith(".ico") ||
                               path.endsWith(".png") ||
                               path.endsWith(".jpg") ||
                               path.endsWith(".svg") ||
                               path.endsWith(".woff") ||
                               path.endsWith(".woff2") ||
                               path.endsWith(".ttf") ||
                               path.endsWith(".eot");
                    }
                });
    }
}
