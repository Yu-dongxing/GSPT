package com.wzz.gspt.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 管理员批量删除用户请求对象
 */
@Data
public class UserBatchDeleteRequest {

    /**
     * 待删除的用户 ID 列表
     */
    private List<Long> userIds;
}
