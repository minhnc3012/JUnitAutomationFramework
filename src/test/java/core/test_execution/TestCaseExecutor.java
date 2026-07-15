package core.test_execution;

import core.base_action.Action;
import core.base_action.SoftAssertExt;
import core.extent_report.TestReportManager;
import core.testdata_manager.TestCase;
import core.testdata_manager.TestStep;
import core.testdata_manager.TestVariableManager;
import core.utilities.HashMapHandler;
import core.utilities.StringHandler;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

/**
 * The reflection-based keyword-step execution engine: given a TestCase, finds the keyword
 * class named by each step ("core.keywords.X" first, then the feature's own keyword
 * package), invokes the named method by name/arity, and lets soft-assertion failures
 * accumulate. Extracted out of BaseTest so both the fixed, compiled test classes
 * (LoginTest, InventoryTest, ...) and DynamicFeatureRunner's runtime-discovered test cases
 * share exactly one execution engine instead of duplicating it.
 */
public class TestCaseExecutor {
    private final Action actions;
    private final TestVariableManager testVars;
    private final String userKeywordPackage;
    private final LinkedHashMap<String, Object> actionClasses = new LinkedHashMap<>();

    public TestCaseExecutor(Action actions, TestVariableManager testVars, String userKeywordPackage) {
        this.actions = actions;
        this.testVars = testVars;
        this.userKeywordPackage = userKeywordPackage;
    }

    public void executeSteps(TestCase curTestCase) {
        for (TestStep step : curTestCase.get_testSteps()) {
            step.setName((String) StringHandler.replaceValueByMapData(step.getName(), "@var->", "@", testVars.getTestVars()));
            String stepInfo = step.getName() + "</br><u>Action Class:</u> " + step.getClassExecution() + "</br><u>Action:</u> " + step.getMethod();
            TestReportManager.getInstance().setStepInfo(stepInfo);
            step.setTestParams(HashMapHandler.replaceValueByMapData(step.getTestParams(), "@var->", "@", testVars.getTestVars()));

            if (step.getClassExecution() != null) {
                try {
                    Object actionClass = actionClasses.get(step.getClassExecution());
                    if (actionClass == null) {
                        Class<?> clazz;
                        try {
                            // find keyword in core package first
                            clazz = Class.forName("core.keywords." + step.getClassExecution());
                        } catch (ClassNotFoundException e) {
                            // then find keyword in the defined user package
                            clazz = Class.forName(userKeywordPackage + "." + step.getClassExecution());
                        }

                        Constructor<?> cons = clazz.getConstructor(Action.class);
                        actionClass = cons.newInstance(actions);
                        actionClasses.put(step.getClassExecution(), actionClass);
                    }

                    Method setSAAction = actionClass.getClass().getMethod("setSoftAssert", actions.getSoftAssert().getClass());
                    setSAAction.invoke(actionClass, actions.getSoftAssert());

                    Method setTestVars = actionClass.getClass().getMethod("setTestVars", testVars.getTestVars().getClass());
                    setTestVars.invoke(actionClass, testVars.getTestVars());

                    Class<?>[] methodClasses = null;
                    for (Method m : actionClass.getClass().getMethods()) {
                        if (m.getName().equals(step.getMethod()) && m.getParameterCount() == step.getTestParams().size()) {
                            methodClasses = new Class<?>[m.getParameterCount()];
                            for (int i = 0; i < m.getParameterTypes().length; i++) {
                                methodClasses[i] = m.getParameterTypes()[i];
                            }
                        }
                    }

                    Method action = actionClass.getClass().getMethod(step.getMethod(), methodClasses);
                    action.invoke(actionClass, step.getTestParams().values().toArray());

                    Method getSAAction = actionClass.getClass().getMethod("getSoftAssert");
                    actions.setSoftAssert((SoftAssertExt) getSAAction.invoke(actionClass));
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof TestAbortedException) {
                        throw (TestAbortedException) e.getTargetException();
                    } else
                        actions.getSoftAssert().assertTrue(false, e.getTargetException().getMessage());
                } catch (Exception e) {
                    actions.getSoftAssert().assertTrue(false, e.toString());
                }
            }
        }
    }

    public void clear() {
        actionClasses.clear();
    }
}
