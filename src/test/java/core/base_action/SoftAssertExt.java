package core.base_action;

import core.extent_report.ReportLogLevel;
import core.extent_report.TestReportManager;
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
        if (driver != null) {
            String scrFile = driver.getScreenshotAs(OutputType.BASE64);
            TestReportManager.getInstance().setSubStepFail(message + "<br>[FAILED]: " + suffix, scrFile, ReportLogLevel.LOG_LVL_4);
        } else {
            TestReportManager.getInstance().setSubStepFail(message + "<br>[FAILED]: " + suffix, ReportLogLevel.LOG_LVL_4);
        }
        errors.add(new AssertionError(message + " " + suffix));
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
