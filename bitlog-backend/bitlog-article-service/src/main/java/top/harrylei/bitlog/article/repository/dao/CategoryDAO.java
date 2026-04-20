package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.repository.mapper.CategoryMapper;

import java.util.List;

/**
 * 文章分类数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class CategoryDAO extends ServiceImpl<CategoryMapper, CategoryDO> {

    /**
     * 根据名称查询分类
     */
    public CategoryDO getByName(String name) {
        return lambdaQuery()
                .eq(CategoryDO::getName, name)
                .one();
    }

    /**
     * 按使用频率降序查询所有分类
     */
    public List<CategoryDO> listAllOrderByArticleCount() {
        return lambdaQuery()
                .orderByDesc(CategoryDO::getArticleCount)
                .list();
    }

    /**
     * 增加分类文章计数
     */
    public void incrementArticleCount(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        lambdaUpdate()
                .eq(CategoryDO::getId, categoryId)
                .setIncrBy(CategoryDO::getArticleCount, 1)
                .update();
    }

    /**
     * 减少分类文章计数（最小为 0）
     */
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
