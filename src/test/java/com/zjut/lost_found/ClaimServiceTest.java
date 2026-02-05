package com.zjut.lost_found;

import com.zjut.lost_found.common.Result;
import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ClaimStatusEnum;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.ClaimRepository;
import com.zjut.lost_found.repository.ItemRepository;
import com.zjut.lost_found.repository.UserRepository;
import com.zjut.lost_found.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认领模块单元测试（完整可运行版）
 * 优化点：
 * 1. 补充@BeforeEach初始化测试数据（user1/admin）
 * 2. 完善断言错误提示
 * 3. 补充异常信息校验
 * 4. 确保所有枚举值匹配
 * 5. 修复countByItemAndClaimer方法缺失问题
 * 6. 修正类型不匹配和方法调用错误
 */
@SpringBootTest
@Transactional // 测试后回滚数据，保证独立性
public class ClaimServiceTest {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 初始化测试数据：创建user1（普通用户）和admin（管理员）
     */
    @BeforeEach
    void initTestData() {
        // 清空原有数据，避免干扰
        userRepository.deleteAll();
        itemRepository.deleteAll();
        claimRepository.deleteAll();

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
     * 测试普通用户提交认领申请（正常场景）
     */
    @Test
    public void testSubmitClaim() {
        // 1. 准备前置数据：已通过审核的物品
        User publisher = userRepository.findByUsername("user1").orElse(null);
        assertNotNull(publisher, "发布人user1不存在");

        Item item = new Item();
        item.setName("认领测试手机");
        item.setType("电子设备");
        item.setTime(LocalDateTime.now());
        item.setDescription("黑色，有手机壳，丢失在食堂");
        item.setPublisher(publisher);
        item.setStatus(ItemStatusEnum.PASSED); // 物品已通过审核
        Item savedItem = itemRepository.save(item);

        // 2. 准备认领人
        User claimer = userRepository.findByUsername("user1").orElse(null);
        assertNotNull(claimer, "认领人user1不存在");

        // 3. 构造认领证明
        String proof = "手机机身有划痕，开机密码1234，里面有我的照片";

        // 4. 提交认领申请（正确参数顺序，变量类型为Claim）
        Claim savedClaim = claimService.submitClaim(savedItem.getId(), proof, claimer);

        // 5. 断言提交成功（直接验证Claim对象，无需Result包装）
        assertNotNull(savedClaim, "应返回认领记录");
        assertNotNull(savedClaim.getId(), "认领记录ID不能为空");

        // 6. 验证认领记录（直接使用savedClaim，无需getData()）
        assertEquals(ClaimStatusEnum.PENDING_AUDIT.name(), savedClaim.getStatus(), "认领状态应为待审核");
        assertEquals(claimer.getId(), savedClaim.getClaimer().getId(), "认领人关联错误");
        assertEquals(savedItem.getId(), savedClaim.getItem().getId(), "物品关联错误");
        assertEquals(proof.trim(), savedClaim.getProof(), "认领证明不匹配");
    }

    /**
     * 测试同一用户重复认领同一物品（异常场景）
     */
    @Test
    public void testSubmitDuplicateClaim() {
        // 1. 准备前置数据
        User publisher = userRepository.findByUsername("user1").orElse(null);
        User claimer = userRepository.findByUsername("user1").orElse(null);
        assertNotNull(publisher);
        assertNotNull(claimer);

        Item item = new Item();
        item.setName("重复认领测试物品");
        item.setType("生活用品");
        item.setTime(LocalDateTime.now());
        item.setDescription("测试重复认领场景");
        item.setPublisher(publisher);
        item.setStatus(ItemStatusEnum.PASSED);
        Item savedItem = itemRepository.save(item);

        // 2. 第一次提交认领
        String proof = "测试证明";
        claimService.submitClaim(savedItem.getId(), proof, claimer);

        // 3. 第二次提交（预期失败）
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> claimService.submitClaim(savedItem.getId(), proof, claimer),
                "重复认领应抛出异常");
        assertTrue(exception.getMessage().contains("重复提交"), "异常信息不符合预期");

        // 4. 验证数据库记录数（使用优化后的countByItemAndClaimer方法）
        long count = claimRepository.countByItemAndClaimer(savedItem, claimer);
        assertEquals(1, count, "重复认领导致数据库记录异常");
    }

    /**
     * 测试管理员审核认领申请（通过）
     */
    @Test
    public void testAuditClaimPass() {
        // 1. 准备前置数据
        User publisher = userRepository.findByUsername("user1").orElse(null);
        User claimer = userRepository.findByUsername("user1").orElse(null);
        User auditor = userRepository.findByUsername("admin").orElse(null);
        assertNotNull(publisher);
        assertNotNull(claimer);
        assertNotNull(auditor, "审核人admin不存在");

        // 新增已通过的物品
        Item item = new Item();
        item.setName("认领审核通过测试");
        item.setType("电子设备");
        item.setTime(LocalDateTime.now());
        item.setDescription("测试认领审核通过");
        item.setPublisher(publisher);
        item.setStatus(ItemStatusEnum.PASSED);
        Item savedItem = itemRepository.save(item);

        // 新增待审核的认领申请
        Claim claim = new Claim();
        claim.setItem(savedItem);
        claim.setClaimer(claimer);
        claim.setProof("正确的认领证明");
        claim.setStatus(ClaimStatusEnum.PENDING_AUDIT.name()); // 保存为字符串
        Claim savedClaim = claimRepository.save(claim);

        // 2. 执行审核通过操作（根据ClaimService实际签名调整参数）
        Claim auditedClaim = claimService.auditClaim(savedClaim.getId(), ClaimStatusEnum.APPROVED.name(), null, auditor);

        // 3. 断言审核成功（直接验证Claim对象）
        assertNotNull(auditedClaim, "审核结果不能为空");

        // 4. 验证状态
        Claim updatedClaim = claimRepository.findById(savedClaim.getId()).orElse(null);
        assertNotNull(updatedClaim);
        assertEquals(ClaimStatusEnum.APPROVED.name(), updatedClaim.getStatus(), "认领状态应为已通过");

        Item updatedItem = itemRepository.findById(savedItem.getId()).orElse(null);
        assertNotNull(updatedItem);
        assertEquals(ItemStatusEnum.CLAIMED, updatedItem.getStatus(), "物品状态应为已认领");
    }
}