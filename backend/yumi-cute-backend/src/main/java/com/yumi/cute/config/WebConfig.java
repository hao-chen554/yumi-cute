package com.yumi.cute.config;

import com.yumi.cute.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${storage.local.path}")
    private String storagePath;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/files/**");   // ← 新增：静态资源不校验登录
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把 URL 的 /files/** 映射到本地磁盘目录
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + storagePath + "/");
    }
}