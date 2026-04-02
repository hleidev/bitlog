package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.api.model.article.req.TagSaveRequest;
import top.harrylei.bitlog.api.model.article.req.TagUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.TagVO;

import java.util.List;

/**
 * 标签业务服务接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface TagService {

    /** 查询所有标签，按使用频率降序 */
    List<TagVO> listAll();

    /**
     * 查询或创建标签（按名称）
     * 存在则返回已有 ID，不存在则新建后返回新 ID
     */
    Long getOrCreate(String name);

    /** 保存标签 */
    Long save(TagSaveRequest req);

    /** 更新标签名称 */
    void update(Long tagId, TagUpdateRequest req);

    /** 删除标签（软删除） */
    void delete(Long tagId);
}
