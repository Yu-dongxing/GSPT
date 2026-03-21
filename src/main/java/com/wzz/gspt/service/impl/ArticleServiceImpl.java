package com.wzz.gspt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzz.gspt.dto.article.AdminArticleQueryRequest;
import com.wzz.gspt.dto.article.ArticleImageItemRequest;
import com.wzz.gspt.dto.article.ArticleImageWallSaveRequest;
import com.wzz.gspt.dto.article.ArticlePublishRequest;
import com.wzz.gspt.dto.article.MyArticleQueryRequest;
import com.wzz.gspt.dto.article.PublicArticleQueryRequest;
import com.wzz.gspt.enums.ArticleCategory;
import com.wzz.gspt.enums.ArticleStatus;
import com.wzz.gspt.enums.ResultCode;
import com.wzz.gspt.enums.UserAuditStatus;
import com.wzz.gspt.enums.UserRole;
import com.wzz.gspt.exception.BusinessException;
import com.wzz.gspt.mapper.ArticleImageMapper;
import com.wzz.gspt.mapper.ArticleMapper;
import com.wzz.gspt.mapper.FileRecordMapper;
import com.wzz.gspt.mapper.UserMapper;
import com.wzz.gspt.pojo.Article;
import com.wzz.gspt.pojo.ArticleImage;
import com.wzz.gspt.pojo.FileRecord;
import com.wzz.gspt.pojo.User;
import com.wzz.gspt.service.ArticleService;
import com.wzz.gspt.service.FileRecordService;
import com.wzz.gspt.utils.DateTimeRangeUtil;
import com.wzz.gspt.vo.ArticleImageVO;
import com.wzz.gspt.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章服务实现类
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final FileRecordMapper fileRecordMapper;
    private final ArticleImageMapper articleImageMapper;
    private final UserMapper userMapper;
    private final FileRecordService fileRecordService;

    /**
     * 发布新文章
     * @param request 发布参数
     * @return 文章视图对象
     */
    @Override
    @Transactional
    public ArticleVO publishArticle(ArticlePublishRequest request) {
        // 1. 基础校验
        validateArticleRequest(request, false);

        // 2. 权限与分类合法性校验
        User currentUser = getCurrentUser();
        validateArticleCategoryPermission(currentUser, request.getCategory());
        validateArticleCover(request);

        // 3. 填充并保存文章实体
        Article article = new Article();
        fillArticle(article, request, currentUser);

        boolean saved = save(article);
        if (!saved) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "发布文章失败");
        }

        // 4. 处理图片墙
        replaceArticleImageWall(article.getId(), request.getImageWall(), null);
        return buildArticleVO(article);
    }

    /**
     * 更新用户自己的文章
     * @param request 修改参数
     * @return 文章视图对象
     */
    @Override
    @Transactional
    public ArticleVO updateMyArticle(ArticlePublishRequest request) {
        // 1. 基础校验
        validateArticleRequest(request, true);

        // 2. 权限与分类合法性校验
        User currentUser = getCurrentUser();
        validateArticleCategoryPermission(currentUser, request.getCategory());
        validateArticleCover(request);

        // 3. 校验文章归属权
        Article article = getOwnedArticle(request.getId(), currentUser.getId());

        // 4. 记录旧的图片墙文件ID，用于后续清理未引用文件
        List<Long> oldImageWallFileIds = getArticleImageWallFileIds(article.getId());

        // 5. 更新文章信息
        fillArticle(article, request, currentUser);
        boolean updated = updateById(article);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "修改文章失败");
        }

        // 6. 替换图片墙
        if (request.getImageWall() != null) {
            replaceArticleImageWall(article.getId(), request.getImageWall(), oldImageWallFileIds);
        }
        return buildArticleVO(article);
    }

    /**
     * 保存/更新文章图片墙（独立操作接口）
     * @param request 图片墙请求参数
     * @return 文章视图对象
     */
    @Override
    @Transactional
    public ArticleVO saveMyArticleImageWall(ArticleImageWallSaveRequest request) {
        validateArticleImageWallRequest(request);
        Article article = getOwnedArticle(request.getArticleId(), getCurrentUserId());
        replaceArticleImageWall(article.getId(), request.getImages(), getArticleImageWallFileIds(article.getId()));
        return buildArticleVO(article);
    }

    /**
     * 分页查询当前登录用户的文章
     * @param request 查询条件（标题、状态、分类、日期范围等）
     * @return 分页结果
     */
    @Override
    public IPage<ArticleVO> pageMyArticles(MyArticleQueryRequest request) {
        validateCommonPageRequest(request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize(), "查询参数不能为空");

        Long currentUserId = getCurrentUserId();
        var startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        var endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);

        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, currentUserId)
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .eq(request.getStatus() != null, Article::getStatus, request.getStatus())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                .ge(startCreateTime != null, Article::getCreateTime, startCreateTime)
                .le(endCreateTime != null, Article::getCreateTime, endCreateTime)
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = page(page, wrapper);
        return convertArticlePage(articlePage, false);
    }

    /**
     * 获取当前用户自己的文章详情
     * @param articleId 文章ID
     * @return 文章视图对象
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
     * 删除当前用户的文章（级联删除关联图片和物理文件引用）
     * @param articleId 文章ID
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
        // 删除文章关联的文件记录
        deleteArticleFiles(article);
    }

    /**
     * 公开查询：获取已发布文章详情
     * @param articleId 文章ID
     * @return 文章视图对象
     */
    @Override
    public ArticleVO getArticleDetail(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        getCurrentUserId(); // 验证登录
        Article article = getPublishedArticle(articleId);
        return buildArticleVO(article);
    }

    /**
     * 公开查询：分页获取所有已发布的文章
     * @param request 查询参数
     * @return 分页结果（内容会截断）
     */
    @Override
    public IPage<ArticleVO> pagePublicArticles(PublicArticleQueryRequest request) {
        validateCommonPageRequest(request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize(), "公共查询参数不能为空");

        var startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        var endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);

        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED) // 仅限已发布
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .like(StringUtils.hasText(request.getAuthorName()), Article::getAuthorName, request.getAuthorName())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                .ge(startCreateTime != null, Article::getCreateTime, startCreateTime)
                .le(endCreateTime != null, Article::getCreateTime, endCreateTime)
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = page(page, wrapper);
        return convertArticlePage(articlePage, true); // true 表示截断正文预览
    }

    /**
     * 随机获取已发布文章（用于推荐等场景）
     * @param count 获取数量
     * @return 文章列表
     */
    @Override
    public List<ArticleVO> randomPublicArticles(Integer count) {
        if (count == null || count < 1) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "数量必须大于等于1");
        }
        if (count > 100) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "数量不能超过100");
        }

        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .last("ORDER BY RAND() LIMIT " + count));

        return articles.stream()
                .map(article -> buildArticleVO(article, true))
                .collect(Collectors.toList());
    }

    /**
     * 管理后台：分页查询所有文章
     * @param request 查询参数
     * @return 分页结果
     */
    @Override
    public IPage<ArticleVO> pageAdminArticles(AdminArticleQueryRequest request) {
        ensureCurrentUserIsAdmin();
        validateCommonPageRequest(request == null ? null : request.getPageNum(), request == null ? null : request.getPageSize(), "管理员查询参数不能为空");

        var startCreateTime = DateTimeRangeUtil.parseDateTime(request.getStartCreateTime(), false);
        var endCreateTime = DateTimeRangeUtil.parseDateTime(request.getEndCreateTime(), true);

        Page<Article> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .like(StringUtils.hasText(request.getTitle()), Article::getTitle, request.getTitle())
                .like(StringUtils.hasText(request.getAuthorName()), Article::getAuthorName, request.getAuthorName())
                .eq(request.getStatus() != null, Article::getStatus, request.getStatus())
                .eq(request.getCategory() != null, Article::getCategory, request.getCategory())
                .ge(startCreateTime != null, Article::getCreateTime, startCreateTime)
                .le(endCreateTime != null, Article::getCreateTime, endCreateTime)
                .orderByDesc(Article::getCreateTime);

        Page<Article> articlePage = page(page, wrapper);
        return convertArticlePage(articlePage, false);
    }

    /**
     * 管理后台：查看任意文章详情
     * @param articleId 文章ID
     * @return 文章视图对象
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
     * 管理后台：直接发布文章
     * @param request 发布参数
     * @return 文章视图对象
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
        replaceArticleImageWall(article.getId(), request.getImageWall(), null);
        return buildArticleVO(article);
    }

    /**
     * 管理后台：修改文章
     * @param request 修改参数
     * @return 文章视图对象
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
        List<Long> oldImageWallFileIds = getArticleImageWallFileIds(article.getId());
        fillArticle(article, request, currentUser);

        boolean updated = updateById(article);
        if (!updated) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "管理员修改文章失败");
        }
        if (request.getImageWall() != null) {
            replaceArticleImageWall(article.getId(), request.getImageWall(), oldImageWallFileIds);
        }
        return buildArticleVO(article);
    }

    /**
     * 管理后台：保存图片墙
     * @param request 图片墙参数
     * @return 文章视图对象
     */
    @Override
    @Transactional
    public ArticleVO saveAdminArticleImageWall(ArticleImageWallSaveRequest request) {
        ensureCurrentUserIsAdmin();
        validateArticleImageWallRequest(request);
        Article article = getById(request.getArticleId());
        if (article == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在");
        }
        replaceArticleImageWall(article.getId(), request.getImages(), getArticleImageWallFileIds(article.getId()));
        return buildArticleVO(article);
    }

    /**
     * 管理后台：删除文章
     * @param articleId 文章ID
     */
    @Override
    @Transactional
    public void deleteAdminArticle(Long articleId) {
        ensureCurrentUserIsAdmin();
        if (articleId == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        Article article = getById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在");
        }
        boolean removed = removeById(articleId);
        if (!removed) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "管理员删除文章失败");
        }
        deleteArticleFiles(article);
    }

    /**
     * 内部方法：验证文章基础请求参数
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
        validateArticleWallImages(request.getImageWall());
    }

    /**
     * 内部方法：分页参数通用验证
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
     * 内部方法：校验封面图和预览图
     */
    private void validateArticleCover(ArticlePublishRequest request) {
        validateArticleImage(request.getCoverFileId(), request.getCoverImageUrl(), "封面");
        validateArticleImage(request.getPreviewImageId(), request.getPreviewImageUrl(), "预览图");
    }

    /**
     * 内部方法：校验图片墙请求
     */
    private void validateArticleImageWallRequest(ArticleImageWallSaveRequest request) {
        if (request == null || request.getArticleId() == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章ID不能为空");
        }
        validateArticleWallImages(request.getImages());
    }

    /**
     * 内部方法：单张图片关联校验（校验文件是否存在及路径是否匹配）
     */
    private void validateArticleImage(Long fileId, String imageUrl, String imageName) {
        if (fileId == null && !StringUtils.hasText(imageUrl)) {
            return;
        }
        if (fileId == null || !StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), imageName + "文件ID和访问路径必须同时提供");
        }

        FileRecord fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), imageName + "文件不存在");
        }

        String trimmedImageUrl = imageUrl.trim();
        if (!trimmedImageUrl.equals(fileRecord.getUrlPath()) && !trimmedImageUrl.endsWith(fileRecord.getUrlPath())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), imageName + "文件路径不匹配");
        }
        ensureImageFile(fileRecord, imageName);
    }

    /**
     * 内部方法：批量校验图片墙图片
     */
    private void validateArticleWallImages(List<ArticleImageItemRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (images.size() > 50) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "文章图片墙最多支持50张图片");
        }

        Set<Long> distinctFileIds = new HashSet<>();
        for (int i = 0; i < images.size(); i++) {
            ArticleImageItemRequest item = images.get(i);
            if (item == null) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "图片墙第" + (i + 1) + "项不能为空");
            }
            if (item.getFileId() == null || !StringUtils.hasText(item.getImageUrl())) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "图片墙图片文件ID和访问路径必须同时提供");
            }
            if (!distinctFileIds.add(item.getFileId())) {
                throw new BusinessException(ResultCode.PARAM_IS_INVALID.getCode(), "图片墙中存在重复的文件ID:" + item.getFileId());
            }
            validateArticleImage(item.getFileId(), item.getImageUrl(), "图片墙");
        }
    }

    /**
     * 内部方法：确保文件后缀为图片格式
     */
    private void ensureImageFile(FileRecord fileRecord, String imageName) {
        String suffix = fileRecord.getFileSuffix();
        if (!StringUtils.hasText(suffix)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), imageName + "文件类型不正确");
        }
        String normalizedSuffix = suffix.trim().toLowerCase();
        if (!normalizedSuffix.equals(".jpg")
                && !normalizedSuffix.equals(".jpeg")
                && !normalizedSuffix.equals(".png")
                && !normalizedSuffix.equals(".gif")
                && !normalizedSuffix.equals(".webp")) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), imageName + "仅支持图片文件");
        }
    }

    /**
     * 内部方法：根据用户角色校验可发布的文章分类
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
     * 内部方法：管理员发布文章时的分类校验
     */
    private void validateAdminArticleCategory(ArticleCategory category) {
        if (category != ArticleCategory.COMPANY) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "管理员只能发布公司标签文章");
        }
    }

    /**
     * 内部方法：将 Request 数据填充至 Article 实体
     */
    private void fillArticle(Article article, ArticlePublishRequest request, User user) {
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent().trim());
        article.setPreviewContent(StringUtils.hasText(request.getPreviewContent()) ? request.getPreviewContent().trim() : null);
        article.setCoverFileId(request.getCoverFileId());
        article.setCoverImageUrl(StringUtils.hasText(request.getCoverImageUrl()) ? request.getCoverImageUrl().trim() : null);
        article.setPreviewImageId(request.getPreviewImageId());
        article.setPreviewImageUrl(StringUtils.hasText(request.getPreviewImageUrl()) ? request.getPreviewImageUrl().trim() : null);
        article.setStatus(request.getStatus());
        article.setCategory(request.getCategory());
        article.setAuthorId(user.getId());
        article.setAuthorName(user.getUsername());
    }

    /**
     * 内部方法：全量替换图片墙（先删后增，并清理无用的文件引用）
     */
    private void replaceArticleImageWall(Long articleId, List<ArticleImageItemRequest> imageRequests, List<Long> oldFileIds) {
        if (articleId == null) {
            return;
        }

        // 删除旧的关联关系
        deleteArticleImageWallRelations(articleId);

        // 插入新的关联关系
        if (imageRequests != null && !imageRequests.isEmpty()) {
            for (int i = 0; i < imageRequests.size(); i++) {
                ArticleImageItemRequest item = imageRequests.get(i);
                ArticleImage articleImage = new ArticleImage();
                articleImage.setArticleId(articleId);
                articleImage.setFileId(item.getFileId());
                articleImage.setImageUrl(item.getImageUrl().trim());
                articleImage.setSortOrder(item.getSortOrder() == null ? i + 1 : item.getSortOrder());
                articleImageMapper.insert(articleImage);
            }
        }

        // 如果存在旧文件ID，对比新ID列表，删除不再被引用的物理文件
        if (oldFileIds != null && !oldFileIds.isEmpty()) {
            Set<Long> newFileIds = imageRequests == null ? new HashSet<>() : imageRequests.stream()
                    .filter(item -> item != null && item.getFileId() != null)
                    .map(ArticleImageItemRequest::getFileId)
                    .collect(Collectors.toSet());
            List<Long> removedFileIds = oldFileIds.stream()
                    .filter(fileId -> !newFileIds.contains(fileId))
                    .distinct()
                    .collect(Collectors.toList());
            fileRecordService.deleteFilesIfUnreferenced(removedFileIds);
        }
    }

    /**
     * 内部方法：删除文章相关的所有文件引用（封面、预览图、图片墙）
     */
    private void deleteArticleFiles(Article article) {
        List<Long> fileIds = new ArrayList<>();
        if (article.getCoverFileId() != null) {
            fileIds.add(article.getCoverFileId());
        }
        if (article.getPreviewImageId() != null) {
            fileIds.add(article.getPreviewImageId());
        }
        fileIds.addAll(getArticleImageWallFileIds(article.getId()));

        // 删除数据库中的图片墙关联记录
        deleteArticleImageWallRelations(article.getId());

        // 异步或逻辑删除无引用的物理文件
        fileRecordService.deleteFilesIfUnreferenced(fileIds);
    }

    /**
     * 内部方法：仅删除图片墙关联关系
     */
    private void deleteArticleImageWallRelations(Long articleId) {
        if (articleId == null) {
            return;
        }
        articleImageMapper.delete(new LambdaQueryWrapper<ArticleImage>()
                .eq(ArticleImage::getArticleId, articleId));
    }

    /**
     * 内部方法：获取指定文章关联的图片墙文件ID列表
     */
    private List<Long> getArticleImageWallFileIds(Long articleId) {
        if (articleId == null) {
            return new ArrayList<>();
        }
        List<ArticleImage> articleImages = articleImageMapper.selectList(new LambdaQueryWrapper<ArticleImage>()
                .eq(ArticleImage::getArticleId, articleId)
                .select(ArticleImage::getFileId));
        if (articleImages == null || articleImages.isEmpty()) {
            return new ArrayList<>();
        }
        return articleImages.stream()
                .map(ArticleImage::getFileId)
                .filter(fileId -> fileId != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 内部方法：获取当前登录用户的完整信息
     */
    private User getCurrentUser() {
        User user = userMapper.selectById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "当前登录用户不存在");
        }
        return user;
    }

    /**
     * 内部方法：通过 Sa-Token 获取当前登录用户ID
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
     * 内部方法：获取并验证文章是否属于该用户
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
     * 内部方法：获取并验证文章是否已发布
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
     * 内部方法：强制检查当前用户是否为管理员
     */
    private void ensureCurrentUserIsAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅管理员可执行该操作");
        }
    }

    /**
     * 内部方法：将文章分页结果转换为 VO 分页结果
     */
    private IPage<ArticleVO> convertArticlePage(Page<Article> articlePage, boolean truncateContent) {
        Page<ArticleVO> resultPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        resultPage.setRecords(articlePage.getRecords().stream()
                .map(article -> buildArticleVO(article, truncateContent))
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 内部方法：构建文章 VO（默认不截断内容）
     */
    private ArticleVO buildArticleVO(Article article) {
        return buildArticleVO(article, false);
    }

    /**
     * 内部方法：构建文章 VO
     * @param truncateContent 是否需要截断内容（用于列表显示预览）
     */
    private ArticleVO buildArticleVO(Article article, boolean truncateContent) {
        return ArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(truncateContent ? abbreviateContent(article.getContent()) : article.getContent())
                .previewContent(article.getPreviewContent())
                .coverFileId(article.getCoverFileId())
                .coverImageUrl(article.getCoverImageUrl())
                .previewImageId(article.getPreviewImageId())
                .previewImageUrl(article.getPreviewImageUrl())
                .imageWall(buildArticleImageVOs(article.getId()))
                .authorId(article.getAuthorId())
                .authorName(article.getAuthorName())
                .status(article.getStatus())
                .category(article.getCategory())
                .createTime(article.getCreateTime())
                .updateTime(article.getUpdateTime())
                .build();
    }

    /**
     * 内部方法：构建图片墙 VO 列表（带排序）
     */
    private List<ArticleImageVO> buildArticleImageVOs(Long articleId) {
        if (articleId == null) {
            return new ArrayList<>();
        }
        List<ArticleImage> articleImages = articleImageMapper.selectList(new LambdaQueryWrapper<ArticleImage>()
                .eq(ArticleImage::getArticleId, articleId)
                .orderByAsc(ArticleImage::getSortOrder)
                .orderByAsc(ArticleImage::getId));
        if (articleImages == null || articleImages.isEmpty()) {
            return new ArrayList<>();
        }
        return articleImages.stream()
                .sorted(Comparator.comparing(ArticleImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ArticleImage::getId, Comparator.nullsLast(Long::compareTo)))
                .map(articleImage -> ArticleImageVO.builder()
                        .id(articleImage.getId())
                        .fileId(articleImage.getFileId())
                        .imageUrl(articleImage.getImageUrl())
                        .sortOrder(articleImage.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 内部方法：截断正文内容作为预览
     */
    private String abbreviateContent(String content) {
        if (!StringUtils.hasText(content) || content.length() <= 50) {
            return content;
        }
        return content.substring(0, 50) + "...";
    }
}