package saucedemo_webuitesting.testclasses.login;

import org.junit.jupiter.params.provider.Arguments;
import saucedemo_webuitesting.testclasses.SauceDemoBaseTest;

import java.util.stream.Stream;

public class LoginTest extends SauceDemoBaseTest {
    @Override
    protected Stream<Arguments> testDataSet() {
        return fetchDataToDataSet("functional/login/LoginTest.json");
    }
}
