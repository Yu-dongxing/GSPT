package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.dto.article.AdminArticleQueryRequest;
import com.wzz.gspt.dto.article.ArticlePublishRequest;
import com.wzz.gspt.dto.article.MyArticleQueryRequest;
import com.wzz.gspt.dto.article.PublicArticleQueryRequest;
import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import com.wzz.gspt.exception.BusinessException;
import com.wzz.gspt.mapper.ArticleMapper;
import com.wzz.gspt.mapper.FileRecordMapper;
import com.wzz.gspt.mapper.UserMapper;
import com.wzz.gspt.pojo.Article;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.service.ArticleService;
import com.wzz.gspt.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 文章服务实现类
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    /**
     * 文件记录数据访问层
     */
    private final FileRecordMapper fileRecordMapper;

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * 发表文章
     *
     * @param request 发文请求
     * @return 发文结果
     */
    @Override
    @Transactional
    public ArticleVO publishArticle(ArticlePublishRequest request) {
        validateArticleRequest(request, false);

        User currentUser = getCurrentUser();
        validateArticleCategoryPermission(currentUser, request.getCategory());
        validateArticleCover(request);

        Article article = new Article();
        fillArticle(article, request, currentUser);

        boolean saved = save(article);
        if (!saved) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "发布文章失败");
        }
        return buildArticleVO(article);
    }

    /**
     * 修改自己的文章
     *
     * @param request 修改请求
     * @return 修改后的文章
     */
    @Override
    @Transactional
    public ArticleVO updateMyArticle(ArticlePublishRequest request) {
        validateArticleRequest(request, true);

        User currentUser = getCurrentUser();
        validateArticleCategoryPermission(currentUser, request.getCategory());
        validateArticleCover(request);

        Article article = getOwnedArticle(request.getId(), currentUser.getId());
        fillArticle(article, request, currentUser);

        boolean updated = updateById(article);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改文章失败");
        }
        return buildArticleVO(article);
    }

    /**
     * 查询当前用户自己的文章列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<ArticleVO> pageMyArticles(MyArticleQueryRequest request) {
        validateCommonPageRequest(request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize(), "查询参数不能为空");

        Long currentUserId = getCurrentUserId();
        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, currentUserId)
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .eq(request.getStatus() != null, Article::getStatus, request.getStatus())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = page(page, wrapper);
        return convertArticlePage(articlePage, false);
    }

    /**
     * 查询当前用户自己的文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @Override
    public ArticleVO getMyArticleDetail(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        Article article = getOwnedArticle(articleId, getCurrentUserId());
        return buildArticleVO(article);
    }

    /**
     * 删除当前用户自己的文章
     *
     * @param articleId 文章 ID
     */
    @Override
    @Transactional
    public void deleteMyArticle(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        Article article = getOwnedArticle(articleId, getCurrentUserId());
        boolean removed = removeById(article.getId());
        if (!removed) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "删除文章失败");
        }
    }

    /**
     * 获取登录用户可访问的文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @Override
    public ArticleVO getArticleDetail(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        getCurrentUserId();
        Article article = getPublishedArticle(articleId);
        return buildArticleVO(article);
    }

    /**
     * 访客公共文章分页查询
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<ArticleVO> pagePublicArticles(PublicArticleQueryRequest request) {
        validateCommonPageRequest(request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize(), "公共查询参数不能为空");

        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .like(StringUtils.hasText(request.getAuthorName()), Article::getAuthorName, request.getAuthorName())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = page(page, wrapper);
        return convertArticlePage(articlePage, true);
    }

    /**
     * 管理员分页查询文章
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Override
    public IPage<ArticleVO> pageAdminArticles(AdminArticleQueryRequest request) {
        ensureCurrentUserIsAdmin();
        validateCommonPageRequest(request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize(), "管理员查询参数不能为空");

        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .like(StringUtils.hasText(request.getAuthorName()), Article::getAuthorName, request.getAuthorName())
                .eq(request.getStatus() != null, Article::getStatus, request.getStatus())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = page(page, wrapper);
        return convertArticlePage(articlePage, false);
    }

    /**
     * 管理员获取文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @Override
    public ArticleVO getAdminArticleDetail(Long articleId) {
        ensureCurrentUserIsAdmin();
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        Article article = getById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在");
        }
        return buildArticleVO(article);
    }

    /**
     * 管理员新增文章
     *
     * @param request 新增请求
     * @return 新增结果
     */
    @Override
    @Transactional
    public ArticleVO createAdminArticle(ArticlePublishRequest request) {
        ensureCurrentUserIsAdmin();
        validateArticleRequest(request, false);
        validateAdminArticleCategory(request.getCategory());
        validateArticleCover(request);

        User currentUser = getCurrentUser();
        Article article = new Article();
        fillArticle(article, request, currentUser);

        boolean saved = save(article);
        if (!saved) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "管理员新增文章失败");
        }
        return buildArticleVO(article);
    }

    /**
     * 管理员修改文章
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @Override
    @Transactional
    public ArticleVO updateAdminArticle(ArticlePublishRequest request) {
        ensureCurrentUserIsAdmin();
        validateArticleRequest(request, true);
        validateAdminArticleCategory(request.getCategory());
        validateArticleCover(request);

        Article article = getById(request.getId());
        if (article == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在");
        }

        User currentUser = getCurrentUser();
        fillArticle(article, request, currentUser);

        boolean updated = updateById(article);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "管理员修改文章失败");
        }
        return buildArticleVO(article);
    }

    /**
     * 管理员删除文章
     *
     * @param articleId 文章 ID
     */
    @Override
    @Transactional
    public void deleteAdminArticle(Long articleId) {
        ensureCurrentUserIsAdmin();
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        boolean removed = removeById(articleId);
        if (!removed) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "管理员删除文章失败");
        }
    }

    /**
     * 校验文章请求参数
     *
     * @param request 请求对象
     * @param isUpdate 是否为修改
     */
    private void validateArticleRequest(ArticlePublishRequest request, boolean isUpdate) {
        if (request == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章参数不能为空");
        }
        if (isUpdate && request.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "修改文章时文章ID不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章内容不能为空");
        }
        if (request.getCategory() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章分类不能为空");
        }
        if (request.getStatus() == null) {
            request.setStatus(ArticleStatus.PUBLISHED);
        }
    }

    /**
     * 校验分页参数
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param nullMessage 空请求提示
     */
    private void validateCommonPageRequest(Long pageNum, Long pageSize, String nullMessage) {
        if (pageNum == null && pageSize == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), nullMessage);
        }
        if (pageNum == null || pageNum < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "页码必须大于等于1");
        }
        if (pageSize == null || pageSize < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "每页条数必须大于等于1");
        }
    }

    /**
     * 校验文章封面信息
     *
     * @param request 请求对象
     */
    private void validateArticleCover(ArticlePublishRequest request) {
        if (request.getCoverFileId() == null && !StringUtils.hasText(request.getCoverImageUrl())) {
            return;
        }
        if (request.getCoverFileId() == null || !StringUtils.hasText(request.getCoverImageUrl())) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "封面文件ID和封面访问路径必须同时提供");
        }

        FileRecord fileRecord = fileRecordMapper.selectById(request.getCoverFileId());
        if (fileRecord == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "封面文件不存在");
        }

        String coverImageUrl = request.getCoverImageUrl().trim();
        if (!coverImageUrl.equals(fileRecord.getUrlPath()) && !coverImageUrl.endsWith(fileRecord.getUrlPath())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "封面文件路径不匹配");
        }
    }

    /**
     * 校验当前用户是否具备指定文章分类的发文权限
     *
     * @param user 当前用户
     * @param category 文章分类
     */
    private void validateArticleCategoryPermission(User user, ArticleCategory category) {
        if (user.getRole() == UserRole.USER && category != ArticleCategory.DEMAND) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "普通用户只能发布需求标签文章");
        }
        if (user.getRole() == UserRole.ENTERPRISE) {
            if (user.getAuditStatus() != UserAuditStatus.APPROVED) {
                throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "企业用户未审核通过，不能发布文章");
            }
            if (category != ArticleCategory.PRODUCT) {
                throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "企业用户只能发布产品标签文章");
            }
        }
        if (user.getRole() == UserRole.ADMIN && category != ArticleCategory.COMPANY) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "管理员只能发布公司标签文章");
        }
    }

    /**
     * 校验管理员文章分类
     *
     * @param category 文章分类
     */
    private void validateAdminArticleCategory(ArticleCategory category) {
        if (category != ArticleCategory.COMPANY) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "管理员只能发布公司标签文章");
        }
    }

    /**
     * 填充文章信息
     *
     * @param article 文章对象
     * @param request 请求对象
     * @param user 当前用户
     */
    private void fillArticle(Article article, ArticlePublishRequest request, User user) {
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent().trim());
        article.setCoverFileId(request.getCoverFileId());
        article.setCoverImageUrl(StringUtils.hasText(request.getCoverImageUrl()) ? request.getCoverImageUrl().trim() : null);
        article.setPreviewImageUrl(StringUtils.hasText(request.getPreviewImageUrl()) ? request.getPreviewImageUrl().trim() : null);
        article.setStatus(request.getStatus());
        article.setCategory(request.getCategory());
        article.setAuthorId(user.getId());
        article.setAuthorName(user.getUsername());
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户
     */
    private User getCurrentUser() {
        User user = userMapper.selectById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }
        return user;
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGGED_IN.getCode(), "当前未登录");
        }
        try {
            return Long.parseLong(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "当前登录状态异常");
        }
    }

    /**
     * 获取当前用户拥有的文章
     *
     * @param articleId 文章 ID
     * @param currentUserId 当前用户 ID
     * @return 文章对象
     */
    private Article getOwnedArticle(Long articleId, Long currentUserId) {
        Article article = getById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在");
        }
        if (!currentUserId.equals(article.getAuthorId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "只能操作自己的文章");
        }
        return article;
    }

    /**
     * 获取已发布文章
     *
     * @param articleId 文章 ID
     * @return 文章对象
     */
    private Article getPublishedArticle(Long articleId) {
        Article article = getById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在");
        }
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "当前文章未发布");
        }
        return article;
    }

    /**
     * 确保当前登录用户为管理员
     */
    private void ensureCurrentUserIsAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅管理员可执行该操作");
        }
    }

    /**
     * 转换文章分页结果
     *
     * @param articlePage 原始分页结果
     * @param truncateContent 是否截断正文
     * @return 转换后的分页结果
     */
    private IPage<ArticleVO> convertArticlePage(Page<Article> articlePage, boolean truncateContent) {
        Page<ArticleVO> resultPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        resultPage.setRecords(articlePage.getRecords().stream()
                .map(article -> buildArticleVO(article, truncateContent))
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 构造文章返回对象
     *
     * @param article 文章对象
     * @return 文章返回对象
     */
    private ArticleVO buildArticleVO(Article article) {
        return buildArticleVO(article, false);
    }

    /**
     * 构造文章返回对象
     *
     * @param article 文章对象
     * @param truncateContent 是否截断正文
     * @return 文章返回对象
     */
    private ArticleVO buildArticleVO(Article article, boolean truncateContent) {
        return ArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(truncateContent ? abbreviateContent(article.getContent()) : article.getContent())
                .coverFileId(article.getCoverFileId())
                .coverImageUrl(article.getCoverImageUrl())
                .previewImageUrl(article.getPreviewImageUrl())
                .authorId(article.getAuthorId())
                .authorName(article.getAuthorName())
                .status(article.getStatus())
                .category(article.getCategory())
                .createTime(article.getCreateTime())
                .updateTime(article.getUpdateTime())
                .build();
    }

    /**
     * 将正文裁剪到前 50 个字符
     *
     * @param content 正文内容
     * @return 裁剪后的内容
     */
    private String abbreviateContent(String content) {
        if (!StringUtils.hasText(content) || content.length() <= 50) {
            return content;
        }
        return content.substring(0, 50) + "...";
    }
}
