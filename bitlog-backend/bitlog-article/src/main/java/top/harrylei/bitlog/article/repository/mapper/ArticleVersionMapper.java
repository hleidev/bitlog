package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;

/**
 * 文章版本 Mapper
 *
 * @author harry
 * @since 0.0.1
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
}
