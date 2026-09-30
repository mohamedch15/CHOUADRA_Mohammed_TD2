package fr.univ.td2.i18n.localizer.orchestrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.univ.td2.i18n.localizer.client.AlbertClient;
import fr.univ.td2.i18n.localizer.client.AlbertClientException;
import fr.univ.td2.i18n.localizer.client.ModelInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Double de test pour {@link AlbertClient} : ne fait aucun appel reseau. Chaque
 * appel a {@code chatComplete} est enregistre (utile pour verifier qu'on ne
 * redemande pas des traductions deja presentes), et la reponse est calculee par
 * une fonction fournie par le test, ce qui permet de simuler une reponse valide,
 * invalide, ou une panne.
 */
final class FakeAlbertClient implements AlbertClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Function<String, String> responseFactory;
    final List<String> requestedUserPrompts = new ArrayList<>();

    FakeAlbertClient(Function<String, String> responseFactory) {
        this.responseFactory = responseFactory;
    }

    static FakeAlbertClient returning(Map<String, String> translations) {
        return new FakeAlbertClient(prompt -> toJson(translations));
    }

    static FakeAlbertClient returningInvalidJson() {
        return new FakeAlbertClient(prompt -> "ceci n'est pas du JSON");
    }

    static FakeAlbertClient throwingNetworkError() {
        return new FakeAlbertClient(prompt -> {
            throw new AlbertClientException("service indisponible");
        });
    }

    @Override
    public List<ModelInfo> listModels() {
        return List.of(new ModelInfo("fake-model"));
    }

    @Override
    public String chatComplete(String model, String systemPrompt, String userPrompt) {
        requestedUserPrompts.add(userPrompt);
        return responseFactory.apply(userPrompt);
    }

    private static String toJson(Map<String, String> translations) {
        try {
            return MAPPER.writeValueAsString(translations);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
