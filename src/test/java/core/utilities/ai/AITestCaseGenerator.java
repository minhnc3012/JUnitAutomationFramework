package core.utilities.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.utilities.AIClient;
import core.utilities.AIConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Dev-time tool (not part of the automated test run): generates a new JSON test-case
 * file, in this project's existing testId/testName/testSteps schema, from a plain
 * language feature description. Run it directly from the IDE or CLI, review the
 * output JSON, then drop it under suites/... like any hand-written test file.
 *
 * Usage: AITestCaseGenerator &lt;output-json-path&gt; &lt;feature description...&gt;
 * Example: AITestCaseGenerator src/test/java/saucedemo_webuitesting/suites/functional/login/LoginTest_generated.json
 *          "Verify login validation errors for every saucedemo.com user type, including visual_user and problem_user"
 */
public class AITestCaseGenerator {

    private static final String SCHEMA_EXAMPLE =
            "{\n" +
            "  \"suiteName\": \"<short suite name>\",\n" +
            "  \"suiteDescription\": \"<one line>\",\n" +
            "  \"testCases\": [\n" +
            "    {\n" +
            "      \"testId\": \"<unique id>\",\n" +
            "      \"testName\": \"<short name>\",\n" +
            "      \"testDescription\": \"<what is validated>\",\n" +
            "      \"testObjectives\": \"<expected outcome>\",\n" +
            "      \"note\": \"\",\n" +
            "      \"testSteps\": [\n" +
            "        {\n" +
            "          \"name\": \"<step name>\",\n" +
            "          \"class\": \"LoginAction\",\n" +
            "          \"method\": \"login\",\n" +
            "          \"parameters\": {\n" +
            "            \"user\": \"standard_user\",\n" +
            "            \"password\": \"secret_sauce\",\n" +
            "            \"errorMessage\": null\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String AVAILABLE_KEYWORDS =
            "LoginAction.login(user, password, errorMessage) — logs in; pass errorMessage=null for a "
            + "successful login, or the exact expected error banner text for a failed one.\n"
            + "InventoryAction.addToCartTest(itemNames: string[], counter: int) — logs in as standard_user, "
            + "adds each named product to the cart, asserts the cart badge equals counter.";

    public static void main(String[] args) throws IOException {
        if (!AIConfig.isTestGenerationEnabled()) {
            System.err.println("AI test generation is disabled. Set ai.testGeneration.enabled=true in "
                    + "src/test/resources/configFiles/AI.properties to use this tool.");
            return;
        }
        if (!AIConfig.isConfigured()) {
            System.err.println("ANTHROPIC_API_KEY environment variable is not set.");
            return;
        }
        if (args.length < 2) {
            System.err.println("Usage: AITestCaseGenerator <output-json-path> <feature description...>");
            return;
        }

        String outputPath = args[0];
        String featureDescription = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String prompt = "You generate JSON test-case files for a keyword-driven Selenium test framework.\n"
                + "Available keywords for this feature:\n" + AVAILABLE_KEYWORDS
                + "\n\nProduce ONLY valid JSON matching exactly this shape (no prose, no markdown fences):\n"
                + SCHEMA_EXAMPLE
                + "\n\nGenerate test cases (cover the happy path plus realistic edge cases) for this feature:\n"
                + featureDescription;

        System.out.println("Requesting test cases from " + AIConfig.getModel() + " ...");
        String response = AIClient.ask(prompt);
        if (response == null) {
            System.err.println("AI call failed or returned no content.");
            return;
        }

        String json = extractJson(response);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode parsed;
        try {
            parsed = mapper.readTree(json);
        } catch (Exception e) {
            System.err.println("Model response was not valid JSON, nothing was written. Raw response:\n" + response);
            return;
        }

        Path out = Paths.get(outputPath);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed));
        System.out.println("Wrote generated test cases to " + out.toAbsolutePath()
                + "\nReview it before committing — treat AI-generated test data like a first draft, not ground truth.");
    }

    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
