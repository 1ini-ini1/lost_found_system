

package com.zjut.lost_found.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置（适配Spring Boot 4.0.2版本，0基础必懂）
 * 作用：自动生成API文档，支持在线调试接口
 * 访问地址：http://localhost:8080/swagger-ui.html
 * 核心说明：Spring Boot 4.0.2版本无法直接搜索到旧版SpringDoc OpenAPI Starter，需使用适配4.x版本的依赖
 * 适配依赖（Maven，直接复制到pom.xml即可）：
 * <dependency>
 *     <groupId>org.springdoc</groupId>
 *     <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
 *     <version&gt;2.3.0&lt;/version&gt;  <!-- 2.3.0及以上版本适配Spring Boot 4.x -->
 * </dependency>
 * 依赖适配说明：
 * 1. 若上述依赖仍无法引入，可排除冲突依赖并指定兼容版本，完整配置如下：
 * <dependency>
 *     <groupId>org.springdoc</groupId>
 *     <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
 *     <version>2.3.0</version>
 *     <exclusions>
 *         <exclusion>
 *             <groupId>org.springframework.boot</groupId>
 *             <artifactId>spring-boot-starter-web</artifactId>
 *         </exclusion>
 *     </exclusions>
 * </dependency>
 * 2. 若使用Gradle，添加以下依赖：
 * implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
 * 3. 依赖引入后若启动报错，检查项目是否存在spring-webmvc依赖，缺失则补充：
 * <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-webmvc</artifactId>
 * </dependency>
 */
@Configuration
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
     * 文档基础信息（标题、版本、作者等）
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园失物招领系统API文档")
                        .version("1.0.0")
                        .description("包含用户、物品、认领申请的核心接口，支持在线调试")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@zjut.edu.cn")));
    }
}