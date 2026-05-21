package top.harrylei.bitlog.file.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 文章内容图片上传记录实体
 *
 * @author Harry
 * @since 2026-05-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("image_record")
public class ImageRecordDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 上传用户 ID
     */
    private Long userId;

    /**
     * 文件存储 Key
     */
    private String fileKey;

    /**
     * 删除标记
     */
    private DeleteStatusEnum deleted;
}
