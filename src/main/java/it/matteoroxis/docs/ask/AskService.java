package it.matteoroxis.docs.ask;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * Query path. Atlas decides which chunks the model may see (vector search plus the team filter),
 * Bedrock writes the answer from those chunks only.
 */
@Service
public class AskService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AskService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultSystem("You answer questions about internal technical documentation. "
                        + "Use only the context you are given. If it does not contain the answer, say so.")
                .build();
        this.vectorStore = vectorStore;
    }

    public Answer ask(Question question) {
        var advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .filterExpression(accessFilter(question))
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build())
                .build();

        ChatResponse response = chatClient.prompt()
                .user(question.question())
                .advisors(advisor)
                .call()
                .chatResponse();

        List<Document> retrieved = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        List<String> sources = retrieved == null ? List.of()
                : retrieved.stream().map(d -> String.valueOf(d.getMetadata().get("title"))).distinct().toList();

        return new Answer(Objects.requireNonNull(response.getResult()).getOutput().getText(), sources);
    }

    // Built with the typed builder, not by concatenating user input into a filter string.
    static Filter.Expression accessFilter(Question q) {
        var b = new FilterExpressionBuilder();
        var team = b.eq("team", q.team());
        return (q.version() == null ? team : b.and(team, b.eq("version", q.version()))).build();
    }
}
