package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.repository.mapper.CategoryMapper;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

import java.util.Collection;
import java.util.List;

/**
 * 文章分类数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class CategoryDAO extends ServiceImpl<CategoryMapper, CategoryDO> {

    /** 根据 ID 查询未删除分类 */
    public CategoryDO getByIdAndNotDeleted(Long id) {
        return lambdaQuery()
                .eq(CategoryDO::getId, id)
                .eq(CategoryDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    /** 批量查询未删除分类 */
    public List<CategoryDO> listByIdAndNotDeleted(Collection<Long> ids) {
        return lambdaQuery()
                .in(CategoryDO::getId, ids)
                .eq(CategoryDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
    }

    /** 根据名称查询分类 */
    public CategoryDO getByName(String name) {
        return lambdaQuery()
                .eq(CategoryDO::getName, name)
                .one();
    }

    /** 按使用频率降序查询所有未删除分类 */
    public List<CategoryDO> listAllOrderByArticleCount() {
        return lambdaQuery()
                .eq(CategoryDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .orderByDesc(CategoryDO::getArticleCount)
                .list();
    }

    /** 增加分类文章计数 */
    public void incrementArticleCount(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        lambdaUpdate()
                .eq(CategoryDO::getId, categoryId)
                .setIncrBy(CategoryDO::getArticleCount, 1)
                .update();
    }

    /** 恢复已删除分类 */
    public void restore(Long id) {
        lambdaUpdate()
                .eq(CategoryDO::getId, id)
                .set(CategoryDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .update();
    }

    /** 恢复已删除分类并更新字段 */
    public void restoreAndUpdate(Long id, String description, int sortOrder) {
        lambdaUpdate()
                .eq(CategoryDO::getId, id)
                .set(CategoryDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .set(CategoryDO::getDescription, description)
                .set(CategoryDO::getSortOrder, sortOrder)
                .update();
    }

    /** 软删除分类 */
    public void delete(Long id) {
        lambdaUpdate()
                .eq(CategoryDO::getId, id)
                .set(CategoryDO::getDeleted, DeleteStatusEnum.DELETED)
                .update();
    }

    /** 减少分类文章计数 */
    public void decrementArticleCount(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        lambdaUpdate()
            .eq(CategoryDO::getId, categoryId)
            .setSql("article_count = GREATEST(article_count - 1, 0)")
            .update();
    }
}
