package top.harrylei.bitlog.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.article.model.req.CategoryCreateParam;
import top.harrylei.bitlog.article.model.req.CategoryUpdateParam;
import top.harrylei.bitlog.article.model.vo.CategoryVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类业务服务实现
 *
 * @author Harry
 * @since 2026-04-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryDAO categoryDAO;
    private final ArticleDAO articleDAO;
    private final ArticleConverter articleConverter;

    @Override
    public List<CategoryVO> listAll(String name) {
        List<CategoryDO> categories = categoryDAO.listAll(name);
        if (categories.isEmpty())
            return List.of();

        List<Long> categoryIds = categories.stream().map(CategoryDO::getId).toList();
        Map<Long, Long> countByCategory = articleDAO.listPublishedByCategoryIds(categoryIds).stream()
            .collect(Collectors.groupingBy(ArticleDO::getCategoryId, Collectors.counting()));

        return categories.stream()
            .map(cat -> articleConverter.toCategoryVO(cat)
                .setArticleCount(countByCategory.getOrDefault(cat.getId(), 0L).intValue()))
            .sorted(Comparator.comparingInt(CategoryVO::getArticleCount).reversed()).toList();
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
    public Long save(CategoryCreateParam req) {
        String name = req.getName().trim();
        if (categoryDAO.getByName(name) != null) {
            ResultCode.CATEGORY_ALREADY_EXISTS.throwException(name);
        }
        return createCategory(name);
    }

    private Long createCategory(String name) {
        CategoryDO category = new CategoryDO().setName(name);
        categoryDAO.save(category);
        log.info("创建分类 name={} id={}", name, category.getId());
        return category.getId();
    }

    @Override
    public void update(Long categoryId, CategoryUpdateParam req) {
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
        if (articleDAO.existsPublishedByCategory(categoryId)) {
            ResultCode.CATEGORY_HAS_ARTICLES.throwException();
        }
        categoryDAO.removeById(categoryId);
        log.info("删除分类 categoryId={}", categoryId);
    }
}
