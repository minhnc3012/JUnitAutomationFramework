package core.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around Anthropic's Messages API (plain java.net.http.HttpClient + the
 * Jackson dependency already on the classpath — no new SDK dependency needed).
 * Every method fails soft: on any error it logs to stderr and returns null rather than
 * throwing, so an AI-integration hiccup never breaks the underlying test run.
 */
public class AIClient {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    private AIClient() {
    }

    /** Text-only prompt. Returns the model's reply text, or null if disabled/failed. */
    public static String ask(String prompt) {
        return ask(prompt, null);
    }

    /** Prompt with an optional base64 PNG screenshot attached. Returns null if disabled/failed. */
    public static String ask(String prompt, String base64Screenshot) {
        if (!AIConfig.isConfigured()) {
            System.err.println("[AIClient] Skipped: ANTHROPIC_API_KEY is not set.");
            return null;
        }
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("model", AIConfig.getModel());
            root.put("max_tokens", AIConfig.getMaxTokens());

            ArrayNode contentArray = MAPPER.createArrayNode();
            if (base64Screenshot != null && !base64Screenshot.isEmpty()) {
                ObjectNode imageBlock = MAPPER.createObjectNode();
                imageBlock.put("type", "image");
                ObjectNode source = MAPPER.createObjectNode();
                source.put("type", "base64");
                source.put("media_type", "image/png");
                source.put("data", base64Screenshot);
                imageBlock.set("source", source);
                contentArray.add(imageBlock);
            }
            ObjectNode textBlock = MAPPER.createObjectNode();
            textBlock.put("type", "text");
            textBlock.put("text", prompt);
            contentArray.add(textBlock);

            ObjectNode message = MAPPER.createObjectNode();
            message.put("role", "user");
            message.set("content", contentArray);

            ArrayNode messages = MAPPER.createArrayNode();
            messages.add(message);
            root.set("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("x-api-key", AIConfig.getApiKey())
                    .header("anthropic-version", API_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(root)))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[AIClient] API call failed (" + response.statusCode() + "): " + response.body());
                return null;
            }
            JsonNode body = MAPPER.readTree(response.body());
            JsonNode contentNode = body.get("content");
            if (contentNode != null && contentNode.isArray() && contentNode.size() > 0) {
                JsonNode firstBlock = contentNode.get(0);
                if (firstBlock.has("text")) {
                    return firstBlock.get("text").asText();
                }
            }
            return null;
        } catch (IOException e) {
            System.err.println("[AIClient] Request error: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[AIClient] Request interrupted: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("[AIClient] Unexpected error: " + e.getMessage());
            return null;
        }
    }
}
