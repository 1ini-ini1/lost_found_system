package com.zjut.lost_found.repository;

import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品数据访问层
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    // 按发布人查询物品（我的发布）
    List<Item> findByPublisher(User publisher);

    // 按物品状态查询（如查询待审核物品）
    List<Item> findByStatus(ItemStatusEnum status);

    // 按物品名称模糊查询（搜索功能）
    List<Item> findByNameContainingIgnoreCase(String name);

    // 按物品类型查询（分类筛选）
    List<Item> findByType(String type);

    // 按状态和创建时间查询（自动归档：已通过且超期未认领）
    List<Item> findByStatusAndCreateTimeBefore(ItemStatusEnum status, LocalDateTime createTime);
}