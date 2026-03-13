package com.wzz.gspt.controller.file;

import com.wzz.gspt.common.Result;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.vo.FileRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 *
 * <p>对外提供文件上传接口。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileController {

    /**
     * 文件记录服务
     */
    private final FileRecordService fileRecordService;

    /**
     * 上传文件接口
     *
     * @param file 上传文件
     * @return 上传成功后的文件记录
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileRecordVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileRecordService.upload(file));
    }
}
