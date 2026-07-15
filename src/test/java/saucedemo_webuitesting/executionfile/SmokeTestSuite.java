package saucedemo_webuitesting.executionfile;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import saucedemo_webuitesting.testclasses.inventory.InventoryTest;
import saucedemo_webuitesting.testclasses.login.LoginTest;

// Java-class equivalent of the old testng.xml <suite name="Smoke_Test"> grouping —
// JUnit5/Surefire auto-discovers *Test classes on its own, so this exists purely for
// organizational parity (and lets an IDE run "Smoke_Test" as one named suite).
@Suite
@SuiteDisplayName("Smoke_Test")
@SelectClasses({LoginTest.class, InventoryTest.class})
public class SmokeTestSuite {
}
