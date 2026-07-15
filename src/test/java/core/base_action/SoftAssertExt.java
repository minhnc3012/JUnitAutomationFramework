package core.base_action;

import core.extent_report.ReportLogLevel;
import core.extent_report.TestReportManager;
import core.utilities.AIClient;
import core.utilities.AIConfig;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TestNG's SoftAssert hooks onAssertSuccess/onAssertFailure to log to ExtentReports and capture
 * screenshots. JUnit5 has no soft-assert type, so this collects failures itself and logs inline
 * at the call site instead of via a framework callback.
 */
public class SoftAssertExt {
    private TakesScreenshot driver;
    private boolean isTakeScreenshot;
    private final List<AssertionError> errors = new ArrayList<>();

    public SoftAssertExt() {
    }

    public SoftAssertExt(TakesScreenshot dr) {
        driver = dr;
        isTakeScreenshot = false;
    }

    public void assertTrue(boolean condition, String message) {
        assertEquals(condition, true, message);
    }

    public void assertEquals(Object actual, Object expected, String message) {
        if (Objects.equals(actual, expected)) {
            onAssertSuccess(message);
        } else {
            onAssertFailure(message, expected, actual);
        }
    }

    private void onAssertSuccess(String message) {
        System.err.println(message + " <PASSED> ");
        if (isTakeScreenshot) {
            String scrFile = driver.getScreenshotAs(OutputType.BASE64);
            TestReportManager.getInstance().setSubStepPass(message + " <PASSED> ", scrFile, ReportLogLevel.LOG_LVL_4);
            isTakeScreenshot = false;
        } else {
            TestReportManager.getInstance().setSubStepPass(message + " <PASSED> ", ReportLogLevel.LOG_LVL_4);
        }
    }

    private void onAssertFailure(String message, Object expected, Object actual) {
        String suffix = String.format("Expected [%s] but found [%s]", expected, actual);
        System.err.println(message + " <FAILED>. " + suffix);
        String scrFile = driver != null ? driver.getScreenshotAs(OutputType.BASE64) : null;

        String failureBlock = message + "<br>[FAILED]: " + suffix;
        if (AIConfig.isFailureTriageEnabled()) {
            String triage = requestAiTriage(message, expected, actual, scrFile);
            if (triage != null && !triage.isEmpty()) {
                failureBlock += "<br><b>[AI triage]:</b> " + triage;
            }
        }

        if (scrFile != null) {
            TestReportManager.getInstance().setSubStepFail(failureBlock, scrFile, ReportLogLevel.LOG_LVL_4);
        } else {
            TestReportManager.getInstance().setSubStepFail(failureBlock, ReportLogLevel.LOG_LVL_4);
        }
        errors.add(new AssertionError(message + " " + suffix));
    }

    // Optional (ai.failureTriage.enabled): asks the model for a one-line root-cause hypothesis.
    // Fails soft — AIClient returns null on any error/missing API key, so this never breaks a test run.
    private String requestAiTriage(String message, Object expected, Object actual, String screenshot) {
        String prompt = "You are helping triage a failed UI test assertion in a Selenium test suite. "
                + "Assertion: \"" + message + "\". Expected: [" + expected + "]. Actual: [" + actual + "]. "
                + "In 1-2 short sentences, suggest the most likely root cause "
                + "(e.g. UI text/locator changed, timing/race condition, real application bug). Be concise and specific.";
        return AIClient.ask(prompt, screenshot);
    }

    public void assertAll() {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("The following asserts failed:");
            for (AssertionError e : errors) {
                sb.append("\n\t").append(e.getMessage());
            }
            List<AssertionError> collected = new ArrayList<>(errors);
            errors.clear();
            AssertionError combined = new AssertionError(sb.toString());
            if (!collected.isEmpty()) {
                combined.initCause(collected.get(0));
            }
            throw combined;
        }
    }

    public SoftAssertExt takeScreenshot() {
        isTakeScreenshot = true;
        return this;
    }
}
