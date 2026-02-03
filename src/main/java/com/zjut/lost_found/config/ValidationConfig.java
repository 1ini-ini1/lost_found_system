package com.zjut.lost_found.config;

import org.hibernate.validator.HibernateValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Configuration
public class ValidationConfig {
    @Bean
    public Validator validator() {
        ValidatorFactory factory = Validation.byProvider(HibernateValidator.class)
                .configure()
                // 快速失败模式：只要有一个字段校验失败就返回
                .failFast(true)
                .buildValidatorFactory();
        return factory.getValidator();
    }
}