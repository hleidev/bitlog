package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.api.model.article.query.ArticlePageQuery;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;

/**
 * 文章主表 Mapper
 *
 * @author Harry
 * @since 2026-04-09
 */
@Mapper
public interface ArticleMapper extends BaseMapper<ArticleDO> {

    IPage<ArticleDO> pagePublished(IPage<ArticleDO> page, @Param("query") ArticlePageQuery query);
}
