package com.wzz.gspt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wzz.gspt.pojo.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章数据访问层
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
