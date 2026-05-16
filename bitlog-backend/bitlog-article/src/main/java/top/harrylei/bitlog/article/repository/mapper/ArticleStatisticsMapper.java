package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.article.repository.entity.ArticleStatisticsDO;

/**
 * 文章统计 Mapper
 *
 * @author Harry
 * @since 2026-04-09
 */
@Mapper
public interface ArticleStatisticsMapper extends BaseMapper<ArticleStatisticsDO> {
}
