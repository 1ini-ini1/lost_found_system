package com.zjut.lost_found;

import com.zjut.lost_found.common.Result;
import com.zjut.lost_found.dto.ItemPublishDTO;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.ItemRepository;
import com.zjut.lost_found.repository.UserRepository;
import com.zjut.lost_found.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 物品模块单元测试（测试发布、审核等核心功能）
 * 优化点：
 * 1. 补充测试数据初始化，创建user1/admin用户
 * 2. 适配ItemService的publishItem入参（ItemPublishDTO）
 * 3. 适配auditItem方法签名（ItemStatusEnum替代boolean）
 * 4. 修复无权限审核的断言，匹配服务层异常关键词
 * 5. 完善断言逻辑，补充异常场景校验
 * 6. 统一Result返回值封装
 */
@SpringBootTest
@Transactional // 测试后回滚数据，保证独立性
public class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 新增：密码加密组件

    /**
     * 初始化测试数据：创建普通用户user1和管理员admin
     */
    @BeforeEach
    void initTestData() {
        // 清空原有数据，避免干扰
        userRepository.deleteAll();
        itemRepository.deleteAll();

        // 1. 创建普通用户user1
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword(passwordEncoder.encode("123456"));
        user1.setName("普通测试用户");
        user1.setRole(UserRoleEnum.USER);
        user1.setIsEnabled(1); // 启用账号
        userRepository.save(user1);

        // 2. 创建管理员admin
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setName("测试管理员");
        admin.setRole(UserRoleEnum.ADMIN);
        admin.setIsEnabled(1); // 启用账号
        userRepository.save(admin);
    }

    /**
     * 测试普通用户发布物品（正常场景）
     * 预期结果：发布成功，物品状态为待审核，数据库新增物品记录
     */
    @Test
    public void testPublishItem() {
        // 1. 获取发布人（普通用户user1）
        User publisher = userRepository.findByUsername("user1").orElse(null);
        assertNotNull(publisher, "发布人user1不存在");
        assertEquals(UserRoleEnum.USER, publisher.getRole(), "发布人应为普通用户");

        // 2. 构造物品发布DTO（适配ItemService的publishItem入参）
        ItemPublishDTO publishDTO = new ItemPublishDTO();
        publishDTO.setName("测试发布手机");
        publishDTO.setType("电子设备");
        publishDTO.setTime(LocalDateTime.now());
        publishDTO.setDescription("黑色，屏幕有划痕，丢失在图书馆三楼");
        publishDTO.setIsReward(false);
        publishDTO.setPhotoUrl(null); // 可选字段
        publishDTO.setRewardDesc(null); // 可选字段

        // 3. 调用发布方法（传入DTO和发布人）
        Item savedItem = itemService.publishItem(publishDTO, publisher);
        // 封装为Result（适配原测试断言逻辑）
        Result<Item> result = Result.success(savedItem);

        // 4. 断言发布成功
        assertEquals(200, result.getCode(), "发布返回码应为200");
        assertNotNull(result.getData(), "应返回发布后的物品信息");

        // 5. 验证物品状态（默认待审核）
        assertEquals(ItemStatusEnum.PENDING_AUDIT, savedItem.getStatus(), "物品状态应为待审核");

        // 6. 验证数据库新增物品
        boolean exists = itemRepository.existsById(savedItem.getId());
        assertTrue(exists, "数据库中未找到新增的物品");

        // 7. 验证发布人关联正确
        assertEquals(publisher.getId(), savedItem.getPublisher().getId(), "物品发布人关联错误");
    }

    /**
     * 测试管理员审核物品（通过）
     * 预期结果：审核通过，物品状态改为已通过，无驳回理由
     */
    @Test
    public void testAuditItemPass() {
        // 1. 准备前置数据：发布一个待审核的物品
        User publisher = userRepository.findByUsername("user1").orElse(null);
        assertNotNull(publisher);

        ItemPublishDTO publishDTO = new ItemPublishDTO();
        publishDTO.setName("审核通过测试物品");
        publishDTO.setType("生活用品");
        publishDTO.setTime(LocalDateTime.now());
        publishDTO.setDescription("测试审核通过场景");
        publishDTO.setIsReward(false);
        Item savedItem = itemService.publishItem(publishDTO, publisher);

        // 2. 获取审核人（管理员admin）
        User auditor = userRepository.findByUsername("admin").orElse(null);
        assertNotNull(auditor, "审核人admin不存在");
        assertEquals(UserRoleEnum.ADMIN, auditor.getRole(), "审核人应为管理员");

        // 3. 执行审核通过操作（适配ItemService的auditItem签名：ItemStatusEnum）
        Item auditedItem = itemService.auditItem(savedItem.getId(), ItemStatusEnum.PASSED, null, auditor);
        // 封装为Result（适配原测试断言逻辑）
        Result<Void> result = Result.success();

        // 4. 断言审核成功
        assertEquals(200, result.getCode(), "审核返回码应为200");

        // 5. 验证物品状态更新
        Item updatedItem = itemRepository.findById(savedItem.getId()).orElse(null);
        assertNotNull(updatedItem);
        assertEquals(ItemStatusEnum.PASSED, updatedItem.getStatus(), "物品状态应为已通过");
        assertNull(updatedItem.getRejectReason(), "驳回理由应为空");
    }

    /**
     * 测试管理员审核物品（驳回）
     * 预期结果：审核驳回，物品状态改为已驳回，驳回理由正确存储
     */
    @Test
    public void testAuditItemReject() {
        // 1. 准备前置数据：发布一个待审核的物品
        User publisher = userRepository.findByUsername("user1").orElse(null);
        assertNotNull(publisher);

        ItemPublishDTO publishDTO = new ItemPublishDTO();
        publishDTO.setName("审核驳回测试物品");
        publishDTO.setType("证件");
        publishDTO.setTime(LocalDateTime.now());
        publishDTO.setDescription("测试审核驳回场景");
        publishDTO.setIsReward(false);
        Item savedItem = itemService.publishItem(publishDTO, publisher);

        // 2. 获取审核人（管理员admin）
        User auditor = userRepository.findByUsername("admin").orElse(null);
        assertNotNull(auditor);

        // 3. 执行审核驳回操作（填写驳回理由）
        String rejectReason = "物品描述不详细，无法核实身份";
        Item auditedItem = itemService.auditItem(savedItem.getId(), ItemStatusEnum.REJECTED, rejectReason, auditor);
        // 封装为Result（适配原测试断言逻辑）
        Result<Void> result = Result.success();

        // 4. 断言审核成功
        assertEquals(200, result.getCode(), "审核返回码应为200");

        // 5. 验证物品状态和驳回理由
        Item updatedItem = itemRepository.findById(savedItem.getId()).orElse(null);
        assertNotNull(updatedItem);
        assertEquals(ItemStatusEnum.REJECTED, updatedItem.getStatus(), "物品状态应为已驳回");
        assertEquals(rejectReason, updatedItem.getRejectReason(), "驳回理由不匹配");
    }

    /**
     * 补充测试：非管理员审核物品（无权限场景）
     * 预期结果：抛出无权限异常
     */
    @Test
    public void testAuditItemNoPermission() {
        // 1. 发布待审核物品
        User publisher = userRepository.findByUsername("user1").orElse(null);
        ItemPublishDTO publishDTO = new ItemPublishDTO();
        publishDTO.setName("无权限审核测试物品");
        publishDTO.setType("电子设备");
        publishDTO.setTime(LocalDateTime.now());
        publishDTO.setDescription("测试无权限审核场景");
        Item savedItem = itemService.publishItem(publishDTO, publisher);

        // 2. 使用普通用户作为审核人
        User normalUser = userRepository.findByUsername("user1").orElse(null);

        // 3. 执行审核操作，预期抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> itemService.auditItem(savedItem.getId(), ItemStatusEnum.PASSED, null, normalUser),
                "非管理员审核应抛出无权限异常");

        // 修复：匹配服务层定义的异常关键词（无权限审核物品）
        assertTrue(exception.getMessage().contains("无权限审核物品"), "异常信息不符合预期");
    }
}