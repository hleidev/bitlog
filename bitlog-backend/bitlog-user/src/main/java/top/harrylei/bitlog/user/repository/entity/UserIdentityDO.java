package top.harrylei.bitlog.user.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 第三方身份关联实体
 *
 * @author Harry
 * @since 2026-07-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_identity")
@Accessors(chain = true)
public class UserIdentityDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String provider;

    /** 第三方平台的用户唯一标识，Google 为 ID Token 里的 sub */
    private String providerUserId;
}
