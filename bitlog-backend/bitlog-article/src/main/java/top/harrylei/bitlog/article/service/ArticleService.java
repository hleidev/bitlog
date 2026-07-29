package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.api.model.article.query.ArticlePageParam;
import top.harrylei.bitlog.api.model.article.req.ArticleMetaUpdateParam;
import top.harrylei.bitlog.api.model.article.req.ArticlePublishParam;
import top.harrylei.bitlog.api.model.article.req.ArticleSaveParam;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleListVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVO;
import top.harrylei.bitlog.common.model.PageVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文章业务服务接口
 *
 * @author harry
 * @since 2026-04-09
 */
public interface ArticleService {

    /**
     * 保存文章草稿（仅处理标题和正文，生成版本链）
     *
     * @param userId 作者用户 ID
     * @param req 保存请求
     * @return 文章 ID
     */
    Long saveArticle(Long userId, ArticleSaveParam req);

    /**
     * 更新文章草稿（生成新版本，仅处理标题和正文）
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param req 更新请求
     */
    void updateArticle(Long userId, Long articleId, ArticleSaveParam req);

    /**
     * 发布文章（更新封面、摘要、分类、标签，并将最新版本设为已发布版本）
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param req 发布请求
     */
    void publishArticle(Long userId, Long articleId, ArticlePublishParam req);

    /**
     * 快速更新文章元数据（摘要、分类、标签），不影响文章内容和版本
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param req 元数据更新请求
     */
    void updateArticleMeta(Long userId, Long articleId, ArticleMetaUpdateParam req);

    /**
     * 切换文章状态（草稿 ↔ 已发布）
     * <p>
     * 切换为已发布时使用现有元数据重新上线，不重新填写封面等信息； 切换为草稿时回退计数。两端均幂等。
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param status 目标状态
     */
    void updateStatus(Long userId, Long articleId, ArticleStatusEnum status);

    /**
     * 获取文章详情（已发布版本，供读者阅读）
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    ArticlePublicDetailVO getPublishedDetail(Long articleId);

    /**
     * 获取文章草稿详情（最新版本，供作者编辑）
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @return 文章详情
     */
    ArticleDetailVO getDraftDetail(Long userId, Long articleId);

    /**
     * 获取文章版本历史列表
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @return 版本列表
     */
    List<ArticleVersionVO> listVersions(Long userId, Long articleId);

    /**
     * 回滚到指定版本（以指定版本内容创建新版本）
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param versionId 要回滚的版本 ID
     */
    void rollbackVersion(Long userId, Long articleId, Long versionId);

    /**
     * 获取指定版本详情（含正文，用于版本对比）
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param versionId 版本 ID
     * @return 版本详情
     */
    ArticleVersionDetailVO getVersionDetail(Long userId, Long articleId, Long versionId);

    /**
     * 分页查询已发布文章列表（公开）
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageVO<ArticlePublicVO> pagePublished(ArticlePageParam query);

    /**
     * 批量切换文章状态
     *
     * @param userId 操作用户 ID
     * @param articleIds 文章 ID 列表
     * @param status 目标状态
     */
    void batchUpdateStatus(Long userId, List<Long> articleIds, ArticleStatusEnum status);

    /**
     * 批量删除文章版本（硬删除，不可恢复）
     * <p>
     * 当前最新草稿版本和已发布版本受保护，不允许删除。
     *
     * @param userId 操作用户 ID
     * @param articleId 文章 ID
     * @param versionIds 要删除的版本 ID 列表
     */
    void deleteVersions(Long userId, Long articleId, List<Long> versionIds);

    /**
     * 批量删除文章（软删除）
     *
     * @param userId 操作用户 ID
     * @param articleIds 文章 ID 列表
     */
    void batchDelete(Long userId, List<Long> articleIds);

    /**
     * 分页查询当前用户的文章列表（含草稿），同时返回各状态计数
     *
     * @param userId 用户 ID
     * @param query 查询参数
     * @return 分页结果及状态计数
     */
    ArticleListVO pageMyArticles(Long userId, ArticlePageParam query);

    /**
     * 根据文章 ID 获取文章基础信息（内部 Feign 接口使用）
     *
     * @param articleId 文章 ID
     * @return 文章 DTO，文章未发布时返回 null
     */
    ArticleDTO getArticleDTO(Long articleId);

    /**
     * 批量查询文章基础信息（内部 Feign 接口使用）
     *
     * @param articleIds 文章 ID 列表
     * @return 文章 DTO 列表（过滤未发布及已删除的文章）
     */
    List<ArticleDTO> getArticleDTOBatch(List<Long> articleIds);

    /**
     * 判断文章当前是否可被读者访问，不加载正文
     *
     * @param articleId 文章 ID
     * @return true 文章存在、未删除且处于已发布状态
     */
    boolean isPublished(Long articleId);

    /**
     * 批量查询文章标题，不加载正文，未发布文章回退到最新草稿版本的标题
     *
     * @param articleIds 文章 ID 集合
     * @return 文章 ID 到标题的映射，已删除文章不在结果中
     */
    Map<Long, String> getArticleTitles(Collection<Long> articleIds);
}
