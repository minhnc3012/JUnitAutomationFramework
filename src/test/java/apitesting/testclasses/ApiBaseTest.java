package apitesting.testclasses;

import core.base_action.RestAction;
import core.test_execution.BaseTest;

/**
 * Unlike SauceDemoBaseTest, this never calls actions.setWebAction(...), so
 * BaseTest.beforeAll()'s browser-startup block (guarded by
 * "actions.getWebAction() != null") is skipped entirely - no browser is launched for a
 * pure REST suite. getConfigFile() returns "" because Config.getHashMapProperties()
 * short-circuits on an empty path, so there is nothing to configure either.
 */
public abstract class ApiBaseTest extends BaseTest {
    public ApiBaseTest(){
        actions.setRestAction(new RestAction());
        setUserKeywordPackage("apitesting.keywords");
        testDataManager.setTestDataPath("src/test/java/apitesting/suites/");
    }

    @Override
    protected String getConfigFile() {
        return "";
    }

    @Override
    protected String getSuiteName() {
        return "API_SMOKE_TEST";
    }
}
