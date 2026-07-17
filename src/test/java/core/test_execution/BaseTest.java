package core.test_execution;

import core.base_action.Action;
import core.base_action.BrowserType;
import core.extent_report.TestReportManager;
import core.testdata_manager.TestCase;
import core.testdata_manager.TestDataManager;
import core.testdata_manager.TestVariableManager;
import core.utilities.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    protected String prodVer;
    protected String[] testIdList;
    protected String testDataPath;
    protected TestCase curTestCase = null;
    protected TestDataManager testDataManager;
    protected Action actions;
    private String userKeywordPackage;
    private TestCaseExecutor executor;
    protected TestVariableManager testVars;

    public BaseTest() {
        actions = new Action();
        testDataManager = new TestDataManager();
        testVars = new TestVariableManager();
        testDataPath = "";
    }

    // Replaces the "config-file" parameter TestNG's testng.xml <suite> used to inject.
    protected abstract String getConfigFile();

    // Replaces testContext.getSuite().getName() (TestNG suite XML has no JUnit5 equivalent).
    protected abstract String getSuiteName();

    @BeforeAll
    protected void beforeAll() {
        executor = new TestCaseExecutor(actions, testVars, getUserKeywordPackage());
        TestReportManager.getInstance().initializeReport(getSuiteName());
        testVars.getConfigVars().putAll(Config.getHashMapProperties(getConfigFile()));
        if (actions.getWebAction() != null) {
            String browserType = (String) testVars.getConfigVars().get("browser");
            String driverTimeOut = (String) testVars.getConfigVars().get("driverTimeout");
            if (browserType != null && !browserType.isEmpty()) {
                actions.getWebAction().setBrowserType(BrowserType.find(browserType));
                TestReportManager.getInstance().setSystemInfo("Browser", actions.getWebAction().getBrowserType().name());
            }
            if (driverTimeOut != null && !driverTimeOut.isEmpty()) {
                actions.getWebAction().setTimeoutDefault(Integer.parseInt(driverTimeOut));
            }
            if (!actions.getWebAction().isBrowserStarted()) {
                actions.getWebAction().startBrowser();
            }
        }
        if (testVars.getConfigVars().get("productVersion") != null) {
            prodVer = (String) testVars.getConfigVars().get("productVersion");
        }
        TestReportManager.getInstance().setSystemInfo("Product Version", prodVer);
    }

    @AfterAll
    protected void afterAll() {
        afterTestClass();
        TestReportManager.getInstance().getTestReport().flush();
    }

    protected abstract Stream<Arguments> testDataSet();

    @ParameterizedTest
    @MethodSource("testDataSet")
    protected void runTestCase(String path, TestCase tc) {
        curTestCase = tc;
        testVars.loadSystemVariables();
        testDataManager.updateVariable(curTestCase, testVars.getSystemVars());

        if (!Objects.equals(testDataPath, path)) {
            testVars.getTestVars().clear();
            testVars.getTestVars().putAll(testVars.getConfigVars());
            testDataPath = path;
        }
        // just update the runtime var for testcase info (if yes), updating the runtime vars for the test step must be done inside each test action
        testDataManager.updateTCInfoVariable(curTestCase, testVars.getConfigVars());
        actions.initSoftAssert();
        String[] suiteNames = {getSuiteName(), getClass().getSimpleName(),
                testDataManager.getTestSuiteMap().get(testDataPath).get_name().replace(" ", "&thinsp;")};
        TestReportManager.getInstance().setTestInfo(curTestCase, suiteNames);

        try {
            if (curTestCase.isActive()) {
                executor.executeSteps(curTestCase);
                actions.getSoftAssert().assertAll();
            } else {
                TestReportManager.getInstance().getTestReport().testSkip(curTestCase.getNote());
            }
            TestReportManager.getInstance().setStepPass("", "", curTestCase.getId() + ": " + curTestCase.getName());
        } catch (AssertionError e) {
            TestReportManager.getInstance().setStepFail(formatFailureMessage(e), curTestCase.getId() + ": " + curTestCase.getName());
            throw e;
        } finally {
            TestReportManager.getInstance().saveDurationTime("[" + curTestCase.getId() + "] " + curTestCase.getName());
        }
    }

    private String formatFailureMessage(AssertionError e) {
        String failedString = "";
        if (e.getMessage() != null) {
            String[] failedMsgs = e.getMessage().split("\n\t");
            for (int i = 1; i < failedMsgs.length; i++) {
                failedString = failedString + "<br>" + i + ". " + failedMsgs[i];
            }
            if (failedString.equals("")) {
                failedString = e.getMessage();
            }
        }
        return failedString;
    }

    protected Stream<Arguments> fetchDataToDataSet(String... dataPaths) {
        List<Arguments> arrTestData = new ArrayList<>();

        for (String path : dataPaths) {
            testDataManager.setTestData(path);
            ArrayList<TestCase> allTestCase = testDataManager.getTestSuiteMap().get(path).get_testCases();
            for (int i = 0; i < allTestCase.size(); i++) {
                if (!allTestCase.get(i).isActive()) {
                    continue;
                }
                if (testIdList != null && !Arrays.stream(testIdList).anyMatch(allTestCase.get(i).getId()::equals)) {
                    continue;
                }
                arrTestData.add(Arguments.of(path, allTestCase.get(i)));
            }
        }

        return arrTestData.stream();
    }

    protected void afterTestClass() {
        testDataManager.clearTestSuiteMap();
        testVars.clear();
        executor.clear();
        if (actions.getWebAction() != null && actions.getWebAction().isBrowserStarted()) {
            actions.getWebAction().stopAllBrowsers();
        }
    }

    public String getUserKeywordPackage() {
        return userKeywordPackage;
    }

    public void setUserKeywordPackage(String highLevelActionPackage) {
        this.userKeywordPackage = highLevelActionPackage;
    }
}
