package top.harrylei.bitlog.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 仅含邮箱的请求参数，用于重发验证邮件与申请重置密码
 *
 * @author Harry
 * @since 2026-07-30
 */
@Data
public class EmailParam {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;
}
