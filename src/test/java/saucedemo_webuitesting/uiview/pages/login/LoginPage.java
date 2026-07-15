package saucedemo_webuitesting.uiview.pages.login;

import core.base_action.WebAction;
import core.base_ui.BaseWebUI;


public class LoginPage extends BaseWebUI<LoginPageMap> {

    public LoginPage(WebAction action) {
        super(new LoginPageMap(), action);
    }

    public LoginPage navigate(String url) {
        webAction.getToUrl(url, true);
        // saucedemo.com keeps the logged-in session in a cookie (separate from cart-contents,
        // which lives in localStorage), so a plain reload keeps a user logged in from a previous
        // test case; clear only the cookie so cart state carried over on purpose is untouched.
        webAction.getBrowser().manage().deleteAllCookies();
        webAction.getToUrl(url, true);
        return this;
    }

    public LoginPage login(String user, String password) {
        webAction.type(Map().getTxtUser(), user,"User Name");
        webAction.type(Map().getTxtPassword(), password, "Password");
        webAction.click(Map().getBtnLogIn(), "Login");
        return this;
    }
}
