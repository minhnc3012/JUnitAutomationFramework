package apitesting.testclasses.login;

import apitesting.testclasses.ApiBaseTest;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class DummyJsonLoginTest extends ApiBaseTest {
    @Override
    protected Stream<Arguments> testDataSet() {
        return fetchDataToDataSet("functional/login/DummyJsonLoginTest.json");
    }
}
