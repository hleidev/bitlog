package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.api.model.article.dto.ArticleCountDTO;
import top.harrylei.bitlog.api.model.article.query.ArticlePageParam;
import top.harrylei.bitlog.api.model.article.query.MyArticlePageParam;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;

/**
 * 文章主表 Mapper
 *
 * @author Harry
 * @since 2026-04-09
 */
@Mapper
public interface ArticleMapper extends BaseMapper<ArticleDO> {

    IPage<ArticleDO> pagePublished(IPage<ArticleDO> page, @Param("query") ArticlePageParam query);

    IPage<ArticleDO> pageByUser(IPage<ArticleDO> page, @Param("userId") Long userId,
        @Param("query") MyArticlePageParam query);

    /**
     * 与 pageByUser 共用 myArticleFilter 片段，保证计数与列表行数始终一致
     */
    ArticleCountDTO selectMyArticleStats(@Param("userId") Long userId, @Param("query") MyArticlePageParam query);
}
