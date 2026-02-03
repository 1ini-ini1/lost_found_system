package com.zjut.lost_found.dto;

import com.zjut.lost_found.entity.AuditLog;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志响应DTO
 * 作用：格式化日志数据，隐藏敏感信息，适配前端展示
 */
@Data
public class AuditLogResponseDTO {
    private Long id;          // 日志ID
    private String operatorName; // 操作用户姓名
    private String operationType; // 操作类型
    private String operationDesc; // 操作详情
    private String targetId;  // 操作目标ID
    private String createTime; // 操作时间（格式化后）

    /**
     * 实体转DTO
     */
    public static AuditLogResponseDTO fromEntity(AuditLog auditLog) {
        AuditLogResponseDTO dto = new AuditLogResponseDTO();
        dto.setId(auditLog.getId());
        dto.setOperatorName(auditLog.getOperator().getName());
        dto.setOperationType(auditLog.getOperationType());
        dto.setOperationDesc(auditLog.getOperationDesc());
        dto.setTargetId(auditLog.getTargetId());

        // 时间格式化
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dto.setCreateTime(auditLog.getCreateTime().format(formatter));
        return dto;
    }
}