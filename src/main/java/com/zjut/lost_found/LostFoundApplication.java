package com.zjut.lost_found;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 校园失物招领系统启动类
 * 0基础关键注解说明：
 * @SpringBootApplication：核心注解，标识这是一个Spring Boot项目，自动整合所有依赖和配置
 * @EnableJpaAuditing：启用JPA审计功能（用于自动填充实体类的创建时间、更新时间）
 */
@SpringBootApplication
@EnableJpaAuditing
public class LostFoundApplication {

    public static void main(String[] args) {
        // 启动Spring Boot项目，参数是当前类的class对象
        SpringApplication.run(LostFoundApplication.class, args);
        // 启动成功后打印提示，方便确认项目启动正常
        System.out.println("校园失物招领系统启动成功！访问地址：http://localhost:8080");
    }

}