package site.muyin.downloadlinks.handle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import site.muyin.downloadlinks.setting.DownloadSetting;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 下载链接渲染器
 *
 * @author <a href="https://lywq.muyin.site">lywq</a>
 * @since 2024/12/11 11:30
 **/
@Component
@RequiredArgsConstructor
public class DownloadLinksRenderer {

    private static final Pattern TAG_PATTERN = Pattern.compile("(?s)<download-links\\b([^>]*)>.*?</download-links>");
    private static final Pattern DATA_LINKS_PATTERN = Pattern.compile("data-links\\s*=\\s*\"(.*?)\"");
    private static final Pattern DOWNLOAD_LINK_PATTERN = Pattern.compile("(?s)<download-link\\b([^>]*)/?>.*?</download-link>|<download-link\\b([^>]*)/>");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("([\\w:-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')");
    private static final Pattern ESCAPED_DOWNLOAD_LINKS_TAG_PATTERN = Pattern.compile("&lt;(/?download-links\\b.*?)&gt;", Pattern.DOTALL);
    private static final Pattern ESCAPED_DOWNLOAD_LINK_TAG_PATTERN = Pattern.compile("&lt;(/?download-link\\b.*?)&gt;", Pattern.DOTALL);
    private static final Pattern DOWNLOAD_LINK_TAG_PATTERN = Pattern.compile("<(/?download-link\\b[^>]*)>", Pattern.DOTALL);
    private static final String STYLE_ID = "tools-download-links-style";
    private static final String STYLE_MARKER = "<!-- " + STYLE_ID + " -->";

    private final ObjectMapper objectMapper;
    private final ReactiveSettingFetcher settingFetcher;

    public Mono<String> render(String html) {
        if (isBlank(html)) {
            return Mono.just(html);
        }
        String normalizedHtml = unescapeDownloadLinkTags(html);
        if (!TAG_PATTERN.matcher(normalizedHtml).find()) {
            return Mono.just(html);
        }
        boolean needsStyle = !normalizedHtml.contains(STYLE_ID) && !normalizedHtml.contains(STYLE_MARKER);

        return settingFetcher.fetch(DownloadSetting.GROUP, DownloadSetting.class)
                .defaultIfEmpty(new DownloadSetting())
                .map(downloadSetting -> {
                    Map<String, String> sourceIconMap = buildSourceIconMap(downloadSetting);
                    String styleBlock = needsStyle ? buildStyleBlock(downloadSetting) : "";

                    final boolean[] styleInjected = {false};
                    String result = replaceAll(normalizedHtml, TAG_PATTERN, matcher -> {
                        String attrs = matcher.group(1);
                        String tagContent = matcher.group();
                        List<Map<String, Object>> links = parseLinks(attrs, tagContent);
                        String htmlContent = buildHtml(links, sourceIconMap);
                        if (needsStyle && !styleInjected[0] && !links.isEmpty()) {
                            styleInjected[0] = true;
                            return styleBlock + "\n" + htmlContent;
                        }
                        return htmlContent;
                    });
                    return result;
                });
    }

    public Mono<String> renderContent(String raw, String content) {
        return render(content)
                .flatMap(renderedContent -> {
                    if (hasRenderedBlock(renderedContent) || !containsDownloadLinks(raw)) {
                        return Mono.just(renderedContent);
                    }
                    String rawBlocks = extractDownloadLinkBlocks(raw);
                    return render(rawBlocks).map(renderedBlocks ->
                            isBlank(renderedBlocks) ? renderedContent : appendHtml(renderedContent, renderedBlocks)
                    );
                });
    }

    private boolean containsDownloadLinks(String html) {
        if (isBlank(html)) {
            return false;
        }
        return TAG_PATTERN.matcher(unescapeDownloadLinkTags(html)).find();
    }

    private boolean hasRenderedBlock(String html) {
        return isNotBlank(html) && html.contains("tools-download-links");
    }

    private String extractDownloadLinkBlocks(String html) {
        if (isBlank(html)) {
            return "";
        }
        Matcher matcher = TAG_PATTERN.matcher(unescapeDownloadLinkTags(html));
        StringBuilder blocks = new StringBuilder();
        while (matcher.find()) {
            blocks.append(matcher.group()).append("\n");
        }
        return blocks.toString();
    }

    private String appendHtml(String content, String htmlToAppend) {
        if (isBlank(content)) {
            return htmlToAppend;
        }
        return content.stripTrailing() + "\n" + htmlToAppend;
    }

    private String unescapeDownloadLinkTags(String html) {
        String normalized = replaceAll(html, ESCAPED_DOWNLOAD_LINKS_TAG_PATTERN, matcher -> "<" + matcher.group(1) + ">");
        normalized = replaceAll(normalized, ESCAPED_DOWNLOAD_LINK_TAG_PATTERN, matcher -> "<" + matcher.group(1) + ">");
        return replaceAll(normalized, DOWNLOAD_LINK_TAG_PATTERN,
                matcher -> "<" + unescapeHtml(matcher.group(1)) + ">");
    }

    private List<Map<String, Object>> parseLinks(String attrs, String tagContent) {
        String data = extractGroup(attrs, DATA_LINKS_PATTERN, 1);
        if (data != null) {
            data = unescapeHtml(data);
            try {
                return objectMapper.readValue(data, new TypeReference<>() {
                });
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return parseDownloadLinkChildren(tagContent);
    }

    private List<Map<String, Object>> parseDownloadLinkChildren(String tagContent) {
        List<Map<String, Object>> links = new java.util.ArrayList<>();
        Matcher matcher = DOWNLOAD_LINK_PATTERN.matcher(tagContent);
        while (matcher.find()) {
            String attrs = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Map<String, Object> link = parseAttributes(attrs);
            if (isNotBlank(str(link.get("url")))) {
                links.add(link);
            }
        }
        return links;
    }

    private Map<String, Object> parseAttributes(String attrs) {
        Map<String, Object> result = new java.util.HashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attrs);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            result.put(name, unescapeHtml(value));
        }
        return result;
    }

    private Map<String, String> buildSourceIconMap(DownloadSetting downloadSetting) {
        List<DownloadSetting.DownloadSource> sourceList = downloadSetting.getDownloadSourceList();
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new java.util.HashMap<>();
        for (DownloadSetting.DownloadSource source : sourceList) {
            if (isNotBlank(source.getName()) && isNotBlank(source.getIcon())) {
                map.put(source.getName(), source.getIcon());
            }
        }
        return map;
    }

    private String buildStyleBlock(DownloadSetting downloadSetting) {
        String lightModeSelector = downloadSetting.getLightModeSelector();
        String darkModeSelector = downloadSetting.getDarkModeSelector();
        List<DownloadSetting.DownloadSource> downloadSourceList = downloadSetting.getDownloadSourceList();

        return """
                    <style id="%s">%s
                    .tools-download-links, .tools-download-links * { box-sizing: border-box; }
                    .tools-download-links button { margin: 0; padding: 0; font: inherit; background: none; border: 0; color: inherit; }
                
                    .tools-download-links { border: 1px solid var(--tools-dl-border) !important; border-radius: 10px !important; background: var(--tools-dl-bg) !important; overflow: hidden !important; margin: 12px 0 !important; }
                    .tools-download-links .tools-download-links__header { display: flex !important; align-items: center !important; gap: 6px !important; padding: 10px 12px !important; background: var(--tools-dl-header-bg) !important; border-bottom: 1px solid var(--tools-dl-border) !important; font-weight: 600 !important; color: var(--tools-dl-header-color) !important; font-size: 13px !important; }
                    .tools-download-links .tools-download-links__list { margin: 0 !important; padding: 10px 12px !important; }
                    .tools-download-links .tools-download-links__item { display: flex !important; align-items: center !important; justify-content: space-between !important; gap: 12px !important; padding: 12px 14px !important; border: 1px solid var(--tools-dl-border) !important; border-radius: 10px !important; margin: 10px 0 !important; background: var(--tools-dl-item-bg) !important; }
                    .tools-download-links .tools-download-links__left { display: flex !important; align-items: center !important; gap: 10px !important; min-width: 0 !important; flex: 1 !important; }
                    .tools-download-links .tools-download-links__icon { width: 40px !important; height: 40px !important; border-radius: 10px !important; background-color: var(--tools-dl-icon-bg) !important; flex-shrink: 0 !important; background-size: contain !important; background-position: center !important; background-repeat: no-repeat !important; display: block !important; }
                    .tools-download-links .tools-download-links__info { display: grid !important; gap: 4px !important; min-width: 0 !important; flex: 1 !important; }
                    .tools-download-links .tools-download-links__title { font-weight: 600 !important; white-space: nowrap !important; overflow: hidden !important; text-overflow: ellipsis !important; font-size: 14px !important; }
                    .tools-download-links .tools-download-links__title-link { color: var(--tools-dl-title-link) !important; cursor: pointer !important; }
                    .tools-download-links .tools-download-links__title-link:hover { color: var(--tools-dl-title-link-hover) !important; text-decoration: underline !important; }
                    .tools-download-links .tools-download-links__meta { color: var(--tools-dl-meta) !important; font-size: 12px !important; }
                    .tools-download-links button.tools-download-links__btn { margin-left: auto !important; display: inline-flex !important; align-items: center !important; justify-content: center !important; width: 36px !important; height: 36px !important; border-radius: 9999px !important; background: var(--tools-dl-btn-bg) !important; border: 1px solid var(--tools-dl-btn-border) !important; color: #ffffff !important; flex-shrink: 0 !important; transition: background .2s ease, border-color .2s ease !important; cursor: pointer !important; padding: 0 !important; font: inherit !important; }
                    .tools-download-links button.tools-download-links__btn:hover { background: var(--tools-dl-btn-bg-hover) !important; border-color: var(--tools-dl-btn-border-hover) !important; }
                    .tools-download-links button.tools-download-links__btn svg { width: 22px !important; height: 22px !important; }
                
                    :root {
                        --tools-dl-border: #edf2f7;
                        --tools-dl-bg: #fafcff;
                        --tools-dl-header-bg: #f7fbff;
                        --tools-dl-header-color: #0f172a;
                        --tools-dl-item-bg: #ffffff;
                        --tools-dl-icon-bg: #eef2f7;
                        --tools-dl-title-link: #1d4ed8;
                        --tools-dl-title-link-hover: #1e40af;
                        --tools-dl-meta: #6b7280;
                        --tools-dl-btn-bg: #60a5fa;
                        --tools-dl-btn-border: #93c5fd;
                        --tools-dl-btn-bg-hover: #3b82f6;
                        --tools-dl-btn-border-hover: #60a5fa;
                    }
                
                    %s {
                        --tools-dl-border: #edf2f7;
                        --tools-dl-bg: #fafcff;
                        --tools-dl-header-bg: #f7fbff;
                        --tools-dl-header-color: #0f172a;
                        --tools-dl-item-bg: #ffffff;
                        --tools-dl-icon-bg: #eef2f7;
                        --tools-dl-title-link: #1d4ed8;
                        --tools-dl-title-link-hover: #1e40af;
                        --tools-dl-meta: #6b7280;
                        --tools-dl-btn-bg: #60a5fa;
                        --tools-dl-btn-border: #93c5fd;
                        --tools-dl-btn-bg-hover: #3b82f6;
                        --tools-dl-btn-border-hover: #60a5fa;
                    }
                    %s {
                        --tools-dl-border: #374151;
                        --tools-dl-bg: #1f2937;
                        --tools-dl-header-bg: #374151;
                        --tools-dl-header-color: #f9fafb;
                        --tools-dl-item-bg: #374151;
                        --tools-dl-icon-bg: #4b5563;
                        --tools-dl-title-link: #60a5fa;
                        --tools-dl-title-link-hover: #93c5fd;
                        --tools-dl-meta: #9ca3af;
                        --tools-dl-btn-bg: #3b82f6;
                        --tools-dl-btn-border: #60a5fa;
                        --tools-dl-btn-bg-hover: #2563eb;
                        --tools-dl-btn-border-hover: #3b82f6;
                    }
                    </style>
                """.formatted(STYLE_ID, STYLE_MARKER, lightModeSelector, darkModeSelector);
    }

    private String buildHtml(List<Map<String, Object>> links, Map<String, String> sourceIconMap) {
        if (links.isEmpty()) {
            return "";
        }

        String containerId = "tools-download-links--" + UUID.randomUUID().toString().replace("-", "");
        StringBuilder items = new StringBuilder();
        StringBuilder iconCssRules = new StringBuilder();
        int index = 0;
        for (Map<String, Object> link : links) {
            String source = str(link.get("source"));
            // 根据 source 从配置中获取 icon
            String icon = sourceIconMap.getOrDefault(source, "");
            String iconClass = "tools-download-links__icon--" + index;
            items.append(buildLinkItem(link, iconClass));

            if (isNotBlank(icon)) {
                String cssUrl = escapeCssUrl(icon);
                iconCssRules.append("#").append(containerId).append(" .").append(iconClass)
                        .append("{background-image:url('")
                        .append(escapeHtml(cssUrl))
                        .append("') !important;}");
            }
            index++;
        }

        String iconStyleBlock = !iconCssRules.isEmpty()
                ? "<style>" + iconCssRules + "</style>\n"
                : "";

        return """
                <div id="%s" class="tools-download-links">
                    <div class="tools-download-links__header">下载地址</div>
                    %s
                    <div class="tools-download-links__list" role="list">
                        %s
                    </div>
                </div>
                """.formatted(containerId, iconStyleBlock, items.toString());
    }

    private String buildLinkItem(Map<String, Object> link, String iconClass) {
        String url = str(link.get("url"));
        String filename = str(link.get("filename"));
        String source = str(link.get("source"));
        String code = str(link.get("code"));

        String escapedSource = escapeHtml(source);
        String displayName = escapeHtml(blankToDefault(filename, url));
        String ariaLabel = escapeHtml("下载 " + blankToDefault(filename, url));

        String jsEscapedUrl = escapeJsString(url);

        String codeInfo = isNotBlank(code)
                ? "  ·  提取码: " + escapeHtml(code)
                : "";

        return """
                <div class="tools-download-links__item" role="listitem">
                    <div class="tools-download-links__left">
                        <div class="tools-download-links__icon %s" role="img" aria-label="%s"></div>
                        <div class="tools-download-links__info">
                            <div class="tools-download-links__title">
                                <button class="tools-download-links__title-link" type="button" role="link" aria-label="%s" onclick="window.open('%s', '_blank', 'noopener,noreferrer')">%s</button>
                            </div>
                            <div class="tools-download-links__meta">
                                %s%s
                            </div>
                        </div>
                    </div>
                    <button class="tools-download-links__btn" type="button" aria-label="%s" onclick="window.open('%s', '_blank', 'noopener,noreferrer')">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M12 3v10m0 0 4-4m-4 4-4-4M5 21h14" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                    </button>
                </div>
                """.formatted(iconClass, escapedSource, ariaLabel, jsEscapedUrl, displayName, escapedSource, codeInfo, ariaLabel, jsEscapedUrl);
    }

    private String replaceAll(String input, Pattern pattern, java.util.function.Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = replacer.apply(matcher);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String extractGroup(String input, Pattern pattern, int group) {
        if (input == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(input);
        if (matcher.find() && matcher.groupCount() >= group) {
            return matcher.group(group);
        }
        return null;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    private String blankToDefault(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    private String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String unescapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private String escapeJsString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeCssUrl(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
