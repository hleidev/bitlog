package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.article.model.vo.TagVO;
import top.harrylei.bitlog.article.repository.entity.TagDO;

import java.util.List;

/**
 * 标签 Mapper
 *
 * @author Harry
 * @since 2026-04-09
 */
@Mapper
public interface TagMapper extends BaseMapper<TagDO> {

    List<TagVO> listAllWithCount(@Param("name") String name);

    List<TagVO> listByArticleId(@Param("articleId") Long articleId);
}
