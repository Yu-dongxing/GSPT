package com.wzz.gspt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wzz.gspt.dto.file.AdminFileQueryRequest;
import com.wzz.gspt.dto.file.FileBatchDeleteRequest;
import com.wzz.gspt.dto.file.MyFileQueryRequest;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.vo.FileRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件记录服务接口
 *
 * <p>继承 MyBatis-Plus 的 IService，统一提供文件上传、查询与删除能力。</p>
 */
public interface FileRecordService extends IService<FileRecord> {

    /**
     * 执行文件上传
     *
     * @param file 上传文件
     * @return 文件上传成功后的记录信息
     */
    FileRecordVO upload(MultipartFile file);

    /**
     * 管理员分页查询文件记录
     *
     * @param request 查询请求
     * @return 文件分页结果
     */
    IPage<FileRecordVO> pageAdminFiles(AdminFileQueryRequest request);

    /**
     * 普通用户或企业用户分页查询自己上传的文件记录
     *
     * @param request 查询请求
     * @return 文件分页结果
     */
    IPage<FileRecordVO> pageMyFiles(MyFileQueryRequest request);

    /**
     * 管理员删除单个文件记录
     *
     * @param fileId 文件记录 ID
     */
    void deleteAdminFile(Long fileId);

    /**
     * 管理员批量删除文件记录
     *
     * @param request 批量删除请求
     */
    void deleteAdminFiles(FileBatchDeleteRequest request);

    /**
     * 普通用户或企业用户删除自己上传的单个文件记录
     *
     * @param fileId 文件记录 ID
     */
    void deleteMyFile(Long fileId);

    /**
     * 普通用户或企业用户批量删除自己上传的文件记录
     *
     * @param request 批量删除请求
     */
    void deleteMyFiles(FileBatchDeleteRequest request);

    /**
     * 删除已经不存在业务引用的文件
     *
     * @param fileId 文件记录 ID
     */
    void deleteFileIfUnreferenced(Long fileId);

    /**
     * 批量删除已经不存在业务引用的文件
     *
     * @param fileIds 文件记录 ID 列表
     */
    void deleteFilesIfUnreferenced(List<Long> fileIds);
}
