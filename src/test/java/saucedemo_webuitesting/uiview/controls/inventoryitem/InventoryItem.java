package saucedemo_webuitesting.uiview.controls.inventoryitem;

import core.base_action.WebAction;
import org.openqa.selenium.WebElement;
import saucedemo_webuitesting.uiview.controls.UIControl;

public class InventoryItem extends UIControl<InventoryItemMap> {
    public InventoryItem(WebElement item, WebAction action){
        super(new InventoryItemMap(), action);
        Map().setInventoryItem(item);
    }

    public InventoryItem clickAddToCart(){
        // Clicking "Add to cart" on one item right after another can occasionally not register
        // on saucedemo.com's current UI, so re-fetch the button and retry until it actually
        // switches to "Remove" instead of assuming a single click always takes effect.
        webAction.waitForExpectedCondition(() -> {
            try {
                webAction.scrollIntoView(Map().getBtnAddToCart());
                Map().getBtnAddToCart().click();
            } catch (Exception ignored) {
            }
            return Map().isBtnRemovePresent();
        });
        return this;
    }
}
