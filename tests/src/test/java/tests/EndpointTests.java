package tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest(httpPort = 8888)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EndpointTests {

    private static String validToken;
    private static String invalidToken;
    private static final String API_KEY = "qazWSXedc";

    @BeforeAll
    void generateTokens() {
        validToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
        invalidToken = "INVALIDTOKEN12345678901234567890";
    }

    @BeforeEach
    void setUp() {
        // Настраиваем WireMock для /auth
        stubFor(post(urlEqualTo("/auth"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        // Настраиваем WireMock для /doAction
        stubFor(post(urlEqualTo("/doAction"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));
    }

    @Test
    @Description("Тест успешной аутентификации (LOGIN)")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-001")
    void testLoginSuccess() {
        String body = "token=" + validToken + "&action=LOGIN";
        
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(200, statusCode, "Статус код должен быть 200");
        assertEquals("OK", response.jsonPath().getString("result"), "Результат должен быть OK");
        
        // Проверяем, что приложение отправило запрос на внешний сервис
        verify(postRequestedFor(urlEqualTo("/auth"))
            .withRequestBody(containing("token=" + validToken))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded")));
    }

    @Test
    @Description("Тест LOGIN без заголовка X-Api-Key")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-002")
    void testLoginWithoutApiKey() {
        String body = "token=" + validToken + "&action=LOGIN";
        var response = sendRequestWithoutApiKey(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(401, statusCode, "Статус код должен быть 401");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Test
    @Description("Тест LOGIN с неверным API-ключом")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-003")
    void testLoginWithInvalidApiKey() {
        String body = "token=" + validToken + "&action=LOGIN";
        var response = sendRequestWithWrongApiKey(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(401, statusCode, "Статус код должен быть 401");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест ACTION после успешного LOGIN")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-004")
    void testActionAfterLogin() {
        // Используем уникальный токен для изоляции теста
        String testToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
        
        // Сначала делаем LOGIN
        String loginBody = "token=" + testToken + "&action=LOGIN";
        var loginResponse = sendRequest(loginBody);
        int loginStatus = loginResponse.getStatusCode();
        assertEquals(200, loginStatus, "Логин должен успешно пройти");
        
        // Затем делаем ACTION
        String actionBody = "token=" + testToken + "&action=ACTION";
        var actionResponse = sendRequest(actionBody);
        int actionStatus = actionResponse.getStatusCode();
        
        assertEquals(200, actionStatus, "ACTION должен быть успешным");
        assertEquals("OK", actionResponse.jsonPath().getString("result"), "Результат должен быть OK");
        
        // Проверяем, что приложение отправило запрос на /doAction
        verify(postRequestedFor(urlEqualTo("/doAction"))
            .withRequestBody(containing("token=" + testToken)));
    }

    @Test
    @Description("Тест ACTION без предварительного LOGIN")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-005")
    void testActionWithoutLogin() {
        // Используем уникальный токен, который точно не был использован для LOGIN
        String unusedToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
        String actionBody = "token=" + unusedToken + "&action=ACTION";
        var response = sendRequest(actionBody);
        int statusCode = response.getStatusCode();
        
        assertEquals(403, statusCode, "Статус код должен быть 403");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Test
    @Description("Тест неуспешного LOGIN (внешний сервис возвращает ошибку)")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-006")
    void testLoginFailure() {
        // Используем уникальный токен для изоляции теста
        String failureToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
        
        // Настраиваем WireMock на возврат ошибки для /auth с конкретным токеном
        stubFor(post(urlEqualTo("/auth"))
            .withRequestBody(containing("token=" + failureToken))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")));
        
        String body = "token=" + failureToken + "&action=LOGIN";
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(500, statusCode, "Статус код должен быть 500");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест LOGOUT после успешного LOGIN")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-007")
    void testLogoutAfterLogin() {
        // Используем уникальный токен для изоляции теста
        String testToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
        
        // Сначала делаем LOGIN
        String loginBody = "token=" + testToken + "&action=LOGIN";
        var loginResponse = sendRequest(loginBody);
        int loginStatus = loginResponse.getStatusCode();
        assertEquals(200, loginStatus, "Логин должен успешно пройти");
        
        // Затем делаем LOGOUT
        String logoutBody = "token=" + testToken + "&action=LOGOUT";
        var logoutResponse = sendRequest(logoutBody);
        int logoutStatus = logoutResponse.getStatusCode();
        
        assertEquals(200, logoutStatus, "LOGOUT должен быть успешным");
        assertEquals("OK", logoutResponse.jsonPath().getString("result"), "Результат должен быть OK");
    }

    @Test
    @Description("Тест LOGOUT без предварительного LOGIN")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("TEST-008")
    void testLogoutWithoutLogin() {
        // Используем уникальный токен, который точно не был использован для LOGIN
        String unusedToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();
        String logoutBody = "token=" + unusedToken + "&action=LOGOUT";
        var response = sendRequest(logoutBody);
        int statusCode = response.getStatusCode();
        
        assertEquals(403, statusCode, "Статус код должен быть 403");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест с неверным форматом токена (меньше 32 символов)")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("TEST-009")
    void testInvalidTokenFormat() {
        String shortToken = "ABC123";
        String body = "token=" + shortToken + "&action=LOGIN";
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(400, statusCode, "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест с неверным форматом токена (символы не A-Z0-9)")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("TEST-010")
    void testInvalidTokenCharacters() {
        String invalidCharsToken = "ABC!@#$$%^&*()1234567890123456";
        String body = "token=" + invalidCharsToken + "&action=LOGIN";
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(400, statusCode, "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест с несуществующим действием")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("TEST-011")
    void testUnknownAction() {
        String body = "token=" + validToken + "&action=UNKNOWN";
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(400, statusCode, "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест с пустым токеном")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-012")
    void testEmptyToken() {
        String body = "token=&action=LOGIN";
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(400, statusCode, "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    @Test
    @Description("Тест с пустым action")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TEST-013")
    void testEmptyAction() {
        String body = "token=" + validToken + "&action=";
        var response = sendRequest(body);
        int statusCode = response.getStatusCode();
        
        assertEquals(400, statusCode, "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "Результат должен быть ERROR");
    }

    private io.restassured.response.Response sendRequest(String body) {
        return given()
            .baseUri("http://localhost:8080")
            .basePath("/endpoint")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("X-Api-Key", API_KEY)
            .body(body)
            .post();
    }

    private io.restassured.response.Response sendRequestWithoutApiKey(String body) {
        return given()
            .baseUri("http://localhost:8080")
            .basePath("/endpoint")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .body(body)
            .post();
    }

    private io.restassured.response.Response sendRequestWithWrongApiKey(String body) {
        return given()
            .baseUri("http://localhost:8080")
            .basePath("/endpoint")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("X-Api-Key", "wrong-api-key")
            .body(body)
            .post();
    }
}
