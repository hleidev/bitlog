package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.model.article.req.TagSaveParam;
import top.harrylei.bitlog.api.model.article.req.TagUpdateParam;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.ArticleTagDAO;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.util.List;

/**
 * 标签业务服务实现
 *
 * @author Harry
 * @since 2026-04-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagDAO tagDAO;
    private final ArticleTagDAO articleTagDAO;
    private final ArticleConverter articleConverter;

    @Override
    public List<TagVO> listAll(String name) {
        return articleConverter.toTagVOList(tagDAO.listAll(name));
    }

    @Transactional
    @Override
    public Long getOrCreate(String name) {
        if (name == null || name.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("标签名称不能为空");
        }
        String trimmed = name.trim();
        TagDO existing = tagDAO.getByName(trimmed);
        if (existing != null) {
            return existing.getId();
        }
        return createTag(trimmed);
    }

    @Override
    public Long save(TagSaveParam req) {
        String name = req.getName().trim();
        if (tagDAO.getByName(name) != null) {
            ResultCode.TAG_ALREADY_EXISTS.throwException(name);
        }
        return createTag(name);
    }

    @Override
    public void update(Long tagId, TagUpdateParam req) {
        TagDO tag = tagDAO.getById(tagId);
        if (tag == null) {
            ResultCode.TAG_NOT_EXISTS.throwException();
        }
        String name = req.getName().trim();
        if (name.equals(tag.getName())) {
            return;
        }
        if (tagDAO.getByName(name) != null) {
            ResultCode.TAG_ALREADY_EXISTS.throwException(name);
        }
        tag.setName(name);
        tagDAO.updateById(tag);
        log.info("更新标签 tagId={} name={}", tagId, name);
    }

    @Transactional
    @Override
    public void batchDelete(List<Long> ids) {
        tagDAO.removeByIds(ids);
        articleTagDAO.removeByTagIds(ids);
        log.info("批量删除标签 ids={}", ids);
    }

    private Long createTag(String name) {
        TagDO tag = new TagDO().setName(name).setArticleCount(0);
        tagDAO.save(tag);
        log.info("创建标签 name={} id={}", name, tag.getId());
        return tag.getId();
    }
}
