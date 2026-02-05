package com.zjut.lost_found.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    // 1. 配置OpenAPI版本 + 新增JWT授权配置（核心修改）
    @Bean
    public OpenAPI customOpenAPI() {
        // 定义JWT安全方案（声明授权类型为Bearer Token）
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // HTTP认证类型
                .scheme("bearer") // 认证方案为bearer（对应JWT的Bearer Token）
                .bearerFormat("JWT") // 令牌格式为JWT
                .name("Authorization"); // 请求头名称（前端传令牌时用的Key）

        // 配置安全上下文（让Swagger识别JWT授权）
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", jwtScheme); // 给安全方案命名为bearerAuth

        return new OpenAPI()
                .info(new Info()
                        .title("校园失物招领系统")
                        .version("1.0.0")
                        .description("接口文档"))
                .components(components) // 绑定安全组件
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth")); // 全局启用JWT授权
    }

    // 2. 配置接口分组（原有代码不变）
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("所有接口")
                .pathsToMatch("/api/**") // 扫描所有/api开头的接口
                .build();
    }
}