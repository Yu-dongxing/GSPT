package com.wzz.gspt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.vo.FileRecordVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件记录服务接口
 *
 * <p>继承 MyBatis-Plus 的 IService，提供文件记录基础能力与上传业务能力。</p>
 */
public interface FileRecordService extends IService<FileRecord> {

    /**
     * 执行文件上传
     *
     * @param file 上传文件
     * @return 文件上传成功后的记录信息
     */
    FileRecordVO upload(MultipartFile file);
}
