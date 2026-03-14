package com.wzz.gspt.controller.article;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wzz.gspt.common.Result;
import com.wzz.gspt.dto.article.ArticlePublishRequest;
import com.wzz.gspt.dto.article.MyArticleQueryRequest;
import com.wzz.gspt.service.ArticleService;
import com.wzz.gspt.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章控制器
 *
 * <p>提供文章发布、修改、查询自己文章以及删除自己文章的接口。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/article")
public class ArticleController {

    /**
     * 文章服务
     */
    private final ArticleService articleService;

    /**
     * 发布文章接口
     *
     * @param request 发文请求
     * @return 发布结果
     */
    @PostMapping("/publish")
    public Result<ArticleVO> publishArticle(@RequestBody ArticlePublishRequest request) {
        return Result.success(articleService.publishArticle(request));
    }

    /**
     * 修改自己的文章接口
     *
     * @param request 修改请求
     * @return 修改后的文章
     */
    @PostMapping("/my/update")
    public Result<ArticleVO> updateMyArticle(@RequestBody ArticlePublishRequest request) {
        return Result.success(articleService.updateMyArticle(request));
    }

    /**
     * 查询当前登录用户自己的文章列表接口
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/my/page")
    public Result<IPage<ArticleVO>> pageMyArticles(@RequestBody MyArticleQueryRequest request) {
        return Result.success(articleService.pageMyArticles(request));
    }

    /**
     * 查询当前登录用户自己的文章详情接口
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @GetMapping("/my/{articleId}")
    public Result<ArticleVO> getMyArticleDetail(@PathVariable Long articleId) {
        return Result.success(articleService.getMyArticleDetail(articleId));
    }

    /**
     * 删除当前登录用户自己的文章接口
     *
     * @param articleId 文章 ID
     * @return 删除结果
     */
    @GetMapping("/my/delete/{articleId}")
    public Result<?> deleteMyArticle(@PathVariable Long articleId) {
        articleService.deleteMyArticle(articleId);
        return Result.success();
    }
}
