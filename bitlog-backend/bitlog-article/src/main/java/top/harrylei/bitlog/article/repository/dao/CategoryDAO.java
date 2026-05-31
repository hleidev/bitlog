package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.repository.mapper.CategoryMapper;

import java.util.Collection;
import java.util.List;

/**
 * 文章分类数据访问对象
 *
 * @author Harry
 * @since 2026-04-02
 */
@Repository
public class CategoryDAO extends ServiceImpl<CategoryMapper, CategoryDO> {

    public CategoryDO getByName(String name) {
        return lambdaQuery().eq(CategoryDO::getName, name).one();
    }

    public List<CategoryDO> listAll(String name) {
        return lambdaQuery().like(StringUtils.hasText(name), CategoryDO::getName, name).list();
    }

}
