package com.andreev.tests;

import com.andreev.attachments.WebAttachments;
import com.codeborne.selenide.Condition;
import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

@DisplayName("Добавление товаров в корзину")
public class AdditionalProductsTests {
    public static final String BASE_URL = "https://demowebshop.tricentis.com";
    public static final String CART_URL = "https://demowebshop.tricentis.com/cart";
    private static String usersCookie;
    private final String productsName = "14.1-inch Laptop";
    private final String productsId = "31";
    private final int countOfFirstAdds = 1;
    private final int countOfSecondAdds = 3;

    @Test
    @DisplayName("Увеличивается количество товаров в корзине при дополнительном добавлении")
    @Description("Сначала добавляется один товар, затем еще 3. Проверяется, что в корзине 4 товара")
    void addingAdditionalProducts() {
        step("Добавляем товар в корзину через апи и сохраняем авторизационную cookie из хеддера ответа", () -> {
            step("Отправляем запрос POST /addproducttocart/catalog/{productsName}/1 и сохраняем cookie");
            usersCookie = given()
                    .when()
                    .post(BASE_URL + "/addproducttocart/details/" + productsId + "/1")
                    .then()
                    .statusCode(200)
                    .extract()
                    .cookie("Nop.customer");
            step("Открываем страницу с минимальными параметрами и добавляем cookie", () -> {
                open(BASE_URL + "/Themes/DefaultClean/Content/images/logo.png");
                getWebDriver().manage().addCookie(new Cookie("Nop.customer", usersCookie));
            });
        });

        step("Открываем страницу с корзиной", () -> {
            open(CART_URL);
        });

        step("В корзине один добавленный товар '" + productsName + "'");
        SoftAssertions.assertSoftly(
                softAssertions -> {
                    $(".cart").$(".product-name").shouldHave(Condition.text(productsName));
                    String countItem = $(".qty-input").getValue();
                    assertThat(countItem).isEqualTo(countOfFirstAdds + "");
                });

        step("Добавляем еще " + countOfSecondAdds + " таких же товара в корзину через апи", () -> {
            step("Отправляем запрос POST /addproducttocart/catalog/{productsName}/1 и в теле передаем количество товаров");
            Response response = given()
                    .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                    .cookie("Nop.customer", usersCookie)
                    .body("addtocart_" + productsId + ".EnteredQuantity=" + countOfSecondAdds)
                    .when().log().method().log().uri().log().headers().log().cookies().log().body()
                    .post(BASE_URL + "/addproducttocart/details/" + productsId + "/1")
                    .then().log().status().log().body()
                    .statusCode(200)
                    .extract().response();

            step("Проверка полей ответа");
            SoftAssertions.assertSoftly(
                    softAssertions -> {
                        step("success = true", () -> {
                            response.path("success").equals(true);
                        });
                        step("message = The product has been added to your <a href=\"/cart\">shopping cart</a>", () -> {
                            response.path("message").equals("The product has been added to your <a href=\"/cart\">shopping cart</a>");
                        });
                        step("updatetopcartsectionhtml = (4)", () -> {
                            response.path("updatetopcartsectionhtml").equals("(4)");
                        });
                        step("updateflyoutcartsectionhtml не пустое", () -> {
                            response.path("updateflyoutcartsectionhtml").equals(not(null));
                        });
                    });
        });

        step("Обновляем страницу с корзиной", () -> {
            open(CART_URL);
        });

        step("В корзине три товара '" + productsName + "'");
        SoftAssertions.assertSoftly(
                softAssertions -> {
                    $(".cart").$(".product-name").shouldHave(Condition.text(productsName));
                    String countItem = $(".qty-input").getValue();
                    assertThat(countItem).isEqualTo(countOfFirstAdds + countOfSecondAdds + "");
                    WebAttachments.takeScreenshot();
                });
    }
}
