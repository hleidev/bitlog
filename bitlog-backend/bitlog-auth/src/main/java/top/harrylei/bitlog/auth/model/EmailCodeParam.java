package top.harrylei.bitlog.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改邮箱的验证码发送请求参数
 *
 * @author Harry
 * @since 2026-08-02
 */
@Data
public class EmailCodeParam {

    /** 待绑定的新邮箱，验证码发往此处 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;
}
