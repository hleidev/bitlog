package top.harrylei.bitlog.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 站点身份配置：站点展示名与站点地址是应用级属性，邮件等模块从它派生，而不是反过来
 *
 * @author Harry
 * @since 2026-09-06
 */
@Data
@Validated
@ConfigurationProperties(prefix = "bitlog.site")
public class SiteProperties {

    /** 站点展示名，用作邮件品牌与发件人显示名 */
    private String name;

    /** 站点规范地址，带协议 */
    private String url;

    /** 去掉协议前缀和结尾斜杠后的主机名，仅供展示用 */
    public String displayHost() {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.replaceFirst("^https?://", "").replaceFirst("/$", "");
    }
}
