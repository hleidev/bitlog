package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.api.model.article.req.TagSaveRequest;
import top.harrylei.bitlog.api.model.article.req.TagUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.util.List;

/**
 * 标签业务服务实现
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagDAO tagDAO;
    private final ArticleConverter articleConverter;

    @Override
    public List<TagVO> listAll() {
        return articleConverter.toTagVOList(tagDAO.listAllOrderByArticleCount());
    }

    @Override
    public Long getOrCreate(String name) {
        if (name == null || name.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("标签名称不能为空");
        }
        String trimmed = name.trim();
        TagDO existing = tagDAO.getByName(trimmed);
        if (existing != null) {
            if (DeleteStatusEnum.NOT_DELETED.equals(existing.getDeleted())) {
                return existing.getId();
            }
            return restoreTag(existing);
        }
        return createTag(trimmed);
    }

    @Override
    public Long save(TagSaveRequest req) {
        String name = req.getName().trim();
        TagDO existing = tagDAO.getByName(name);
        if (existing != null) {
            if (DeleteStatusEnum.NOT_DELETED.equals(existing.getDeleted())) {
                ResultCode.TAG_ALREADY_EXISTS.throwException(name);
            }
            return restoreTag(existing);
        }
        return createTag(name);
    }

    @Override
    public void update(Long tagId, TagUpdateRequest req) {
        TagDO tag = tagDAO.getById(tagId);
        if (tag == null || DeleteStatusEnum.DELETED.equals(tag.getDeleted())) {
            ResultCode.TAG_NOT_EXISTS.throwException();
        }
        String name = req.getName().trim();
        if (name.equals(tag.getName())) {
            return;
        }
        TagDO conflict = tagDAO.getByName(name);
        if (conflict != null && DeleteStatusEnum.NOT_DELETED.equals(conflict.getDeleted())) {
            ResultCode.TAG_ALREADY_EXISTS.throwException(name);
        }
        tag.setName(name);
        tagDAO.updateById(tag);
        log.info("更新标签 tagId={} name={}", tagId, name);
    }

    // 恢复已删除的同名标签
    private Long restoreTag(TagDO tag) {
        tagDAO.restore(tag.getId());
        log.info("恢复已删除标签 name={} id={}", tag.getName(), tag.getId());
        return tag.getId();
    }

    // 新建标签
    private Long createTag(String name) {
        TagDO tag = new TagDO()
                .setName(name)
                .setArticleCount(0)
                .setDeleted(DeleteStatusEnum.NOT_DELETED);
        tagDAO.save(tag);
        log.info("创建标签 name={} id={}", name, tag.getId());
        return tag.getId();
    }

    @Override
    public void delete(Long tagId) {
        TagDO tag = tagDAO.getById(tagId);
        if (tag == null || DeleteStatusEnum.DELETED.equals(tag.getDeleted())) {
            ResultCode.TAG_NOT_EXISTS.throwException();
        }
        tagDAO.delete(tagId);
        log.info("删除标签 tagId={}", tagId);
    }
}