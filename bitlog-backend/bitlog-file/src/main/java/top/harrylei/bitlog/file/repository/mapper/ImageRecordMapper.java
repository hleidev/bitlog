package top.harrylei.bitlog.file.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.file.repository.entity.ImageRecordDO;

/**
 * 文章内容图片上传记录 Mapper
 *
 * @author Harry
 * @since 2026-05-20
 */
@Mapper
public interface ImageRecordMapper extends BaseMapper<ImageRecordDO> {}
