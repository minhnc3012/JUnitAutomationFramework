package saucedemo_webuitesting.testclasses.inventory;

import org.junit.jupiter.params.provider.Arguments;
import saucedemo_webuitesting.testclasses.SauceDemoBaseTest;

import java.util.stream.Stream;

public class InventoryTest extends SauceDemoBaseTest {
    protected boolean isLogin = false;

    @Override
    protected Stream<Arguments> testDataSet() {
        return fetchDataToDataSet("functional/inventory/AddToCartTest.json");
    }

}
