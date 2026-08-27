package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.article.model.req.CategoryCreateParam;
import top.harrylei.bitlog.article.model.req.CategoryUpdateParam;
import top.harrylei.bitlog.article.model.vo.CategoryVO;

import java.util.List;

/**
 * 分类业务服务接口
 *
 * @author harry
 * @since 2026-04-02
 */
public interface CategoryService {

    /**
     * 查询所有分类，支持按名称模糊搜索，按文章数降序
     */
    List<CategoryVO> listAll(String name);

    /**
     * 查询或创建顶级分类（存在则返回 ID，不存在则新建后返回 ID）
     */
    Long getOrCreate(String name);

    /**
     * 创建分类
     */
    Long save(CategoryCreateParam req);

    /**
     * 更新分类名称
     */
    void update(Long categoryId, CategoryUpdateParam req);

    /**
     * 删除分类（有子分类或有文章时拒绝）
     */
    void delete(Long categoryId);
}
