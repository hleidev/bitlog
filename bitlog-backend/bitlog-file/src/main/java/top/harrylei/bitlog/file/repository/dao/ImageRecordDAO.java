package top.harrylei.bitlog.file.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.file.repository.entity.ImageRecordDO;
import top.harrylei.bitlog.file.repository.mapper.ImageRecordMapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 文章内容图片上传记录数据访问对象
 *
 * @author Harry
 * @since 2026-05-20
 */
@Repository
public class ImageRecordDAO extends ServiceImpl<ImageRecordMapper, ImageRecordDO> {

    /** 查询 deleted=0 且上传时间早于 before 的所有 file_key */
    public List<String> getKeysOlderThan(LocalDateTime before) {
        return lambdaQuery().select(ImageRecordDO::getFileKey)
            .eq(ImageRecordDO::getDeleted, DeleteStatusEnum.NOT_DELETED).lt(ImageRecordDO::getCreateTime, before).list()
            .stream().map(ImageRecordDO::getFileKey).toList();
    }

    /** 批量标记为已删除 */
    public void markDeleted(Collection<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }
        lambdaUpdate().in(ImageRecordDO::getFileKey, fileKeys).set(ImageRecordDO::getDeleted, DeleteStatusEnum.DELETED)
            .update();
    }
}
