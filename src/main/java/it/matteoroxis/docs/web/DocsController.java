package it.matteoroxis.docs.web;

import java.util.List;
import java.util.Map;

import it.matteoroxis.docs.ask.Answer;
import it.matteoroxis.docs.ask.AskService;
import it.matteoroxis.docs.ask.Question;
import it.matteoroxis.docs.ingest.DocPage;
import it.matteoroxis.docs.ingest.IngestionService;
import it.matteoroxis.docs.search.NativeSearchService;
import it.matteoroxis.docs.search.NativeSearchService.SearchHit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocsController {

    private final IngestionService ingestion;
    private final AskService ask;
    private final NativeSearchService search;

    public DocsController(IngestionService ingestion, AskService ask, NativeSearchService search) {
        this.ingestion = ingestion;
        this.ask = ask;
        this.search = search;
    }

    @PostMapping("/pages")
    public Map<String, Object> ingest(@RequestBody DocPage page) {
        return Map.of("pageId", page.id(), "chunks", ingestion.ingest(page));
    }

    @PostMapping("/ask")
    public Answer ask(@RequestBody Question question) {
        return ask.ask(question);
    }

    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam String team, @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        return search.search(team, q, limit);
    }
}
