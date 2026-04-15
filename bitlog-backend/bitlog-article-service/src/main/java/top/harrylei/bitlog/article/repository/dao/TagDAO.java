package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.repository.mapper.TagMapper;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

import java.util.List;

/**
 * 标签数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class TagDAO extends ServiceImpl<TagMapper, TagDO> {

    /**
     * 根据名称查询标签
     */
    public TagDO getByName(String name) {
        return lambdaQuery()
                .eq(TagDO::getName, name)
                .one();
    }

    /**
     * 根据 ID 列表批量查询标签
     */
    public List<TagDO> listByIds(List<Long> tagIds) {
        return lambdaQuery()
                .in(TagDO::getId, tagIds)
                .eq(TagDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
    }

    /**
     * 按使用频率降序查询所有未删除标签，支持按名称模糊搜索
     *
     * @param name 标签名称关键字，为空时返回全量
     */
    public List<TagDO> listAll(String name) {
        return lambdaQuery()
                .eq(TagDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .like(name != null && !name.isBlank(), TagDO::getName, name)
                .orderByDesc(TagDO::getArticleCount)
                .list();
    }

    /**
     * 批量增加标签文章计数
     */
    public void incrementArticleCount(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        tagIds.forEach(tagId ->
                lambdaUpdate()
                        .eq(TagDO::getId, tagId)
                        .setIncrBy(TagDO::getArticleCount, 1)
                        .update()
        );
    }

    /**
     * 批量减少标签文章计数（最小为 0）
     */
    public void decrementArticleCount(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        tagIds.forEach(tagId ->
                lambdaUpdate()
                        .eq(TagDO::getId, tagId)
                        .setSql("article_count = GREATEST(article_count - 1, 0)")
                        .update()
        );
    }

    public void delete(Long id) {
        updateDeletedStatus(id, DeleteStatusEnum.DELETED);
    }

    public void batchDelete(List<Long> ids) {
        lambdaUpdate()
                .in(TagDO::getId, ids)
                .set(TagDO::getDeleted, DeleteStatusEnum.DELETED)
                .update();
    }

    public void restore(Long id) {
        updateDeletedStatus(id, DeleteStatusEnum.NOT_DELETED);
    }

    private void updateDeletedStatus(Long id, DeleteStatusEnum status) {
        lambdaUpdate()
                .eq(TagDO::getId, id)
                .set(TagDO::getDeleted, status)
                .update();
    }
}
