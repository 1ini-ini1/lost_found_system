package com.zjut.lost_found.repository;

import com.zjut.lost_found.entity.Notice;
import com.zjut.lost_found.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知数据访问层
 */
@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {

    // 按接收人查询通知
    List<Notice> findByReceiveUserOrderByCreateTimeDesc(User receiveUser);

    // 按接收人和已读状态查询
    List<Notice> findByReceiveUserAndIsRead(User receiveUser, Boolean isRead);
}