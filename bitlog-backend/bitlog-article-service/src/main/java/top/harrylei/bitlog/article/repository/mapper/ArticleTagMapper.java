package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.article.repository.entity.ArticleTagDO;

/**
 * 文章标签关联 Mapper
 *
 * @author harry
 * @since 0.0.1
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTagDO> {
}
