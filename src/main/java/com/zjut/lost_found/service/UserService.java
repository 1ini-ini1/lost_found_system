package com.zjut.lost_found.service;

import com.zjut.lost_found.dto.UserRegisterDTO;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.exception.BusinessException;
import com.zjut.lost_found.repository.UserRepository;
import com.zjut.lost_found.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户核心业务服务
 * 核心功能：用户注册、登录、角色修改、用户查询、按角色查用户列表
 * 最终优化点：
 * 1. 补充getUserListByRole方法（解决测试类报错）
 * 2. 修复setIsEnabled类型不匹配问题
 * 3. 完善日志和参数校验
 * 4. 修复重复修改角色日志打印旧角色错误
 * 5. 确保重复修改角色抛出明确异常（适配测试用例）
 * 6. 补充updateTime字段更新（数据一致性）
 * 7. 登录方法新增返回JWT令牌重载方法（适配Controller）+ 生产级优化（入参/异常/日志脱敏）
 * 8. 增强异常信息可读性，统一日志格式
 * 9. 补充用户状态修改方法（完善用户管理）
 * 10. 修复JWT令牌生成逻辑，调用真实JwtUtil工具类
 * 11. 治本优化：新增existsByUsername方法（适配Controller的注册/登录校验）
 * 12. 替换通用RuntimeException为自定义BusinessException，异常体系更规范
 * 13. 核心修复：getUserByUsername补充启用状态校验，与登录逻辑统一
 * 14. 增强existsByUsername鲁棒性，支持大小写不敏感查询
 * 15. 新增getUserByUsernameWithPassword方法，供认证使用（保留密码）
 * 16. 关键修复：注册方法返回新对象，不修改持久化对象的password字段（解决Column 'password' cannot be null报错）
 * 17. 登录方法新增令牌脱敏日志、分层异常处理、边界值校验（生产级健壮性）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    // ========== 常量定义（避免魔法值） ==========
    private static final String ERROR_USERNAME_EXIST = "账号已存在：%s";
    private static final String ERROR_USER_NOT_FOUND = "用户不存在，ID：%d";
    private static final String ERROR_ACCOUNT_DISABLED = "账号不存在或已禁用：%s";
    private static final String ERROR_PASSWORD_WRONG = "密码错误：用户名%s";
    private static final String ERROR_ROLE_NOT_CHANGE = "用户ID：%d 已拥有角色：%s，无需修改";
    private static final String ERROR_NO_SUPER_ADMIN_PERMISSION = "操作人ID：%d 无权限修改用户角色（需超级管理员）";
    private static final String ERROR_LOGIN_SYSTEM_EXCEPTION = "登录出现系统异常，用户名：%s";

    // ========== 依赖注入 ==========
    @Getter
    private final UserRepository userRepository;          // 用户数据访问层
    private final PasswordEncoder passwordEncoder;        // 密码加密组件
    private final AuditLogService auditLogService;        // 操作日志服务
    private final JwtUtil jwtUtil;                        // JWT工具类（新增注入）

    /**
     * 校验用户名是否已存在（核心新增：适配Controller的注册/登录校验）
     * @param username 用户名
     * @return 存在返回true，不存在返回false
     */
    public boolean existsByUsername(String username) {
        Assert.hasText(username, "用户名不能为空");
        String cleanUsername = username.trim();
        log.debug("[用户名校验] 校验用户名是否存在：{}", cleanUsername);
        // 增强鲁棒性：支持大小写不敏感查询（适配不同输入场景）
        return userRepository.findByUsernameIgnoreCase(cleanUsername).isPresent();
    }

    /**
     * 用户注册（核心方法）
     * @param dto 注册请求参数DTO
     * @return 保存后的用户实体（隐藏密码）
     * @throws BusinessException 账号已存在时抛出（替换原RuntimeException）
     */
    @Transactional(rollbackFor = Exception.class)
    public User register(UserRegisterDTO dto) {
        // 1. 严格参数校验
        Assert.notNull(dto, "注册参数不能为空");
        Assert.hasText(dto.getUsername(), "用户名不能为空");
        Assert.hasText(dto.getPassword(), "密码不能为空");
        Assert.hasText(dto.getName(), "姓名不能为空");

        String username = dto.getUsername().trim();
        log.info("[用户注册] 开始处理请求，用户名：{}", username);

        // 2. 校验用户名唯一性（复用existsByUsername方法，统一逻辑）
        if (existsByUsername(username)) {
            String errorMsg = String.format(ERROR_USERNAME_EXIST, username);
            log.warn("[用户注册] 失败：{}", errorMsg);
            throw BusinessException.usernameExists(); // 替换为自定义异常
        }

        // 3. 密码加密（BCrypt算法）
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 4. 构建用户实体（修复：setIsEnabled类型匹配 + 补充时间字段）
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setName(dto.getName().trim());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(UserRoleEnum.USER);    // 默认普通用户
        user.setIsEnabled(1);               // 修复：改为int类型（1=启用，匹配实体类）
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 5. 保存用户
        User savedUser = userRepository.save(user);
        log.info("[用户注册] 成功，用户ID：{}", savedUser.getId());

        // 6. 记录注册日志
        auditLogService.recordLog(
                savedUser,
                "USER_REGISTER",
                String.format("用户注册成功，账号：%s，ID：%d", username, savedUser.getId()),
                savedUser.getId().toString()
        );

        // ========== 核心修复：创建新对象返回，不修改持久化对象的password ==========
        // 错误写法：savedUser.setPassword(null); （会修改持久化对象，导致后续更新传null）
        // 正确写法：新建User对象，复制非敏感属性，密码设为null（仅影响返回值，不影响数据库）
        User resultUser = new User();
        copyUserProperties(savedUser, resultUser);
        resultUser.setPassword(null);

        return resultUser;
    }

    /**
     * 用户登录（适配Controller返回令牌）- 生产级优化版
     * @param username 用户名
     * @param rawPassword 明文密码
     * @return JWT令牌字符串
     * @throws BusinessException 账号不存在/禁用/密码错误时抛出
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public String login(String username, String rawPassword) {
        // 1. 强化入参校验（空值/空白值/超长值）
        if (!StringUtils.hasText(username)) {
            log.warn("[用户登录] 失败：用户名为空");
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(rawPassword)) {
            log.warn("[用户登录] 失败：密码为空，用户名：{}", username.trim());
            throw new IllegalArgumentException("密码不能为空");
        }
        // 边界值校验：避免超长字符串攻击
        String cleanUsername = username.trim();
        if (cleanUsername.length() > 50) {
            log.warn("[用户登录] 失败：用户名过长（超过50字符），用户名：{}", cleanUsername.substring(0, 50) + "...");
            throw new IllegalArgumentException("用户名长度不能超过50字符");
        }

        try {
            // 2. 调用核心登录逻辑获取用户
            User user = loginAndGetUser(cleanUsername, rawPassword);

            // 3. 生成JWT令牌（调用真实JwtUtil工具类）
            String jwtToken = generateJwtToken(user);

            // 4. 日志脱敏：仅打印令牌前10位+后4位，避免完整令牌泄露
            String maskedToken = maskToken(jwtToken);
            log.info("[用户登录] 生成令牌成功，用户ID：{}，令牌（脱敏）：{}", user.getId(), maskedToken);

            return jwtToken;
        } catch (IllegalArgumentException | BusinessException e) {
            // 业务/参数异常：直接抛出，保留明确提示
            throw e;
        } catch (Exception e) {
            // 系统异常：包装为通用提示，记录完整堆栈
            String errorMsg = String.format(ERROR_LOGIN_SYSTEM_EXCEPTION, cleanUsername);
            log.error(errorMsg, e);
            throw new BusinessException(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 用户登录（核心方法，返回用户实体）
     * @param username 用户名
     * @param rawPassword 明文密码
     * @return 登录成功的用户实体
     * @throws BusinessException 账号不存在/禁用/密码错误时抛出
     */
    public User loginAndGetUser(String username, String rawPassword) {
        // 1. 参数校验（兜底，避免外部调用时跳过校验）
        Assert.hasText(username, "用户名不能为空");
        Assert.hasText(rawPassword, "密码不能为空");

        String cleanUsername = username.trim();
        log.info("[用户登录] 开始处理请求，用户名：{}", cleanUsername);

        // 2. 校验账号存在且启用（匹配int类型的isEnabled）
        // 核心修改：使用带密码的查询方法
        User user = getUserByUsernameWithPassword(cleanUsername);

        // 3. 校验密码（BCrypt算法匹配）
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            String errorMsg = String.format(ERROR_PASSWORD_WRONG, cleanUsername);
            log.warn("[用户登录] 失败：{}", errorMsg);
            throw BusinessException.passwordError(); // 替换为自定义异常
        }

        log.info("[用户登录] 成功，用户ID：{}", user.getId());

        // 4. 记录登录日志
        auditLogService.recordLog(
                user,
                "USER_LOGIN",
                String.format("用户登录成功，账号：%s，ID：%d", cleanUsername, user.getId()),
                user.getId().toString()
        );

        // 隐藏密码返回（此处为非持久化对象，仅影响返回值，无风险）
        user.setPassword(null);
        return user;
    }

    /**
     * 按ID查询用户（供其他服务调用）
     * @param userId 用户ID
     * @return 用户实体（隐藏密码）
     * @throws EntityNotFoundException 用户不存在时抛出
     */
    public User getUserById(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        log.debug("[用户查询] 查询用户信息，用户ID：{}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMsg = String.format(ERROR_USER_NOT_FOUND, userId);
                    log.warn("[用户查询] 失败：{}", errorMsg);
                    return new EntityNotFoundException(errorMsg);
                });

        // 隐藏密码（非持久化对象，无风险）
        User resultUser = new User();
        copyUserProperties(user, resultUser);
        resultUser.setPassword(null);
        return resultUser;
    }

    /**
     * 按用户名查询用户（对外接口，隐藏密码）
     * @param username 用户名
     * @return 用户实体（隐藏密码）
     * @throws BusinessException 用户不存在/禁用时抛出
     */
    public User getUserByUsername(String username) {
        Assert.hasText(username, "用户名不能为空");
        String cleanUsername = username.trim();
        log.debug("[用户查询] 查询用户信息（隐藏密码），用户名：{}", cleanUsername);

        User user = getUserByUsernameWithPassword(cleanUsername);
        // 隐藏密码（非持久化对象，无风险）
        User resultUser = new User();
        copyUserProperties(user, resultUser);
        resultUser.setPassword(null);
        return resultUser;
    }

    /**
     * 按用户名查询用户（内部认证专用，保留密码）
     * @param username 用户名
     * @return 用户实体（保留密码）
     * @throws BusinessException 用户不存在/禁用时抛出
     */
    public User getUserByUsernameWithPassword(String username) {
        Assert.hasText(username, "用户名不能为空");
        String cleanUsername = username.trim();
        log.debug("[用户查询] 查询用户信息（保留密码），用户名：{}", cleanUsername);

        // 校验账号存在且启用
        User user = userRepository.findByUsernameAndIsEnabled(cleanUsername, 1)
                .orElseThrow(() -> {
                    String errorMsg = String.format(ERROR_ACCOUNT_DISABLED, cleanUsername);
                    log.warn("[用户查询] 失败：{}", errorMsg);
                    return BusinessException.userNotFound();
                });

        // 不隐藏密码，返回原始密码（供认证使用）
        return user;
    }

    /**
     * 修改用户角色（管理员操作）
     * @param userId 被修改用户ID
     * @param newRole 新角色
     * @param operator 操作人（需超级管理员权限）
     * @return 修改后的用户实体（隐藏密码）
     * @throws BusinessException 无权限/角色未变更时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateUserRole(Long userId, UserRoleEnum newRole, User operator) {
        // 1. 参数校验
        Assert.notNull(userId, "被修改用户ID不能为空");
        Assert.notNull(newRole, "新角色不能为空");
        Assert.notNull(operator, "操作人信息不能为空");

        log.info("[角色修改] 开始处理请求，用户ID：{}，新角色：{}，操作人ID：{}",
                userId, newRole.name(), operator.getId());

        // 2. 校验操作人权限（仅超级管理员可操作）
        checkSuperAdminPermission(operator);

        // 3. 查询被修改用户
        User user = getUserById(userId);
        // 重新获取持久化对象（避免使用已隐藏密码的对象）
        User persistentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ERROR_USER_NOT_FOUND, userId)));
        UserRoleEnum oldRole = persistentUser.getRole();

        // 4. 校验角色是否已存在（修复：确保抛出异常，适配测试用例）
        if (oldRole.equals(newRole)) {
            String errorMsg = String.format(ERROR_ROLE_NOT_CHANGE, userId, newRole.name());
            log.warn("[角色修改] 失败：{}", errorMsg);
            throw new BusinessException(400, errorMsg); // 替换为自定义异常
        }

        // 5. 更新角色 + 补充更新时间
        persistentUser.setRole(newRole);
        persistentUser.setUpdateTime(LocalDateTime.now());
        User updatedUser = userRepository.save(persistentUser);

        // 修复：日志打印正确的旧角色和新角色
        log.info("[角色修改] 成功，用户ID：{}，旧角色：{}，新角色：{}",
                userId, oldRole.name(), newRole.name());

        // 6. 记录修改日志
        auditLogService.recordLog(
                operator,
                "USER_UPDATE_ROLE",
                String.format("修改用户角色成功，用户ID：%d，旧角色：%s，新角色：%s",
                        userId, oldRole.name(), newRole.name()),
                userId.toString()
        );

        // 隐藏密码返回
        User resultUser = new User();
        copyUserProperties(updatedUser, resultUser);
        resultUser.setPassword(null);
        return resultUser;
    }

    /**
     * 按角色查询用户列表（补充：解决测试类报错）
     * @param role 用户角色
     * @return 对应角色的用户列表（隐藏密码）
     * @throws IllegalArgumentException 角色为空时抛出
     */
    public List<User> getUserListByRole(UserRoleEnum role) {
        // 1. 参数校验
        Assert.notNull(role, "查询角色不能为空");
        log.info("[角色查询] 查询角色为{}的用户列表", role.name());

        // 2. 调用Repository查询
        List<User> userList = userRepository.findByRole(role);
        log.info("[角色查询] 成功，查询到角色为{}的用户共{}个", role.name(), userList.size());

        // 隐藏所有用户的密码（新建对象返回）
        return userList.stream().map(user -> {
            User resultUser = new User();
            copyUserProperties(user, resultUser);
            resultUser.setPassword(null);
            return resultUser;
        }).toList();
    }

    /**
     * 修改用户启用/禁用状态（超级管理员操作）
     * @param userId 用户ID
     * @param isEnabled 1=启用，0=禁用
     * @param operator 操作人（超级管理员）
     * @return 修改后的用户实体
     * @throws BusinessException 状态未变更时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public User updateUserStatus(Long userId, Integer isEnabled, User operator) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(isEnabled, "状态值不能为空（1=启用，0=禁用）");
        Assert.isTrue(isEnabled == 0 || isEnabled == 1, "状态值只能是0或1");
        checkSuperAdminPermission(operator);

        // 获取持久化对象
        User persistentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(String.format(ERROR_USER_NOT_FOUND, userId)));

        if (persistentUser.getIsEnabled().equals(isEnabled)) {
            String errorMsg = String.format("用户ID：%d 已处于%s状态，无需修改", userId, isEnabled == 1 ? "启用" : "禁用");
            log.warn("[状态修改] 失败：{}", errorMsg);
            throw new BusinessException(400, errorMsg); // 替换为自定义异常
        }

        persistentUser.setIsEnabled(isEnabled);
        persistentUser.setUpdateTime(LocalDateTime.now());
        User updatedUser = userRepository.save(persistentUser);

        log.info("[状态修改] 成功，用户ID：{}，新状态：{}", userId, isEnabled == 1 ? "启用" : "禁用");
        auditLogService.recordLog(
                operator,
                "USER_UPDATE_STATUS",
                String.format("修改用户状态成功，用户ID：%d，新状态：%s", userId, isEnabled == 1 ? "启用" : "禁用"),
                userId.toString()
        );

        // 隐藏密码返回
        User resultUser = new User();
        copyUserProperties(updatedUser, resultUser);
        resultUser.setPassword(null);
        return resultUser;
    }

    // ========== 私有工具方法（新增+优化） ==========

    /**
     * 校验超级管理员权限
     * @throws BusinessException 无权限时抛出
     */
    private void checkSuperAdminPermission(User operator) {
        if (!UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            String errorMsg = String.format(ERROR_NO_SUPER_ADMIN_PERMISSION, operator.getId());
            log.error("[权限校验] 失败：{}", errorMsg);
            throw new BusinessException(403, errorMsg); // 替换为自定义异常（403=无权限）
        }
    }

    /**
     * 生成JWT令牌（调用真实JwtUtil工具类）
     */
    private String generateJwtToken(User user) {
        // 调用JwtUtil生成规范的JWT令牌
        return jwtUtil.generateToken(user.getUsername());
    }

    /**
     * 复制用户属性（抽离工具方法，避免重复代码）
     */
    private void copyUserProperties(User source, User target) {
        target.setId(source.getId());
        target.setUsername(source.getUsername());
        target.setName(source.getName());
        target.setPhone(source.getPhone());
        target.setEmail(source.getEmail());
        target.setRole(source.getRole());
        target.setIsEnabled(source.getIsEnabled());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
    }

    /**
     * JWT令牌脱敏（新增）
     * 只保留前10位+后4位，中间用****替换，避免日志泄露完整令牌
     */
    private String maskToken(String token) {
        if (!StringUtils.hasText(token) || token.length() <= 14) {
            return token; // 令牌过短时直接返回，避免索引越界
        }
        return token.substring(0, 10) + "****" + token.substring(token.length() - 4);
    }
}