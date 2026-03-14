package com.wzz.gspt.dto.file;

import lombok.Data;

import java.util.List;

/**
 * 文件批量删除请求对象
 */
@Data
public class FileBatchDeleteRequest {

    /**
     * 文件记录 ID 列表
     */
    private List<Long> fileIds;
}
