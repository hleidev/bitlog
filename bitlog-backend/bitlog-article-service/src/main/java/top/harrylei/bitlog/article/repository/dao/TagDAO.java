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
     * 按使用频率降序查询所有未删除标签
     */
    public List<TagDO> listAllOrderByArticleCount() {
        return lambdaQuery()
                .eq(TagDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
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

    public void restore(Long id) {
        lambdaUpdate()
                .eq(TagDO::getId, id)
                .set(TagDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .update();
    }

    public void delete(Long id) {
        lambdaUpdate()
                .eq(TagDO::getId, id)
                .set(TagDO::getDeleted, DeleteStatusEnum.DELETED)
                .update();
    }
}
