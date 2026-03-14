package com.wzz.gspt.controller.file;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wzz.gspt.common.Result;
import com.wzz.gspt.dto.file.FileBatchDeleteRequest;
import com.wzz.gspt.dto.file.MyFileQueryRequest;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户文件管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file/my")
public class UserFileController {

    /**
     * 文件记录服务
     */
    private final FileRecordService fileRecordService;

    /**
     * 普通用户或企业用户分页查询自己上传的文件记录
     *
     * @param request 查询请求
     * @return 文件分页结果
     */
    @PostMapping("/page")
    public Result<IPage<FileRecordVO>> pageFiles(@RequestBody MyFileQueryRequest request) {
        return Result.success(fileRecordService.pageMyFiles(request));
    }

    /**
     * 普通用户或企业用户删除自己上传的单个文件记录
     *
     * @param fileId 文件记录 ID
     * @return 删除结果
     */
    @GetMapping("/delete/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId) {
        fileRecordService.deleteMyFile(fileId);
        return Result.success();
    }

    /**
     * 普通用户或企业用户批量删除自己上传的文件记录
     *
     * @param request 批量删除请求
     * @return 删除结果
     */
    @GetMapping("/delete/batch")
    public Result<?> deleteFiles(@RequestBody FileBatchDeleteRequest request) {
        fileRecordService.deleteMyFiles(request);
        return Result.success();
    }
}
