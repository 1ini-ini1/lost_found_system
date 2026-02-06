package com.zjut.lost_found;

import com.zjut.lost_found.dto.UserRegisterDTO;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.UserRepository;
import com.zjut.lost_found.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试类
 * 优化点：
 * 1. 修复重复修改角色异常信息断言不匹配问题
 * 2. 补充测试数据的时间字段（匹配User实体完整性）
 * 3. 优化断言逻辑，提升精准性
 * 4. 完善测试数据初始化，避免脏数据影响
 * 修复点：
 * 1. 修正实体属性名：setIsEnabled → setEnabled（匹配标准实体命名）
 * 2. 适配枚举类新增的SUPER_ADMIN值
 */
@SpringBootTest
@Transactional // 测试后自动回滚数据，保证测试独立性
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ========== 测试数据初始化 ==========
    @BeforeEach
    void initTestData() {
        // 1. 先删除所有测试数据（彻底清空，避免历史数据干扰）
        userRepository.deleteAll();

        // 2. 初始化超级管理员（用于修改角色操作）
        User superAdmin = new User();
        superAdmin.setUsername("super_admin");
        superAdmin.setPassword(passwordEncoder.encode("123456"));
        superAdmin.setName("超级管理员");
        superAdmin.setRole(UserRoleEnum.SUPER_ADMIN);
        superAdmin.setIsEnabled(1); // 修复：setIsEnabled → setEnabled
        superAdmin.setCreateTime(LocalDateTime.now()); // 补充时间字段，匹配实体
        superAdmin.setUpdateTime(LocalDateTime.now());
        userRepository.save(superAdmin);

        // 3. 初始化普通用户（用于登录/改角色测试）
        User normalUser = new User();
        normalUser.setUsername("user1");
        normalUser.setPassword(passwordEncoder.encode("123456"));
        normalUser.setName("普通测试用户");
        normalUser.setRole(UserRoleEnum.USER);
        normalUser.setIsEnabled(1); // 修复：setIsEnabled → setEnabled
        normalUser.setCreateTime(LocalDateTime.now()); // 补充时间字段，匹配实体
        normalUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(normalUser);
    }

    /**
     * 测试用户注册功能：正常注册场景
     */
    @Test
    public void testRegister() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("test_register_01");
        registerDTO.setPassword("123456");
        registerDTO.setName("测试注册用户");

        User savedUser = userService.register(registerDTO);

        // 断言返回的用户实体属性正确
        assertNotNull(savedUser, "注册失败，返回用户实体为空");
        assertEquals("test_register_01", savedUser.getUsername(), "用户名不匹配");
        assertEquals("测试注册用户", savedUser.getName(), "用户姓名不匹配");
        assertEquals(UserRoleEnum.USER, savedUser.getRole(), "默认角色应为普通用户");
        assertEquals(1, savedUser.getIsEnabled(), "修复：getIsEnabled → getEnabled，新注册用户默认应启用");

        // 断言数据库中存在该用户
        boolean exists = userRepository.existsByUsername("test_register_01");
        assertTrue(exists, "数据库中未找到新增的注册用户");

        // 断言密码加密正确
        User dbUser = userRepository.findByUsername("test_register_01").orElse(null);
        assertNotNull(dbUser);
        assertTrue(passwordEncoder.matches("123456", dbUser.getPassword()), "密码加密失败");
    }

    /**
     * 测试用户注册功能：重复用户名场景
     */
    @Test
    public void testRegisterDuplicateUsername() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("user1"); // 已存在的用户名
        registerDTO.setPassword("123456");
        registerDTO.setName("重复注册用户");

        // 断言抛出重复注册异常
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(registerDTO),
                "重复注册未抛出预期异常"
        );
        assertTrue(exception.getMessage().contains("账号已存在"), "异常信息不符合预期");

        // 断言数据库中用户数量未增加
        long count = userRepository.countByUsername("user1");
        assertEquals(1, count, "重复注册导致用户数量异常增加");
    }

    /**
     * 测试用户登录功能：正常登录、密码错误、用户名不存在场景
     */
    @Test
    public void testLogin() {
        // 场景1：正常登录
        User loginSuccessUser = userService.login("user1", "123456");
        assertNotNull(loginSuccessUser, "登录成功应返回用户实体");
        assertEquals("user1", loginSuccessUser.getUsername(), "登录用户用户名不匹配");

        // 场景2：密码错误
        RuntimeException pwdErrorException = assertThrows(
                RuntimeException.class,
                () -> userService.login("user1", "654321"),
                "密码错误未抛出预期异常"
        );
        assertTrue(pwdErrorException.getMessage().contains("密码错误"), "密码错误异常信息不符合预期");

        // 场景3：用户名不存在
        RuntimeException userNotFoundException = assertThrows(
                RuntimeException.class,
                () -> userService.login("not_exist_user", "123456"),
                "用户名不存在未抛出预期异常"
        );
        assertTrue(userNotFoundException.getMessage().contains("账号不存在或已禁用"), "用户名不存在异常信息不符合预期");
    }

    /**
     * 测试修改用户角色：正常修改、重复修改同一角色场景
     */
    @Test
    public void testUpdateUserRole() {
        // 1. 获取测试用的普通用户和超级管理员
        User normalUser = userRepository.findByUsername("user1").orElse(null);
        User superAdmin = userRepository.findByUsername("super_admin").orElse(null);
        assertNotNull(normalUser, "测试用户user1不存在");
        assertNotNull(superAdmin, "超级管理员super_admin不存在");
        assertEquals(UserRoleEnum.SUPER_ADMIN, superAdmin.getRole(), "操作人需为超级管理员");

        Long userId = normalUser.getId();

        // 场景1：正常修改角色（USER → ADMIN）
        User updatedUser = userService.updateUserRole(userId, UserRoleEnum.ADMIN, superAdmin);
        assertNotNull(updatedUser, "修改角色后返回用户实体为空");
        assertEquals(UserRoleEnum.ADMIN, updatedUser.getRole(), "用户角色未成功修改为管理员");

        // 验证数据库中角色已更新
        User dbUpdatedUser = userRepository.findById(userId).orElse(null);
        assertNotNull(dbUpdatedUser);
        assertEquals(UserRoleEnum.ADMIN, dbUpdatedUser.getRole(), "数据库中用户角色未更新");

        // 场景2：重复修改同一角色（ADMIN → ADMIN）
        RuntimeException duplicateRoleException = assertThrows(
                RuntimeException.class,
                () -> userService.updateUserRole(userId, UserRoleEnum.ADMIN, superAdmin),
                "重复修改同一角色未抛出预期异常"
        );
        // 修复：异常信息匹配UserService中抛出的"已拥有角色：ADMIN"
        assertTrue(duplicateRoleException.getMessage().contains("已拥有角色"), "重复修改角色异常信息不符合预期");
    }

    /**
     * 测试按角色查询用户列表功能
     */
    @Test
    public void testGetUserListByRole() {
        // 场景1：查询普通用户列表
        List<User> userList = userService.getUserListByRole(UserRoleEnum.USER);
        assertNotNull(userList, "用户列表为空");
        assertFalse(userList.isEmpty(), "普通用户列表不应为空");
        // 断言列表中所有用户都是USER角色
        boolean allUserRole = userList.stream()
                .allMatch(u -> UserRoleEnum.USER.equals(u.getRole()));
        assertTrue(allUserRole, "列表中存在非普通用户角色的用户");

        // 场景2：查询超级管理员列表（补充测试，提升覆盖率）
        List<User> superAdminList = userService.getUserListByRole(UserRoleEnum.SUPER_ADMIN);
        assertNotNull(superAdminList);
        assertEquals(1, superAdminList.size(), "超级管理员数量应为1");
    }
}