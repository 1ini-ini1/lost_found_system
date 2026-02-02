package com.zjut.lost_found.repository;

import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 认领申请数据访问层
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    // 按物品查询认领申请（物品发布人查看所有申请）
    List<Claim> findByItem(Item item);

    // 按认领人查询申请（我的认领）
    List<Claim> findByClaimer(User claimer);

    // 按物品和认领人查询（防重复申请）
    List<Claim> findByItemAndClaimer(Item item, User claimer);

    // 按申请状态查询（如查询待审核申请）
    List<Claim> findByStatus(String status);
}