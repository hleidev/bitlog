package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.api.model.article.req.TagSaveParam;
import top.harrylei.bitlog.api.model.article.req.TagUpdateParam;
import top.harrylei.bitlog.api.model.article.vo.TagVO;

import java.util.List;

/**
 * 标签业务服务接口
 *
 * @author harry
 * @since 2026-04-02
 */
public interface TagService {

    /**
     * 查询所有未删除标签，按使用频率降序
     *
     * @param name 标签名称关键字，为空时返回全量
     */
    List<TagVO> listAll(String name);

    /**
     * 查询或创建标签（按名称） 存在则返回已有 ID，不存在则新建后返回新 ID
     */
    Long getOrCreate(String name);

    /** 保存标签 */
    Long save(TagSaveParam req);

    /** 更新标签名称 */
    void update(Long tagId, TagUpdateParam req);

    /**
     * 批量删除标签（软删除）并清理文章关联 任意 ID 不存在则整体回滚
     */
    void batchDelete(List<Long> ids);
}
