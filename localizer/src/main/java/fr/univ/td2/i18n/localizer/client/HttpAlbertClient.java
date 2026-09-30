package fr.univ.td2.i18n.localizer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

/** Implementation reelle du client Albert, avec {@link java.net.http.HttpClient} et Jackson. */
public final class HttpAlbertClient implements AlbertClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public HttpAlbertClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public List<ModelInfo> listModels() {
        HttpRequest request = requestBuilder("/models")
                .GET()
                .build();
        String body = send(request);
        JsonNode root = readJson(body);
        ArrayNode data = (ArrayNode) root.path("data");
        return StreamSupport.stream(data.spliterator(), false)
                .map(node -> new ModelInfo(node.path("id").asText()))
                .toList();
    }

    @Override
    public String chatComplete(String model, String systemPrompt, String userPrompt) {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("model", model);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        HttpRequest request;
        try {
            request = requestBuilder("/chat/completions")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(90))
                    .build();
        } catch (Exception e) {
            throw new AlbertClientException("Construction de la requete impossible", e);
        }

        String body = send(request);
        JsonNode root = readJson(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || !content.isTextual()) {
            throw new AlbertClientException("Reponse Albert sans contenu exploitable");
        }
        return content.asText();
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30));
    }

    private String send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new AlbertClientException("Erreur reseau lors de l'appel a Albert", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlbertClientException("Appel a Albert interrompu", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new AlbertClientException("Albert a repondu avec le statut " + response.statusCode());
        }
        return response.body();
    }

    private JsonNode readJson(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            throw new AlbertClientException("Reponse Albert illisible (JSON invalide)", e);
        }
    }
}
