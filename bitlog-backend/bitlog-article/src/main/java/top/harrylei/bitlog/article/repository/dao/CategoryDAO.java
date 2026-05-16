package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.repository.mapper.CategoryMapper;

import java.util.List;
import java.util.Map;

/**
 * 文章分类数据访问对象
 *
 * @author Harry
 * @since 2026-04-02
 */
@Repository
public class CategoryDAO extends ServiceImpl<CategoryMapper, CategoryDO> {

    /**
     * 根据名称查询分类（用于全局唯一性校验）
     */
    public CategoryDO getByName(String name) {
        return lambdaQuery().eq(CategoryDO::getName, name).one();
    }

    /**
     * 查询所有分类，支持按名称模糊搜索，按文章数降序
     */
    public List<CategoryDO> listAll(String name) {
        return lambdaQuery().like(StringUtils.hasText(name), CategoryDO::getName, name)
                .orderByDesc(CategoryDO::getArticleCount).list();
    }

    /**
     * 增加分类文章计数
     */
    public void incrementArticleCount(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        lambdaUpdate().eq(CategoryDO::getId, categoryId).setIncrBy(CategoryDO::getArticleCount, 1).update();
    }

    /**
     * 减少分类文章计数（最小为 0）
     */
    public void decrementArticleCount(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        lambdaUpdate().eq(CategoryDO::getId, categoryId).setSql("article_count = GREATEST(article_count - 1, 0)")
                .update();
    }

    /**
     * 按指定数量批量减少分类文章计数（批量删除场景）
     */
    public void decrementArticleCountBatch(Map<Long, Long> categoryIdCountMap) {
        if (categoryIdCountMap == null || categoryIdCountMap.isEmpty()) {
            return;
        }
        categoryIdCountMap.forEach((categoryId, count) -> lambdaUpdate().eq(CategoryDO::getId, categoryId)
                .setSql("article_count = GREATEST(article_count - {0}, 0)", count).update());
    }

}
