package top.harrylei.bitlog.link.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 站点地址归一化测试
 *
 * @author Harry
 * @since 2026-08-15
 */
class SiteUrlNormalizerTest {

    @Test
    @DisplayName("normalize_尾部斜杠_被去掉")
    void normalize_trailingSlash_stripped() {
        assertEquals("https://crow200.top", SiteUrlNormalizer.normalize("https://crow200.top/"));
        assertEquals("https://crow200.top", SiteUrlNormalizer.normalize("https://crow200.top"));
    }

    @Test
    @DisplayName("normalize_协议与主机名_转小写")
    void normalize_schemeAndHost_lowercased() {
        assertEquals("https://crow200.top", SiteUrlNormalizer.normalize("HTTPS://Crow200.TOP"));
    }

    @Test
    @DisplayName("normalize_www前缀_保留")
    void normalize_wwwPrefix_kept() {
        assertEquals("https://www.crow200.top", SiteUrlNormalizer.normalize("https://www.crow200.top"));
    }

    @Test
    @DisplayName("normalize_hash路由片段_保留")
    void normalize_hashFragment_kept() {
        assertEquals("https://www.echoblog.com.cn/#/home",
            SiteUrlNormalizer.normalize("https://www.echoblog.com.cn/#/home"));
    }

    @Test
    @DisplayName("normalize_路径大小写_不变")
    void normalize_pathCase_preserved() {
        assertEquals("https://a.com/Blog/Post", SiteUrlNormalizer.normalize("https://a.com/Blog/Post/"));
    }

    @Test
    @DisplayName("normalize_非默认端口_保留")
    void normalize_explicitPort_kept() {
        assertEquals("http://a.com:8080", SiteUrlNormalizer.normalize("http://a.com:8080/"));
    }

    @Test
    @DisplayName("normalize_显式写出的默认端口_被去掉")
    void normalize_defaultPort_stripped() {
        assertEquals("https://a.com", SiteUrlNormalizer.normalize("https://a.com:443/"));
        assertEquals("http://a.com", SiteUrlNormalizer.normalize("http://a.com:80"));
    }

    @Test
    @DisplayName("normalize_同站不同写法_收敛为同一字符串")
    void normalize_equivalentForms_converge() {
        String canonical = SiteUrlNormalizer.normalize("https://crow200.top");
        assertEquals(canonical, SiteUrlNormalizer.normalize("https://crow200.top/"));
        assertEquals(canonical, SiteUrlNormalizer.normalize("HTTPS://Crow200.top:443/"));
        assertEquals(canonical, SiteUrlNormalizer.normalize("  https://CROW200.TOP  "));
    }

    @Test
    @DisplayName("normalize_首尾空白_被裁剪")
    void normalize_surroundingWhitespace_trimmed() {
        assertEquals("https://a.com", SiteUrlNormalizer.normalize("  https://a.com/  "));
    }

    @Test
    @DisplayName("normalize_javascript伪协议_返回null")
    void normalize_javascriptScheme_rejected() {
        assertNull(SiteUrlNormalizer.normalize("javascript:alert(1)"));
        assertNull(SiteUrlNormalizer.normalize("JavaScript:alert(1)"));
    }

    @Test
    @DisplayName("normalize_data与file协议_返回null")
    void normalize_otherSchemes_rejected() {
        assertNull(SiteUrlNormalizer.normalize("data:text/html,<script>alert(1)</script>"));
        assertNull(SiteUrlNormalizer.normalize("file:///etc/passwd"));
    }

    @Test
    @DisplayName("normalize_缺协议或缺主机_返回null")
    void normalize_incomplete_rejected() {
        assertNull(SiteUrlNormalizer.normalize("crow200.top"));
        assertNull(SiteUrlNormalizer.normalize("https://"));
        assertNull(SiteUrlNormalizer.normalize("/friends"));
    }

    @Test
    @DisplayName("normalize_含空格的畸形地址_返回null")
    void normalize_malformed_rejected() {
        assertNull(SiteUrlNormalizer.normalize("https://exa mple.com"));
    }

    @Test
    @DisplayName("normalize_空输入_返回null")
    void normalize_blank_rejected() {
        assertNull(SiteUrlNormalizer.normalize(null));
        assertNull(SiteUrlNormalizer.normalize(""));
        assertNull(SiteUrlNormalizer.normalize("   "));
    }

    @Test
    @DisplayName("isValid_与normalize判定一致")
    void isValid_matchesNormalize() {
        assertTrue(SiteUrlNormalizer.isValid("https://a.com"));
        assertTrue(SiteUrlNormalizer.isValid("http://a.com"));
        assertFalse(SiteUrlNormalizer.isValid("javascript:alert(1)"));
        assertFalse(SiteUrlNormalizer.isValid(null));
    }
}
