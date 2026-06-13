package site.muyin.downloadlinks.handle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactivePostContentHandler;

@Component
@RequiredArgsConstructor
public class PostDownloadLinksContentHandler implements ReactivePostContentHandler {

    private final DownloadLinksRenderer renderer;

    @Override
    public Mono<PostContentContext> handle(PostContentContext postContent) {
        String raw = postContent.getRaw();
        String content = postContent.getContent();

        return renderer.render(raw)
                .flatMap(renderedRaw -> renderer.renderContent(raw, content)
                        .map(renderedContent -> {
                            postContent.setRaw(renderedRaw);
                            postContent.setContent(renderedContent);
                            return postContent;
                        }));
    }
}

