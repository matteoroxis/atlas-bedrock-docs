package it.matteoroxis.docs.search;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * The same retrieval the advisor performs, written out by hand: one Bedrock call to embed the
 * question, then a plain aggregation pipeline that Atlas runs. Useful when you want to own the
 * document shape or add stages ($lookup, $project) that the VectorStore abstraction does not expose.
 */
@Service
public class NativeSearchService {

    private final MongoTemplate mongoTemplate;
    private final EmbeddingModel embeddingModel;
    private final String collection;
    private final String index;

    public NativeSearchService(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.mongodb.collection-name}") String collection,
            @Value("${spring.ai.vectorstore.mongodb.index-name}") String index) {
        this.mongoTemplate = mongoTemplate;
        this.embeddingModel = embeddingModel;
        this.collection = collection;
        this.index = index;
    }

    public List<SearchHit> search(String team, String text, int limit) {
        float[] vector = embeddingModel.embed(text); // Bedrock: text in, vector out

        List<Document> pipeline = List.of(
                new Document("$vectorSearch", new Document("index", index)
                        .append("path", "embedding")
                        .append("queryVector", toList(vector))
                        .append("numCandidates", limit * 20)
                        .append("limit", limit)
                        .append("filter", new Document("metadata.team", team))),
                new Document("$project", new Document("_id", 0)
                        .append("title", "$metadata.title")
                        .append("version", "$metadata.version")
                        .append("content", 1)
                        .append("score", new Document("$meta", "vectorSearchScore"))));

        return mongoTemplate.getCollection(collection).aggregate(pipeline)
                .map(d -> new SearchHit(d.getString("title"), d.getString("version"),
                        d.getString("content"), d.getDouble("score")))
                .into(new ArrayList<>());
    }

    private static List<Float> toList(float[] vector) {
        var list = new ArrayList<Float>(vector.length);
        for (float v : vector) {
            list.add(v);
        }
        return list;
    }

    public record SearchHit(String title, String version, String content, double score) {
    }
}
