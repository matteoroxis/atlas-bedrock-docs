package it.matteoroxis.docs.ingest;

/**
 * A page of internal technical documentation. {@code team} is the access boundary:
 * a question asked on behalf of one team must never retrieve another team's pages.
 */
public record DocPage(String id, String title, String team, String product, String version, String body) {
}
