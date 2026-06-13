package site.muyin.downloadlinks.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.halo.app.plugin.ReactiveSettingFetcher;
import site.muyin.downloadlinks.setting.DownloadSetting;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DownloadLinksRendererTest {

    private final DownloadLinksRenderer renderer = new DownloadLinksRenderer(
            new ObjectMapper(),
            new TestSettingFetcher()
    );

    @Test
    void rendersDownloadLinkChildrenForExternalEditors() {
        String html = """
                <article>
                  <download-links>
                    <download-link source="百度网盘" filename="示例文件.zip" url="https://example.com/file.zip" code="abcd"></download-link>
                  </download-links>
                </article>
                """;

        StepVerifier.create(renderer.render(html))
                .assertNext(rendered -> {
                    assertThat(rendered).contains("tools-download-links");
                    assertThat(rendered).contains("示例文件.zip");
                    assertThat(rendered).contains("百度网盘");
                    assertThat(rendered).contains("提取码: abcd");
                    assertThat(rendered).contains("https://example.com/file.zip");
                    assertThat(rendered).doesNotContain("<download-link");
                })
                .verifyComplete();
    }

    @Test
    void rendersDownloadLinksWhenMarkdownWrapsChildrenInParagraphs() {
        String html = """
                <p><download-links></p>
                <p><download-link
                    source="百度网盘"
                    filename="示例文件.zip"
                    url="https://example.com/file.zip"
                    code="abcd"
                  ></download-link><br>
                  <download-link
                    source="夸克网盘"
                    filename="备用下载地址"
                    url="https://example.com/backup.zip"
                  ></download-link></p>
                <p></download-links></p>
                """;

        StepVerifier.create(renderer.render(html))
                .assertNext(rendered -> {
                    assertThat(rendered).contains("tools-download-links");
                    assertThat(rendered).contains("示例文件.zip");
                    assertThat(rendered).contains("备用下载地址");
                    assertThat(rendered).contains("https://example.com/file.zip");
                    assertThat(rendered).contains("https://example.com/backup.zip");
                    assertThat(rendered).doesNotContain("<download-links");
                    assertThat(rendered).doesNotContain("<download-link");
                })
                .verifyComplete();
    }

    @Test
    void rendersDownloadLinksEscapedByMarkdownRenderer() {
        String html = """
                <p>&lt;download-links&gt;
                  &lt;download-link
                    source=&quot;百度网盘&quot;
                    filename=&quot;示例文件.zip&quot;
                    url=&quot;https://example.com/file.zip&quot;
                    code=&quot;abcd&quot;
                  &gt;&lt;/download-link&gt;
                &lt;/download-links&gt;</p>
                """;

        StepVerifier.create(renderer.render(html))
                .assertNext(rendered -> {
                    assertThat(rendered).contains("tools-download-links");
                    assertThat(rendered).contains("示例文件.zip");
                    assertThat(rendered).contains("https://example.com/file.zip");
                    assertThat(rendered).doesNotContain("&lt;download-links");
                    assertThat(rendered).doesNotContain("&lt;download-link");
                })
                .verifyComplete();
    }

    @Test
    void restoresDownloadLinksFromRawWhenMarkdownDropsCustomTags() {
        String content = """
                <p>11</p>

                  
                  
                """;
        String raw = """
                11

                <download-links>
                  <download-link
                    source="百度网盘"
                    filename="示例文件.zip"
                    url="https://example.com/file.zip"
                    code="abcd"
                  ></download-link>
                  <download-link
                    source="夸克网盘"
                    filename="备用下载地址"
                    url="https://example.com/backup.zip"
                  ></download-link>
                </download-links>
                """;

        StepVerifier.create(renderer.renderContent(raw, content))
                .assertNext(rendered -> {
                    assertThat(rendered).contains("<p>11</p>");
                    assertThat(rendered).contains("tools-download-links");
                    assertThat(rendered).contains("示例文件.zip");
                    assertThat(rendered).contains("备用下载地址");
                    assertThat(rendered).contains("https://example.com/file.zip");
                    assertThat(rendered).contains("https://example.com/backup.zip");
                })
                .verifyComplete();
    }

    @Test
    void keepsRenderingDataLinksFromDefaultEditor() {
        String html = """
                <download-links data-links="[{&quot;source&quot;:&quot;百度网盘&quot;,&quot;filename&quot;:&quot;默认编辑器.zip&quot;,&quot;url&quot;:&quot;https://example.com/default.zip&quot;,&quot;code&quot;:&quot;efgh&quot;}]"></download-links>
                """;

        StepVerifier.create(renderer.render(html))
                .assertNext(rendered -> {
                    assertThat(rendered).contains("tools-download-links");
                    assertThat(rendered).contains("默认编辑器.zip");
                    assertThat(rendered).contains("提取码: efgh");
                    assertThat(rendered).contains("https://example.com/default.zip");
                })
                .verifyComplete();
    }

    @Test
    void skipsDownloadLinkChildrenWithoutUrl() {
        String html = """
                <download-links>
                  <download-link source="百度网盘" filename="缺少链接"></download-link>
                </download-links>
                """;

        StepVerifier.create(renderer.render(html))
                .assertNext(rendered -> {
                    assertThat(rendered).doesNotContain("tools-download-links__item");
                    assertThat(rendered).doesNotContain("缺少链接");
                })
                .verifyComplete();
    }

    @SuppressWarnings("removal")
    private static class TestSettingFetcher implements ReactiveSettingFetcher {

        @Override
        public <T> Mono<T> fetch(String group, Class<T> clazz) {
            DownloadSetting setting = new DownloadSetting()
                    .setLightModeSelector(".light")
                    .setDarkModeSelector(".dark")
                    .setDownloadSourceList(List.of(
                            new DownloadSetting.DownloadSource()
                                    .setName("百度网盘")
                                    .setIcon("/plugins/download-links/assets/static/icon/baidu.png")
                    ));
            return Mono.just(clazz.cast(setting));
        }

        @Override
        public Mono<com.fasterxml.jackson.databind.JsonNode> get(String group) {
            return Mono.empty();
        }

        @Override
        public Mono<JsonNode> getSettingValue(String group) {
            return Mono.empty();
        }

        @Override
        public Mono<Map<String, com.fasterxml.jackson.databind.JsonNode>> getValues() {
            return Mono.just(Map.of());
        }

        @Override
        public Mono<Map<String, JsonNode>> getSettingValues() {
            return Mono.just(Map.of());
        }
    }
}
