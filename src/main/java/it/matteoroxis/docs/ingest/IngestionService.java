package it.matteoroxis.docs.ingest;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Ingestion path. Chunking, metadata and lifecycle are decided here, on the data side.
 * Bedrock is only called (inside {@code vectorStore.add}) to turn each chunk into a vector.
 */
@Service
public class IngestionService {

    private final VectorStore vectorStore;
    private final String embeddingModelId;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(400).build();

    public IngestionService(VectorStore vectorStore,
            @Value("${spring.ai.bedrock.titan.embedding.model}") String embeddingModelId) {
        this.vectorStore = vectorStore;
        this.embeddingModelId = embeddingModelId;
    }

    public int ingest(DocPage page) {
        // Re-ingesting a page replaces its chunks: the data owner decides what is current.
        vectorStore.delete(new FilterExpressionBuilder().eq("page_id", page.id()).build());

        var source = new Document(page.body(), Map.of(
                "page_id", page.id(),
                "title", page.title(),
                "team", page.team(),
                "product", page.product(),
                "version", page.version(),
                // Nothing else records which model produced the vector, so we do.
                "embedding_model", embeddingModelId));

        List<Document> chunks = splitter.apply(List.of(source));
        vectorStore.add(chunks); // one Bedrock embedding call per chunk, then one write to Atlas
        return chunks.size();
    }
}
