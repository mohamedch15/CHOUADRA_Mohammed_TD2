package fr.univ.td2.i18n.localizer.translate;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlbertResponseParserTest {

    @Test
    void parsesPlainJsonObject() {
        Optional<Map<String, String>> result = AlbertResponseParser.parse("{\"app.hello\": \"Hello!\"}");

        assertTrue(result.isPresent());
        assertEquals("Hello!", result.get().get("app.hello"));
    }

    @Test
    void parsesJsonWrappedInMarkdownCodeFence() {
        String raw = "```json\n{\"app.hello\": \"Hello!\"}\n```";
        Optional<Map<String, String>> result = AlbertResponseParser.parse(raw);

        assertTrue(result.isPresent());
        assertEquals("Hello!", result.get().get("app.hello"));
    }

    @Test
    void rejectsMalformedJson() {
        Optional<Map<String, String>> result = AlbertResponseParser.parse("ceci n'est pas du JSON");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsJsonArray() {
        Optional<Map<String, String>> result = AlbertResponseParser.parse("[\"a\", \"b\"]");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsBlankResponse() {
        assertTrue(AlbertResponseParser.parse("").isEmpty());
        assertTrue(AlbertResponseParser.parse(null).isEmpty());
    }
}
