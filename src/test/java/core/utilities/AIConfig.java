package core.utilities;

import java.util.Properties;

/**
 * Feature toggles for the optional AI integrations (test-case generation, failure triage).
 * Enable/disable flags live in AI.properties (checked into the repo, no secrets); the
 * Anthropic API key itself is read only from the ANTHROPIC_API_KEY environment variable
 * so it never ends up committed.
 */
public class AIConfig {
    private static final String AI_CONFIG_PATH = "src/test/resources/configFiles/AI.properties";
    private static Properties props;

    private AIConfig() {
    }

    private static Properties getProps() {
        if (props == null) {
            props = Config.getProperties(AI_CONFIG_PATH);
        }
        return props;
    }

    public static boolean isTestGenerationEnabled() {
        return Boolean.parseBoolean(getProps().getProperty("ai.testGeneration.enabled", "false"));
    }

    public static boolean isFailureTriageEnabled() {
        return Boolean.parseBoolean(getProps().getProperty("ai.failureTriage.enabled", "false"));
    }

    public static String getModel() {
        return getProps().getProperty("ai.model", "claude-sonnet-5");
    }

    public static int getMaxTokens() {
        return Integer.parseInt(getProps().getProperty("ai.maxTokens", "400"));
    }

    public static String getApiKey() {
        return System.getenv("ANTHROPIC_API_KEY");
    }

    public static boolean isConfigured() {
        String key = getApiKey();
        return key != null && !key.isEmpty();
    }
}
