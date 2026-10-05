package it.matteoroxis.docs.ask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasFilterExpressionConverter;

/**
 * Shows what the access filter becomes once Spring AI hands it to Atlas: the {@code filter}
 * of the {@code $vectorSearch} stage. No network calls.
 */
class AccessFilterTest {

    private final MongoDBAtlasFilterExpressionConverter converter = new MongoDBAtlasFilterExpressionConverter();

    @Test
    void teamOnly() {
        var filter = AskService.accessFilter(new Question("platform", null, "retry policy?"));
        String mql = converter.convertExpression(filter);
        System.out.println("teamOnly -> " + mql);
        assertThat(mql).contains("metadata.team").contains("platform");
    }

    @Test
    void teamAndVersion() {
        var filter = AskService.accessFilter(new Question("platform", "2.x", "retry policy?"));
        String mql = converter.convertExpression(filter);
        System.out.println("teamAndVersion -> " + mql);
        assertThat(mql).contains("$and").contains("metadata.team").contains("metadata.version");
    }
}
