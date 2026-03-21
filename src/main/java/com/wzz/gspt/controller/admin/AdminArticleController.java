package com.wzz.gspt.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wzz.gspt.common.Result;
import com.wzz.gspt.dto.article.AdminArticleQueryRequest;
import com.wzz.gspt.dto.article.ArticleImageWallSaveRequest;
import com.wzz.gspt.dto.article.ArticlePublishRequest;
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
 * 后台文章管理控制器
 *
 * <p>提供管理员文章分页查询、详情、增删改查接口。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/article")
public class AdminArticleController {

    /**
     * 文章服务
     */
    private final ArticleService articleService;

    /**
     * 管理员分页查询文章接口
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    public Result<IPage<ArticleVO>> pageAdminArticles(@RequestBody AdminArticleQueryRequest request) {
        return Result.success(articleService.pageAdminArticles(request));
    }

    /**
     * 管理员获取文章详情接口
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @GetMapping("/info/{articleId}")
    public Result<ArticleVO> getAdminArticleDetail(@PathVariable Long articleId) {
        return Result.success(articleService.getAdminArticleDetail(articleId));
    }

    /**
     * 管理员新增文章接口
     *
     * @param request 新增请求
     * @return 新增结果
     */
    @PostMapping("/add")
    public Result<ArticleVO> createAdminArticle(@RequestBody ArticlePublishRequest request) {
        return Result.success(articleService.createAdminArticle(request));
    }

    /**
     * 管理员修改文章接口
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PostMapping("/update")
    public Result<ArticleVO> updateAdminArticle(@RequestBody ArticlePublishRequest request) {
        return Result.success(articleService.updateAdminArticle(request));
    }

    /**
     * 保存后台文章图片墙
     *
     * @param request 图片墙请求
     * @return 更新后的文章详情
     */
    @PostMapping("/image-wall/save")
    public Result<ArticleVO> saveAdminArticleImageWall(@RequestBody ArticleImageWallSaveRequest request) {
        return Result.success(articleService.saveAdminArticleImageWall(request));
    }

    /**
     * 编辑后台文章图片墙
     *
     * @param request 图片墙请求
     * @return 更新后的文章详情
     */
    @PostMapping("/image-wall/update")
    public Result<ArticleVO> updateAdminArticleImageWall(@RequestBody ArticleImageWallSaveRequest request) {
        return Result.success(articleService.saveAdminArticleImageWall(request));
    }

    /**
     * 管理员删除文章接口
     *
     * @param articleId 文章 ID
     * @return 删除结果
     */
    @GetMapping("/delete/{articleId}")
    public Result<?> deleteAdminArticle(@PathVariable Long articleId) {
        articleService.deleteAdminArticle(articleId);
        return Result.success();
    }
}
