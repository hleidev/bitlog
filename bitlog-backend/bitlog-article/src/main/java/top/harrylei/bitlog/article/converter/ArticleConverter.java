package top.harrylei.bitlog.article.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVO;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.repository.entity.TagDO;

import java.util.List;

/**
 * 文章对象转换器
 *
 * @author Harry
 * @since 2026-04-09
 */
@Mapper(componentModel = "spring")
public interface ArticleConverter {

    /**
     * ArticleDO + ArticleVersionDO → ArticleVO（不含正文，统计数据另外填充）
     */
    @Mapping(target = "id", source = "version.articleId")
    @Mapping(target = "userId", source = "article.userId")
    @Mapping(target = "categoryId", source = "article.categoryId")
    @Mapping(target = "latestVersionId", source = "article.latestVersionId")
    @Mapping(target = "publishedVersionId", source = "article.publishedVersionId")
    @Mapping(target = "createTime", source = "article.createTime")
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tagIds", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "publishTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ArticleVO toVO(ArticleDO article, ArticleVersionDO version);

    /**
     * ArticleDO + ArticleVersionDO → ArticleDetailVO（含正文，统计数据另外填充）
     */
    @Mapping(target = "id", source = "version.articleId")
    @Mapping(target = "versionId", source = "version.id")
    @Mapping(target = "userId", source = "article.userId")
    @Mapping(target = "categoryId", source = "article.categoryId")
    @Mapping(target = "latestVersionId", source = "article.latestVersionId")
    @Mapping(target = "publishedVersionId", source = "article.publishedVersionId")
    @Mapping(target = "createTime", source = "article.createTime")
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tagIds", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "publishTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ArticleDetailVO toDetailVO(ArticleDO article, ArticleVersionDO version);

    /**
     * ArticleVersionDO → ArticleVersionVO（版本历史列表条目，latest 由服务层填充）
     */
    @Mapping(target = "latest", ignore = true)
    ArticleVersionVO toVersionVO(ArticleVersionDO version);

    /**
     * List<ArticleVersionDO> → List<ArticleVersionVO>
     */
    List<ArticleVersionVO> toVersionVOList(List<ArticleVersionDO> list);

    /**
     * ArticleVersionDO → ArticleVersionDetailVO（含正文，用于版本对比）
     */
    ArticleVersionDetailVO toVersionDetailVO(ArticleVersionDO version);

    /**
     * ArticleDO + ArticleVersionDO → ArticlePublicVO（公开列表，publishTime/category/tags 由服务层填充）
     */
    @Mapping(target = "id", source = "version.articleId")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "publishTime", ignore = true)
    ArticlePublicVO toPublicVO(ArticleDO article, ArticleVersionDO version);

    /**
     * ArticleDO + ArticleVersionDO → ArticlePublicDetailVO（公开详情，publishTime/category/tags 由服务层填充）
     */
    @Mapping(target = "id", source = "version.articleId")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "publishTime", ignore = true)
    ArticlePublicDetailVO toPublicDetailVO(ArticleDO article, ArticleVersionDO version);

    TagVO toTagVO(TagDO tag);

    List<TagVO> toTagVOList(List<TagDO> tags);

    CategoryVO toCategoryVO(CategoryDO category);

    List<CategoryVO> toCategoryVOList(List<CategoryDO> categories);

    /**
     * ArticleDO + ArticleVersionDO → ArticleDTO（内部 Feign 接口使用）
     */
    @Mapping(target = "id", source = "version.articleId")
    @Mapping(target = "userId", source = "article.userId")
    @Mapping(target = "publishTime", ignore = true)
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    ArticleDTO toDTO(ArticleDO article, ArticleVersionDO version);
}
