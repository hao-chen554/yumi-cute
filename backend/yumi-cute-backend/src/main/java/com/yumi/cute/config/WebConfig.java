package com.yumi.cute.config;

import com.yumi.cute.interceptor.FileAccessInterceptor;
import com.yumi.cute.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final FileAccessInterceptor fileAccessInterceptor;

    @Value("${storage.local.path}")
    private String storagePath;

    public WebConfig(JwtInterceptor jwtInterceptor,FileAccessInterceptor fileAccessInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.fileAccessInterceptor =  fileAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 接口
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/files/**");

        // 静态文件
        registry.addInterceptor(fileAccessInterceptor)
                .addPathPatterns("/files/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把 URL 的 /files/** 映射到本地磁盘目录
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + storagePath + "/");
    }
}