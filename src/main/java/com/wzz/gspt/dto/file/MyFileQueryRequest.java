package com.wzz.gspt.dto.file;

import lombok.Data;

/**
 * 企业用户文件分页查询请求对象
 */
@Data
public class MyFileQueryRequest {

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 10L;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storageName;

    /**
     * 文件后缀
     */
    private String fileSuffix;

    /**
     * 创建开始时间
     */
    private String startCreateTime;

    /**
     * 创建结束时间
     */
    private String endCreateTime;
}
