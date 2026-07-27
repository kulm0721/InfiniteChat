package com.shanyangcode.userservice.config;

import java.util.ArrayList;
import java.util.Arrays;

import com.shanyangcode.userservice.handler.JwtHandler;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtHandler jwtHandler;

    private final ArrayList<String> excludePath = new ArrayList<>(Arrays.asList("/api/user/login/code", "/api/user/register", "/api/user/sendCaptcha", "/api/user/login/password", "/api/user/refresh"));

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtHandler).addPathPatterns("/**").excludePathPatterns(excludePath);
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOriginPatterns("*") // 生产环境应指定具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*").allowCredentials(true).maxAge(3600);
    }
}