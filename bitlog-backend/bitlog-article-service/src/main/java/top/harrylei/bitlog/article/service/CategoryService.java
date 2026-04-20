package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.api.model.article.req.CategorySaveRequest;
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
     * 查询所有分类，按使用频率降序
     */
    List<CategoryVO> listAll();

    /**
     * 查询或创建分类（按名称）
     * 存在则返回已有 ID，不存在则新建后返回新 ID
     */
    Long getOrCreate(String name);

    /**
     * 保存分类
     */
    Long save(CategorySaveRequest req);

    /**
     * 更新分类
     */
    void update(Long categoryId, CategorySaveRequest req);

    /**
     * 删除分类
     */
    void delete(Long categoryId);
}
