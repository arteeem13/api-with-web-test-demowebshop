package com.andreev.tests;

import com.codeborne.selenide.Condition;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class DemowebshopTests {

    @Test
    void test() {
        String productsName = "14.1-inch Laptop";
        String productsId = "31";
        String countOfFirstAdds = "1";
        String countOfSecondAdds = "3";

        step("Добавляем товар в корзину через апи и сохраняем авторизационную cookie из хеддера ответа", () -> {

            step("Отправляем запрос POST /addproducttocart/catalog/{productsName}/1 и сохраняем cookie");
            String usersCookie = given()
                    .when()
                    .post("https://demowebshop.tricentis.com/addproducttocart/details/" + productsId + "/1")
                    .then()
                    .statusCode(200)
                    .extract()
                    .cookie("Nop.customer");

            step("Открываем страницу с минимальными параметрами и добавляем cookie", () -> {
                open("https://demowebshop.tricentis.com/Themes/DefaultClean/Content/images/logo.png");
                getWebDriver().manage().addCookie(new Cookie("Nop.customer", usersCookie));
            });
        });

        step("Открываем страницу с корзиной", () -> {
            open("https://demowebshop.tricentis.com/cart");
        });

        step("В корзине один добавленный товар '" + productsName + "'");
        SoftAssertions.assertSoftly(
                softAssertions -> {
                    $(".cart").$(".product-name").shouldHave(Condition.text(productsName));
                    String countItem = $(".qty-input").getValue();
                    assertThat(countItem).isEqualTo(countOfFirstAdds);
                });

        step("Добавляем еще 3 таких же товара в корзину через апи", () -> {
            step("Отправляем запрос POST /addproducttocart/catalog/{productsName}/1 и сохраняем cookie");
            Response response = given()
                    .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                    .body("addtocart_" + productsId + ".EnteredQuantity: " + countOfSecondAdds)
                    .when()
                    .post("https://demowebshop.tricentis.com/addproducttocart/details/" + productsId + "/1")
                    .then()
                    .statusCode(302)
                    .extract().response();
            System.out.println(response);
        });
    }
}
