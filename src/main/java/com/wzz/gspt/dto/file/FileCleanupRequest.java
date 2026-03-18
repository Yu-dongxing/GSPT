package com.wzz.gspt.dto.file;

import lombok.Data;

/**
 * 文件清理请求
 */
@Data
public class FileCleanupRequest {

    /**
     * 是否仅预览清理结果而不实际删除
     */
    private Boolean dryRun = Boolean.FALSE;
}
