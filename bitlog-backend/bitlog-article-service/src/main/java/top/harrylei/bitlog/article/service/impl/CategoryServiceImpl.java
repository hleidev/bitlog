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
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
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
            if (DeleteStatusEnum.NOT_DELETED.equals(category.getDeleted())) {
                return category.getId();
            }
            // 已删除的同名分类：恢复复用（仅恢复删除标记）
            categoryDAO.restore(category.getId());
            log.info("恢复已删除分类 name={} id={}", categoryName, category.getId());
            return category.getId();
        }
        return createCategory(categoryName, "", 0);
    }

    @Override
    public Long save(CategorySaveRequest req) {
        String name = req.getName().trim();
        CategoryDO category = categoryDAO.getByName(name);
        if (category != null) {
            if (DeleteStatusEnum.NOT_DELETED.equals(category.getDeleted())) {
                ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
            }
            // 已删除的同名分类：恢复并更新字段
            categoryDAO.restoreAndUpdate(category.getId(),
                    req.getDescription() != null ? req.getDescription() : "",
                    req.getSortOrder() != null ? req.getSortOrder() : 0);
            log.info("恢复已删除分类 name={} id={}", name, category.getId());
            return category.getId();
        }
        return createCategory(name,
                req.getDescription() != null ? req.getDescription() : "",
                req.getSortOrder() != null ? req.getSortOrder() : 0);
    }

    @Override
    public void update(Long categoryId, CategorySaveRequest req) {
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null || DeleteStatusEnum.DELETED.equals(category.getDeleted())) {
            ResultCode.CATEGORY_NOT_EXISTS.throwException();
        }
        String name = req.getName().trim();
        if (!name.equals(category.getName())) {
            CategoryDO conflict = categoryDAO.getByName(name);
            if (conflict != null && DeleteStatusEnum.NOT_DELETED.equals(conflict.getDeleted())) {
                ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
            }
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
        CategoryDO category = categoryDAO.getById(categoryId);
        if (category == null || DeleteStatusEnum.DELETED.equals(category.getDeleted())) {
            ResultCode.CATEGORY_NOT_EXISTS.throwException();
        }
        categoryDAO.delete(categoryId);
        log.info("删除分类 categoryId={}", categoryId);
    }

    // 新建分类
    private Long createCategory(String name, String description, int sortOrder) {
        CategoryDO category = new CategoryDO()
                .setName(name)
                .setDescription(description)
                .setSortOrder(sortOrder)
                .setArticleCount(0)
                .setDeleted(DeleteStatusEnum.NOT_DELETED);
        categoryDAO.save(category);
        log.info("创建分类 name={} id={}", name, category.getId());
        return category.getId();
    }
}
