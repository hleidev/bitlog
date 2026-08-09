package top.harrylei.bitlog.user.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 用户个人信息实体
 *
 * @author Harry
 * @since 2026-03-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
@Accessors(chain = true)
public class UserInfoDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String avatar;

    private String position;

    private String company;

    private String profile;

    private DeleteStatusEnum deleted;
}
