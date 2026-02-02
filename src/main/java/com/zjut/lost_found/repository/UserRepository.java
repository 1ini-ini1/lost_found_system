package com.zjut.lost_found.repository;

import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层（0基础必懂）
 * 继承JpaRepository：提供基础CRUD操作（save、findById等）
 * 继承JpaSpecificationExecutor：支持动态多条件查询
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // 按账号查询用户（登录时用）
    Optional<User> findByUsername(String username);

    // 按账号和启用状态查询（登录校验：账号存在且启用）
    Optional<User> findByUsernameAndIsEnabled(String username, Boolean isEnabled);

    // 按角色查询用户（管理员查询所有普通用户）
    Iterable<User> findByRole(UserRoleEnum role);
}