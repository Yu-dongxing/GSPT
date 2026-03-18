package com.wzz.gspt.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 未引用文件清理结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCleanupResultVO {

    /**
     * 是否为预览模式
     */
    private Boolean dryRun;

    /**
     * 扫描的文件记录总数
     */
    private Integer totalScanned;

    /**
     * 未引用文件数量
     */
    private Integer unusedCount;

    /**
     * 实际删除成功数量
     */
    private Integer deletedCount;

    /**
     * 删除失败数量
     */
    private Integer failedCount;

    /**
     * 预览或已删除的未引用文件明细
     */
    @Builder.Default
    private List<FileRecordVO> unusedFiles = new ArrayList<>();

    /**
     * 删除成功的文件记录 ID
     */
    @Builder.Default
    private List<Long> deletedFileIds = new ArrayList<>();

    /**
     * 删除失败的文件明细
     */
    @Builder.Default
    private List<FailedFileItem> failedFiles = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedFileItem {

        /**
         * 文件记录 ID
         */
        private Long fileId;

        /**
         * 原始文件名
         */
        private String originalName;

        /**
         * 失败原因
         */
        private String reason;
    }
}
