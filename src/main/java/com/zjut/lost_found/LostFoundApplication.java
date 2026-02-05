package com.zjut.lost_found;
import java.lang.String;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 校园失物招领系统启动类
 * @SpringBootApplication：核心注解，整合@Configuration+@EnableAutoConfiguration+@ComponentScan
 * @EnableJpaAuditing：启用JPA审计（自动填充创建/更新时间）
 * @EnableTransactionManagement：启用事务管理（保证业务操作原子性）
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
public class LostFoundApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
        // 启动成功提示（带颜色，更醒目）
        System.out.println("\033[32m=====================================\033[0m");
        System.out.println("\033[32m校园失物招领系统启动成功！\033[0m");
        System.out.println("\033[32m接口文档地址：http://localhost:8080/swagger-ui.html\033[0m");
        System.out.println("\033[32m=====================================\033[0m");
    }

}