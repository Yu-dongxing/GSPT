package com.wzz.gspt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wzz.gspt.dto.article.AdminArticleQueryRequest;
import com.wzz.gspt.dto.article.ArticlePublishRequest;
import com.wzz.gspt.dto.article.MyArticleQueryRequest;
import com.wzz.gspt.dto.article.PublicArticleQueryRequest;
import com.wzz.gspt.pojo.Article;
import com.wzz.gspt.vo.ArticleVO;

import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService extends IService<Article> {

    /**
     * 发表文章
     *
     * @param request 发文请求
     * @return 发文结果
     */
    ArticleVO publishArticle(ArticlePublishRequest request);

    /**
     * 修改自己的文章
     *
     * @param request 修改请求
     * @return 修改后的文章
     */
    ArticleVO updateMyArticle(ArticlePublishRequest request);

    /**
     * 查询当前用户自己的文章列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<ArticleVO> pageMyArticles(MyArticleQueryRequest request);

    /**
     * 查询当前用户自己的文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    ArticleVO getMyArticleDetail(Long articleId);

    /**
     * 删除当前用户自己的文章
     *
     * @param articleId 文章 ID
     */
    void deleteMyArticle(Long articleId);

    /**
     * 获取登录用户可访问的文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    ArticleVO getArticleDetail(Long articleId);

    /**
     * 访客公共文章分页查询
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<ArticleVO> pagePublicArticles(PublicArticleQueryRequest request);

    /**
     * 随机获取指定数量的公共文章预览列表
     *
     * @param count 数量
     * @return 文章预览列表
     */
    List<ArticleVO> randomPublicArticles(Integer count);

    /**
     * 管理员分页查询文章
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<ArticleVO> pageAdminArticles(AdminArticleQueryRequest request);

    /**
     * 管理员获取文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    ArticleVO getAdminArticleDetail(Long articleId);

    /**
     * 管理员新增文章
     *
     * @param request 新增请求
     * @return 新增结果
     */
    ArticleVO createAdminArticle(ArticlePublishRequest request);

    /**
     * 管理员修改文章
     *
     * @param request 修改请求
     * @return 修改结果
     */
    ArticleVO updateAdminArticle(ArticlePublishRequest request);

    /**
     * 管理员删除文章
     *
     * @param articleId 文章 ID
     */
    void deleteAdminArticle(Long articleId);
}
