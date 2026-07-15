package core.test_execution;

import core.base_action.Action;
import core.base_action.BrowserType;
import core.base_action.RestAction;
import core.base_action.WebAction;
import core.extent_report.TestReportManager;
import core.testdata_manager.TestCase;
import core.testdata_manager.TestDataManager;
import core.testdata_manager.TestVariableManager;
import core.utilities.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generic, runtime-driven test runner. Unlike LoginTest/InventoryTest, which each hardcode
 * one JSON path via @MethodSource, this scans a directory of *.json test-case files supplied
 * at invocation time and generates one DynamicTest per test case using the exact same
 * TestCaseExecutor engine. Dropping a new JSON file into that folder makes it run on the
 * next invocation — no new Java class, no recompile. This is what TestPlatform's "Run"
 * button in the web UI actually invokes on the backend.
 *
 * Invoked as:
 *   mvn test -Dtest=DynamicFeatureRunner \
 *     -Dfeature.path=&lt;folder containing *.json test-case files&gt; \
 *     -Dkeyword.package=&lt;package with this feature's keyword classes, e.g. saucedemo_webuitesting.keywords&gt; \
 *     -Dconfig.file=&lt;properties file, e.g. src/test/resources/configFiles/WebUI_Chrome.properties&gt; \
 *     -Dsuite.name=&lt;report name, e.g. LOGIN_FEATURE&gt;
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DynamicFeatureRunner {
    private Action actions;

    @TestFactory
    Stream<DynamicNode> discoverAndRun() {
        String featurePath = require("feature.path");
        String keywordPackage = require("keyword.package");
        String configFile = System.getProperty("config.file", "");
        String suiteName = System.getProperty("suite.name", "DYNAMIC_FEATURE");

        TestReportManager.getInstance().initializeReport(suiteName);

        TestVariableManager testVars = new TestVariableManager();
        testVars.getConfigVars().putAll(Config.getHashMapProperties(configFile));

        actions = new Action();
        actions.setWebAction(new WebAction());
        actions.setRestAction(new RestAction());

        String browserType = (String) testVars.getConfigVars().get("browser");
        String driverTimeOut = (String) testVars.getConfigVars().get("driverTimeout");
        if (browserType != null && !browserType.isEmpty()) {
            actions.getWebAction().setBrowserType(BrowserType.find(browserType));
        }
        if (driverTimeOut != null && !driverTimeOut.isEmpty()) {
            actions.getWebAction().setTimeoutDefault(Integer.parseInt(driverTimeOut));
        }
        actions.getWebAction().startBrowser();

        TestCaseExecutor executor = new TestCaseExecutor(actions, testVars, keywordPackage);
        TestDataManager testDataManager = new TestDataManager();
        testDataManager.setTestDataPath("");

        File folder = new File(featurePath);
        File[] jsonFiles = folder.listFiles((dir, name) -> name.endsWith(".json"));
        List<DynamicNode> containers = new ArrayList<>();

        if (jsonFiles != null) {
            Arrays.sort(jsonFiles, Comparator.comparing(File::getName));
            for (File jsonFile : jsonFiles) {
                String path = jsonFile.getPath();
                testDataManager.setTestData(path);
                List<TestCase> testCases = testDataManager.getTestSuiteMap().get(path).get_testCases();

                List<DynamicTest> dynamicTests = new ArrayList<>();
                for (TestCase tc : testCases) {
                    if (!tc.isActive()) {
                        continue;
                    }
                    dynamicTests.add(DynamicTest.dynamicTest(tc.getId() + ": " + tc.getName(), () ->
                            runOne(tc, executor, testDataManager, testVars, suiteName, folder.getName(), jsonFile.getName())));
                }
                containers.add(DynamicContainer.dynamicContainer(jsonFile.getName(), dynamicTests));
            }
        }

        return containers.stream();
    }

    private void runOne(TestCase tc, TestCaseExecutor executor, TestDataManager testDataManager,
                         TestVariableManager testVars, String suiteName, String moduleName, String featureFileName) {
        testVars.loadSystemVariables();
        testDataManager.updateVariable(tc, testVars.getSystemVars());
        testVars.getTestVars().clear();
        testVars.getTestVars().putAll(testVars.getConfigVars());
        testDataManager.updateTCInfoVariable(tc, testVars.getConfigVars());
        actions.initSoftAssert();
        TestReportManager.getInstance().setTestInfo(tc, new String[]{suiteName, moduleName, featureFileName});

        try {
            executor.executeSteps(tc);
            actions.getSoftAssert().assertAll();
            TestReportManager.getInstance().setStepPass("", "", tc.getId() + ": " + tc.getName());
        } catch (AssertionError e) {
            TestReportManager.getInstance().setStepFail(e.getMessage(), tc.getId() + ": " + tc.getName());
            throw e;
        } finally {
            TestReportManager.getInstance().saveDurationTime("[" + tc.getId() + "] " + tc.getName());
        }
    }

    @AfterAll
    void tearDown() {
        if (actions != null && actions.getWebAction() != null && actions.getWebAction().getBrowser() != null) {
            actions.getWebAction().stopAllBrowsers();
        }
        TestReportManager.getInstance().getTestReport().flush();
    }

    private static String require(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing required system property: -D" + key + "=...");
        }
        return value;
    }
}
