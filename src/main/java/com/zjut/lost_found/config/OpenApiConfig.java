package com.zjut.lost_found.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    // 1. 配置OpenAPI版本（Swagger UI必须）
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园失物招领系统")
                        .version("1.0.0")
                        .description("接口文档"));
    }

    // 2. 配置接口分组（强制扫描控制器）
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("所有接口")
                .pathsToMatch("/api/**") // 扫描所有/api开头的接口
                .build();
    }
}