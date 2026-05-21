package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;

import java.util.List;

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
     * 查询所有未删除文章的版本内容
     *
     * @return 内容列表
     */
    List<String> listAllContentFromActiveArticles();
}
