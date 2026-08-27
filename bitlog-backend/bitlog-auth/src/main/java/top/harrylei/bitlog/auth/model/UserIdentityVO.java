package top.harrylei.bitlog.auth.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 第三方身份绑定展示对象
 *
 * @author Harry
 * @since 2026-08-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "第三方身份绑定")
public class UserIdentityVO {

    @Schema(description = "平台标识", example = "google")
    private String provider;

    /** 与本站登录邮箱无从属关系，展示出来用户才知道绑的究竟是哪个第三方账号 */
    @Schema(description = "第三方平台侧邮箱", example = "someone@gmail.com")
    private String providerEmail;
}
