package com.zjut.lost_found.service;

import com.zjut.lost_found.dto.UserRegisterDTO;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户核心业务服务（0基础必懂）
 * 核心功能：用户注册、登录、信息修改
 */
@Service  // 标识为Spring业务组件，被容器管理
@RequiredArgsConstructor  // Lombok自动注入依赖（替代@Autowired）
public class UserService {

    private final UserRepository userRepository;  // 注入用户数据访问层
    private final PasswordEncoder passwordEncoder;  // 密码加密组件（Spring Security提供）

    /**
     * 用户注册（核心方法）
     */
    @Transactional(rollbackFor = Exception.class)  // 事务：异常时回滚数据
    public User register(UserRegisterDTO dto) {
        // 1. 校验账号唯一性（账号已存在则抛出异常）
        Optional<User> existingUser = userRepository.findByUsername(dto.getUsername());
        if (existingUser.isPresent()) {
            throw new RuntimeException("账号已存在：" + dto.getUsername());
        }

        // 2. 密码加密（BCrypt算法，不可逆，安全存储）
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 3. 构建用户实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encodedPassword);  // 存储加密后的密码
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        // 角色默认普通用户，账号默认启用（继承BaseEntity自动填充时间）

        // 4. 保存用户到数据库
        return userRepository.save(user);
    }

    /**
     * 用户登录（核心方法）
     */
    public User login(String username, String rawPassword) {
        // 1. 校验账号存在且启用
        User user = userRepository.findByUsernameAndIsEnabled(username, true)
                .orElseThrow(() -> new RuntimeException("账号不存在或已禁用"));

        // 2. 校验密码（明文与加密密码比对，无需解密）
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 登录成功，返回用户信息
        return user;
    }

    /**
     * 按ID查询用户（供其他服务调用）
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在，ID：" + userId));
    }

    /**
     * 修改用户角色（管理员操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateUserRole(Long userId, UserRoleEnum newRole, User operator) {
        // 1. 校验操作人权限（仅超级管理员可修改）
        if (!UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            throw new RuntimeException("无权限修改用户角色");
        }

        // 2. 校验用户存在
        User user = getUserById(userId);

        // 3. 修改角色并保存
        user.setRole(newRole);
        return userRepository.save(user);
    }
}