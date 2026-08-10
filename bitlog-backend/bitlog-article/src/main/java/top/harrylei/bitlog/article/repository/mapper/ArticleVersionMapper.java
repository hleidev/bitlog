package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;


/**
 * 文章版本 Mapper
 *
 * @author Harry
 * @since 2026-04-09
 */
@Mapper
public interface ArticleVersionMapper extends BaseMapper<ArticleVersionDO> {

    /**
     * 查询文章的最大版本号，不存在时返回 0
     *
     * @param articleId 文章 ID
     * @return 最大版本号
     */
    int getMaxVersion(@Param("articleId") Long articleId);

    /**
     * 逐行扫描所有未删除文章的版本内容，结果不落集合
     *
     * @param handler 逐行回调
     */
    void scanContentFromActiveArticles(ResultHandler<String> handler);
}
