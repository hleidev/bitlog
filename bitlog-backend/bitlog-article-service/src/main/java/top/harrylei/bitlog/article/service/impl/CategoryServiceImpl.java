package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.api.model.article.req.CategorySaveRequest;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.util.List;

/**
 * 分类业务服务实现
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;
    private final ArticleConverter articleConverter;

    @Override
    public List<CategoryVO> listAll() {
        return articleConverter.toCategoryVOList(categoryDAO.listAllOrderByArticleCount());
    }

    @Override
    public Long getOrCreate(String name) {
        if (name == null || name.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("分类名称不能为空");
        }
        String categoryName = name.trim();
        CategoryDO category = categoryDAO.getByName(categoryName);
        if (category != null) {
            return category.getId();
        }
        return createCategory(categoryName, "", 0);
    }

    @Override
    public Long save(CategorySaveRequest req) {
        String name = req.getName().trim();
        if (categoryDAO.getByName(name) != null) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }
        return createCategory(name,
                req.getDescription() != null ? req.getDescription() : "",
                req.getSortOrder() != null ? req.getSortOrder() : 0);
    }

    @Override
    public void update(Long categoryId, CategorySaveRequest req) {
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null) {
            ResultCode.CATEGORY_NOT_EXISTS.throwException();
        }
        String name = req.getName().trim();
        if (!name.equals(category.getName()) && categoryDAO.getByName(name) != null) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }
        category.setName(name);
        if (req.getDescription() != null) {
            category.setDescription(req.getDescription());
        }
        if (req.getSortOrder() != null) {
            category.setSortOrder(req.getSortOrder());
        }
        categoryDAO.updateById(category);
        log.info("更新分类 categoryId={}", categoryId);
    }

    @Override
    public void delete(Long categoryId) {
        if (!categoryDAO.removeById(categoryId)) {
            ResultCode.CATEGORY_NOT_EXISTS.throwException();
        }
        log.info("删除分类 categoryId={}", categoryId);
    }

    private Long createCategory(String name, String description, int sortOrder) {
        CategoryDO category = new CategoryDO()
                .setName(name)
                .setDescription(description)
                .setSortOrder(sortOrder)
                .setArticleCount(0);
        categoryDAO.save(category);
        log.info("创建分类 name={} id={}", name, category.getId());
        return category.getId();
    }
}
