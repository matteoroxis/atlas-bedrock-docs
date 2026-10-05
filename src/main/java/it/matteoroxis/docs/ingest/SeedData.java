package it.matteoroxis.docs.ingest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads the sample pages at startup when {@code docs.seed=true}.
 */
@Component
@ConditionalOnProperty(name = "docs.seed", havingValue = "true")
class SeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedData.class);

    private final IngestionService ingestion;
    private final JsonMapper jsonMapper;

    SeedData(IngestionService ingestion, JsonMapper jsonMapper) {
        this.ingestion = ingestion;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try (InputStream in = new ClassPathResource("seed/pages.json").getInputStream()) {
            List<DocPage> pages = jsonMapper.readValue(in, new TypeReference<List<DocPage>>() { });
            for (DocPage page : pages) {
                log.info("Ingested {} ({} chunks)", page.id(), ingestion.ingest(page));
            }
        }
    }
}
