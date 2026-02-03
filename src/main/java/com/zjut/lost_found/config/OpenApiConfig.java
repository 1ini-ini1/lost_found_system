package com.zjut.lost_found.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Swagger配置（适配Spring Boot 4.0.2 + SpringDoc 2.3.0）
 * 核心：显式导入SpringDoc核心配置，确保/v3/api-docs接口被正确注册
 * 访问地址：http://localhost:8080/swagger-ui/index.html
 */
@Configuration
@Import(SpringDocConfiguration.class) // 关键：显式导入SpringDoc核心配置，强制注册/v3/api-docs接口
public class OpenApiConfig {

    /**
     * 物品模块API分组
     */
    @Bean
    public GroupedOpenApi itemApi() {
        return GroupedOpenApi.builder()
                .group("物品管理模块")
                .pathsToMatch("/api/item/**")
                .build();
    }

    /**
     * 用户模块API分组
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户管理模块")
                .pathsToMatch("/api/user/**")
                .build();
    }

    /**
     * 认领申请模块API分组
     */
    @Bean
    public GroupedOpenApi claimApi() {
        return GroupedOpenApi.builder()
                .group("认领申请模块")
                .pathsToMatch("/api/claim/**")
                .build();
    }

    /**
     * 唯一的OpenAPI Bean（确保SpringDoc能识别）
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园失物招领系统接口文档")
                        .version("1.0.0")
                        .description("基于Spring Boot 4.0.2的校园失物招领系统后端接口文档，支持在线调试")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@zjut.edu.cn")));
    }
}