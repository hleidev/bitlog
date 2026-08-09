package top.harrylei.bitlog.auth.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.auth.repository.entity.UserIdentityDO;
import top.harrylei.bitlog.auth.repository.mapper.UserIdentityMapper;

import java.util.List;

/**
 * 第三方身份关联数据访问对象
 *
 * @author Harry
 * @since 2026-07-31
 */
@Repository
public class UserIdentityDAO extends ServiceImpl<UserIdentityMapper, UserIdentityDO> {

    public UserIdentityDO getByProvider(String provider, String providerUserId) {
        return lambdaQuery().eq(UserIdentityDO::getProvider, provider)
            .eq(UserIdentityDO::getProviderUserId, providerUserId).one();
    }

    public List<UserIdentityDO> listByUserId(Long userId) {
        return lambdaQuery().eq(UserIdentityDO::getUserId, userId).list();
    }

    public boolean existsByUserAndProvider(Long userId, String provider) {
        return lambdaQuery().eq(UserIdentityDO::getUserId, userId).eq(UserIdentityDO::getProvider, provider).exists();
    }

    public void bind(Long userId, String provider, String providerUserId, String providerEmail) {
        save(new UserIdentityDO().setUserId(userId).setProvider(provider).setProviderUserId(providerUserId)
            .setProviderEmail(providerEmail));
    }

    public boolean unbind(Long userId, String provider) {
        return lambdaUpdate().eq(UserIdentityDO::getUserId, userId).eq(UserIdentityDO::getProvider, provider).remove();
    }

    /** 注销时物理删除：不删则 uk_provider_uid 仍占位，同一第三方账号将永远无法重新注册 */
    public void removeByUserId(Long userId) {
        lambdaUpdate().eq(UserIdentityDO::getUserId, userId).remove();
    }
}
