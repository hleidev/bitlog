package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.repository.mapper.TagMapper;

import java.util.List;

/**
 * 标签数据访问对象
 *
 * @author Harry
 * @since 2026-04-02
 */
@Repository
public class TagDAO extends ServiceImpl<TagMapper, TagDO> {

    public TagDO getByName(String name) {
        return lambdaQuery().eq(TagDO::getName, name).one();
    }

    public List<TagDO> listByIds(List<Long> tagIds) {
        return lambdaQuery().in(TagDO::getId, tagIds).list();
    }

    /**
     * 查询所有标签，按已发布文章数降序，支持名称模糊搜索，articleCount 实时计算
     */
    public List<TagVO> listAll(String name) {
        return getBaseMapper().listAllWithCount(name);
    }

}
