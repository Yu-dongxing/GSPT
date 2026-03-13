package com.wzz.gspt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wzz.gspt.pojo.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 *
 * <p>用于查询上传人的基础用户信息。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
