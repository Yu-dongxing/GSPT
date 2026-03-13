package com.wzz.gspt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wzz.gspt.pojo.FileRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件记录数据访问层
 *
 * <p>用于操作文件上传记录表 sys_file。</p>
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
}
