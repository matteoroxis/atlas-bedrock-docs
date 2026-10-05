# Bedrock owns the model, Atlas owns the data

A small Spring Boot application that answers questions about internal technical documentation.
It splits responsibilities in two:

- **Amazon Bedrock owns the model.** Titan Text Embeddings V2 turns text into vectors, a chat model
  called through the Converse API writes the answers. Bedrock stores nothing.
- **MongoDB Atlas owns the data.** Pages are chunked, stored with their metadata and vectors, indexed,
  filtered and replaced in Atlas. The access boundary (`team`) is enforced there, before any text is
  sent to Bedrock.

Built with Spring Boot 4.1, Spring AI 2.0 and Java 21.

## Where each side lives in the code

| Concern | Side | Where |
|---|---|---|
| Embedding model, chat model, region | Bedrock | `application.yml`, `spring.ai.bedrock.*` |
| Collection, vector index, filter fields | Atlas | `application.yml`, `spring.ai.vectorstore.mongodb.*`, `atlas/doc_chunks_vector_index.json` |
| Chunking, metadata, re-ingestion | Atlas | `ingest/IngestionService.java` |
| Access filter (team, version) | Atlas | `ask/AskService.java` (`accessFilter`) |
| Retrieval + generation | Both, through the advisor | `ask/AskService.java` |
| Hand-written `$vectorSearch` pipeline | Atlas | `search/NativeSearchService.java` |

## Prerequisites

- A MongoDB Atlas cluster and a connection string.
- AWS credentials available through the default provider chain (environment, profile or IAM role),
  with Bedrock model access in your region for:
  - `amazon.titan-embed-text-v2:0`
  - the chat model you choose (default: `eu.anthropic.claude-haiku-4-5-20251001-v1:0`, an EU
    cross-Region inference profile).
- JDK 21 or later, Maven 3.9+.

## Run

Pass the Atlas connection string as an environment variable, or put it in `config/application.yml`
next to `pom.xml` (ignored by git, loaded automatically by Spring Boot):

```yaml
spring:
  mongodb:
    uri: mongodb+srv://<user>:<password>@<cluster>/
    database: docs
```

```bash
export MONGODB_URI="mongodb+srv://<user>:<password>@<cluster>/"
export AWS_REGION=eu-central-1
export DOCS_SEED=true            # load src/main/resources/seed/pages.json at startup
mvn spring-boot:run
```

With `initialize-schema: true` (the default for this sample) Spring AI creates the vector index on
startup. In production, create it explicitly from `atlas/doc_chunks_vector_index.json` and set
`DOCS_INIT_SCHEMA=false`. The index needs a minute to become queryable after creation.

## Try it

The seed contains pages from two teams that both document a "retry policy", with different rules.
The same question gets a different answer depending on the team, because Atlas only returns that
team's chunks.

```bash
curl -s localhost:8080/ask -H 'Content-Type: application/json' \
  -d '{"team":"platform","version":"2.x","question":"What is the retry policy?"}'

curl -s localhost:8080/ask -H 'Content-Type: application/json' \
  -d '{"team":"payments","question":"What is the retry policy?"}'

# Retrieval only, through the hand-written pipeline
curl -s "localhost:8080/search?team=platform&q=timeouts"

# Add or replace a page
curl -s localhost:8080/pages -H 'Content-Type: application/json' -d @page.json
```

In the sample, `team` comes from the request body. In a real deployment it comes from the caller's
identity.

## Tests

`mvn test` runs `AccessFilterTest`, which prints the `$vectorSearch` filter that Spring AI generates
from the access filter. No network calls.
