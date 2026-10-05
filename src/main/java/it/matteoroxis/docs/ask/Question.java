package it.matteoroxis.docs.ask;

/**
 * In a real deployment {@code team} comes from the caller's identity, not from the request body.
 */
public record Question(String team, String version, String question) {
}
