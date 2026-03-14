package com.wzz.gspt.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wzz.gspt.common.Result;
import com.wzz.gspt.dto.file.AdminFileQueryRequest;
import com.wzz.gspt.dto.file.FileBatchDeleteRequest;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员文件管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/file")
public class AdminFileController {

    /**
     * 文件记录服务
     */
    private final FileRecordService fileRecordService;

    /**
     * 管理员分页查询文件记录
     *
     * @param request 查询请求
     * @return 文件分页结果
     */
    @PostMapping("/page")
    public Result<IPage<FileRecordVO>> pageFiles(@RequestBody AdminFileQueryRequest request) {
        return Result.success(fileRecordService.pageAdminFiles(request));
    }

    /**
     * 管理员删除单个文件记录
     *
     * @param fileId 文件记录 ID
     * @return 删除结果
     */
    @GetMapping("/delete/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId) {
        fileRecordService.deleteAdminFile(fileId);
        return Result.success();
    }

    /**
     * 管理员批量删除文件记录
     *
     * @param request 批量删除请求
     * @return 删除结果
     */
    @GetMapping("/delete//batch")
    public Result<?> deleteFiles(@RequestBody FileBatchDeleteRequest request) {
        fileRecordService.deleteAdminFiles(request);
        return Result.success();
    }
}
