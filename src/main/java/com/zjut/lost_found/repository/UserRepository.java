package com.zjut.lost_found.repository;

import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // 按账号查询用户（登录时用）
    Optional<User> findByUsername(String username);

    // 优化：参数改为Integer，匹配User实体的isEnabled类型
    Optional<User> findByUsernameAndIsEnabled(String username, Integer isEnabled);

    // 按角色查询用户（管理员查询）
    List<User> findByRole(UserRoleEnum role);

    // 判断用户名是否存在
    boolean existsByUsername(String username);

    // 新增：支持大小写不敏感的用户名查询（增强鲁棒性）
    Optional<User> findByUsernameIgnoreCase(String username);

    // 统计用户名数量
    long countByUsername(String username);

    // 删除指定用户名的用户（测试数据初始化用）
    void deleteByUsername(String username);
}