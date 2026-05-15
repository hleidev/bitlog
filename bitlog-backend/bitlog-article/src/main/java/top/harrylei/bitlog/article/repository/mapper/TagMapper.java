package top.harrylei.bitlog.article.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.article.repository.entity.TagDO;

/**
 * 标签 Mapper
 *
 * @author harry
 * @since 0.0.1
 */
@Mapper
public interface TagMapper extends BaseMapper<TagDO> {
}
