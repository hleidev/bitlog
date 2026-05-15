package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.model.article.req.CategoryCreateRequest;
import top.harrylei.bitlog.api.model.article.req.CategoryUpdateRequest;
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
 * 
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;
    private final ArticleConverter articleConverter;

    @Override
    public List<CategoryVO> listAll(String name) {
        return articleConverter.toCategoryVOList(categoryDAO.listAll(name));
    }

    @Transactional
    @Override
    public Long getOrCreate(String name) {
        if (name == null || name.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("分类名称不能为空");
        }
        String trimmed = name.trim();
        CategoryDO existing = categoryDAO.getByName(trimmed);
        if (existing != null) {
            return existing.getId();
        }
        return createCategory(trimmed);
    }

    @Override
    public Long save(CategoryCreateRequest req) {
        String name = req.getName().trim();
        if (categoryDAO.getByName(name) != null) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }
        return createCategory(name);
    }

    private Long createCategory(String name) {
        CategoryDO category = new CategoryDO().setName(name).setArticleCount(0);
        categoryDAO.save(category);
        log.info("创建分类 name={} id={}", name, category.getId());
        return category.getId();
    }

    @Override
    public void update(Long categoryId, CategoryUpdateRequest req) {
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null)
            ResultCode.CATEGORY_NOT_EXISTS.throwException();

        String name = req.getName().trim();
        if (name.equals(category.getName()))
            return;

        CategoryDO conflict = categoryDAO.getByName(name);
        if (conflict != null && !conflict.getId().equals(categoryId)) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }

        categoryDAO.updateById(category.setName(name));
        log.info("更新分类 categoryId={} name={}", categoryId, name);
    }

    @Override
    public void delete(Long categoryId) {
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null) {
            ResultCode.CATEGORY_NOT_EXISTS.throwException();
        }
        if (category.getArticleCount() > 0) {
            ResultCode.CATEGORY_HAS_ARTICLES.throwException();
        }
        categoryDAO.removeById(categoryId);
        log.info("删除分类 categoryId={}", categoryId);
    }
}
