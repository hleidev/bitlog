package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.api.model.article.req.CategoryCreateRequest;
import top.harrylei.bitlog.api.model.article.req.CategoryUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;

import java.util.List;

/**
 * 分类业务服务接口
 *
 * @author harry
 * @since 0.0.1
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
    Long save(CategoryCreateRequest req);

    /**
     * 更新分类名称
     */
    void update(Long categoryId, CategoryUpdateRequest req);

    /**
     * 删除分类（有子分类或有文章时拒绝）
     */
    void delete(Long categoryId);
}
