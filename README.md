### Тестовое задание NordCodes:
Реализовано 13 тестов, которые прошли успешно:
-  testLoginSuccess  - Успешная аутентификация
-  testLoginWithWrongApiKey  - Ошибка при неверном API ключе (401)
-  testLoginWithoutApiKey  - Ошибка при отсутствии API ключа (400)
-  testActionAfterLogin  - Действие после авторизации (200)
-  testActionWithoutLogin  - Отказ в доступе без LOGIN (403)
-  testLoginFailure  - Ошибка внешнего сервиса (500)
-  testLogoutAfterLogin  - Завершение сессии после LOGIN (200)
-  testLogoutWithoutLogin  - Отказ в LOGOUT без LOGIN (403)
-  testLoginWithInvalidTokenLength  - Неверная длина токена (400)
-  testLoginWithInvalidTokenChars  - Неверные символы в токене (400)
-  testLoginWithInvalidTokenMixedCase  - MIXEDCASE недопустим (400)
-  testActionWithUnknownAction  - Неизвестное действие (400)
-  testLoginWithEmptyTokenAndAction  - Пустые поля (400)

### Запуск приложения
Тестируемое приложение - классический веб-сервис на Spring Boot. Для запуска необходима Java 17 или выше:
```
# 1. Запустить WireMock
java -jar /tmp/wiremock-standalone.jar --port 8888 --container-threads 20
 
# 2. Запустить тестируемое приложение
java -jar -Dsecret=qazWSXedc -Dmock=http://localhost:8888/ internal-0.0.1-SNAPSHOT.jar
 
# 3. Запустить тесты
cd tests && mvn clean test allure:report
```

### Принцип работы тестов
- Выполняют HTTP-запросы к уже запущенному приложению на  http://localhost:8080/endpoint 
- Используя RestAssured, отправляют запросы с заголовками и телом
- Приложение, получая запрос, обращается к моку на порту 8888
- WireMock возвращает заранее настроенные ответы (200, 500 и т.д.)
- Тесты проверяют ответ приложения и то, что приложение правильно взаимодействовало с моком
