package top.harrylei.bitlog.article.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * HTML 净化工具，防止存储型 XSS
 *
 * @author Harry
 * @since 2026-05-23
 */
public class HtmlSanitizer {

    private static final String ATTR_CLASS = "class";
    private static final String ATTR_ID = "id";

    private static final Safelist SAFELIST = Safelist.relaxed()
        // 代码块和标记 class（语法高亮）
        .addAttributes("code", ATTR_CLASS).addAttributes("pre", ATTR_CLASS).addAttributes("span", ATTR_CLASS)
        .addAttributes("div", ATTR_CLASS)
        // 标题 id（锚点/目录）
        .addAttributes("h1", ATTR_ID).addAttributes("h2", ATTR_ID).addAttributes("h3", ATTR_ID)
        .addAttributes("h4", ATTR_ID).addAttributes("h5", ATTR_ID).addAttributes("h6", ATTR_ID)
        // 外链标记；relaxed() 已含 a[href]=http/https/ftp/mailto，仅补页内锚点
        .addAttributes("a", "target").addProtocols("a", "href", "#");

    private HtmlSanitizer() {}

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        Document clean = new Cleaner(SAFELIST).clean(Jsoup.parseBodyFragment(html));
        clean.outputSettings().prettyPrint(false);
        // 防止 tab-napping：target="_blank" 必须携带 rel="noopener noreferrer"
        clean.select("a[target=_blank]").forEach(a -> a.attr("rel", "noopener noreferrer"));
        return clean.body().html();
    }
}
