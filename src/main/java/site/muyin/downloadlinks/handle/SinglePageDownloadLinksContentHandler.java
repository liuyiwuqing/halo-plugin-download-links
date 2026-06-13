package site.muyin.downloadlinks.handle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactiveSinglePageContentHandler;

@Component
@RequiredArgsConstructor
public class SinglePageDownloadLinksContentHandler implements ReactiveSinglePageContentHandler {

    private final DownloadLinksRenderer renderer;

    @Override
    public Mono<SinglePageContentContext> handle(SinglePageContentContext singlePageContent) {
        String raw = singlePageContent.getRaw();
        String content = singlePageContent.getContent();

        return renderer.render(raw)
                .flatMap(renderedRaw -> renderer.renderContent(raw, content)
                        .map(renderedContent -> {
                            singlePageContent.setRaw(renderedRaw);
                            singlePageContent.setContent(renderedContent);
                            return singlePageContent;
                        }));
    }
}


