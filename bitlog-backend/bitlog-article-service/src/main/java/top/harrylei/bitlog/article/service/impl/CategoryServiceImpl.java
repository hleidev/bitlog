package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    @Override
    public Long save(CategoryCreateRequest req) {
        long parentId = req.getParentId() != null ? req.getParentId() : 0L;
        String name = req.getName().trim();

        // 校验父分类：必须存在且本身是顶级分类（最多两级）
        if (parentId != 0L) {
            CategoryDO parent = categoryDAO.getById(parentId);
            if (parent == null || parent.getParentId() != 0L) {
                ResultCode.CATEGORY_PARENT_INVALID.throwException();
            }
        }

        // 同级名称唯一性校验
        if (categoryDAO.getByName(name, parentId) != null) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }

        CategoryDO category = new CategoryDO()
                .setParentId(parentId)
                .setName(name)
                .setArticleCount(0);
        categoryDAO.save(category);
        log.info("创建分类 name={} parentId={} id={}", name, parentId, category.getId());
        return category.getId();
    }

    @Override
    public void update(Long categoryId, CategoryUpdateRequest req) {
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null) ResultCode.CATEGORY_NOT_EXISTS.throwException();

        String name = req.getName().trim();
        long newParentId = req.getParentId() != null ? req.getParentId() : category.getParentId();

        if (name.equals(category.getName()) && newParentId == category.getParentId()) return;

        if (newParentId != category.getParentId()) {
            if (category.getParentId() == 0L && categoryDAO.hasChildren(categoryId)) {
                ResultCode.CATEGORY_CANNOT_MOVE.throwException();
            }
            if (newParentId != 0L) {
                CategoryDO parent = categoryDAO.getById(newParentId);
                if (parent == null || parent.getParentId() != 0L) ResultCode.CATEGORY_PARENT_INVALID.throwException();
            }
        }

        CategoryDO conflict = categoryDAO.getByName(name, newParentId);
        if (conflict != null && !conflict.getId().equals(categoryId)) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }

        categoryDAO.updateById(category.setName(name).setParentId(newParentId));
        log.info("更新分类 categoryId={} name={} parentId={}", categoryId, name, newParentId);
    }

    @Override
    public void delete(Long categoryId) {
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null) {
            ResultCode.CATEGORY_NOT_EXISTS.throwException();
        }
        if (categoryDAO.hasChildren(categoryId)) {
            ResultCode.CATEGORY_HAS_CHILDREN.throwException();
        }
        if (category.getArticleCount() > 0) {
            ResultCode.CATEGORY_HAS_ARTICLES.throwException();
        }
        categoryDAO.removeById(categoryId);
        log.info("删除分类 categoryId={}", categoryId);
    }
}
