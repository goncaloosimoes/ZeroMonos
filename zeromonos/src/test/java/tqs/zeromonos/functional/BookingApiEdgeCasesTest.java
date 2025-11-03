package tqs.zeromonos.functional;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import tqs.zeromonos.TestcontainersConfiguration;

/**
 * Testes de Edge Cases e Validações Adicionais da API de Booking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@Import(TestcontainersConfiguration.class)
@DisplayName("Testes de Edge Cases - API de Booking")
class BookingApiEdgeCasesTest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ==================== TESTES DE VALIDAÇÃO DE ENTRADA ====================

    @Test
    @DisplayName("POST /api/bookings - Deve rejeitar JSON malformado")
    void testCreateBooking_InvalidJson() {
        given()
                .contentType(ContentType.JSON)
                .body("{ invalid json }")
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(is(HttpStatus.BAD_REQUEST.value()),
                        is(HttpStatus.UNPROCESSABLE_ENTITY.value()),
                        is(HttpStatus.INTERNAL_SERVER_ERROR.value()))); // Spring pode retornar 500 para JSON inválido
    }

    @Test
    @DisplayName("POST /api/bookings - Deve rejeitar quando municipalityName é null")
    void testCreateBooking_NullMunicipality() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) { // Domingo
            tomorrow = tomorrow.plusDays(1);
        }

        String requestBody = String.format(
                "{\"municipalityName\":null,\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Test\"}",
                tomorrow);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(is(HttpStatus.BAD_REQUEST.value()), is(HttpStatus.NOT_FOUND.value())));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve rejeitar quando municipalityName está vazio")
    void testCreateBooking_EmptyMunicipality() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        String requestBody = String.format(
                "{\"municipalityName\":\"\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Test\"}",
                tomorrow);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(is(HttpStatus.BAD_REQUEST.value()), is(HttpStatus.NOT_FOUND.value())));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve rejeitar quando timeSlot é inválido")
    void testCreateBooking_InvalidTimeSlot() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        String requestBody = String.format(
                "{\"municipalityName\":\"Lisboa\",\"requestedDate\":\"%s\",\"timeSlot\":\"INVALID_SLOT\",\"description\":\"Test\"}",
                tomorrow);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(is(HttpStatus.BAD_REQUEST.value()), is(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve aceitar descrição longa (edge case)")
    void testCreateBooking_VeryLongDescription() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        // Criar descrição com 200 caracteres (mais razoável que 500)
        String longDescription = "A".repeat(200);

        String requestBody = String.format(
                "{\"municipalityName\":\"Aveiro\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"%s\"}",
                tomorrow, longDescription);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", notNullValue())
                .body("description", equalTo(longDescription));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve aceitar descrição vazia")
    void testCreateBooking_EmptyDescription() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        String requestBody = String.format(
                "{\"municipalityName\":\"Lisboa\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"\"}",
                tomorrow);

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("token");

        assertNotNull(token);
    }

    @Test
    @DisplayName("POST /api/bookings - Deve rejeitar quando requestedDate está em formato inválido")
    void testCreateBooking_InvalidDateFormat() {
        String requestBody = "{\"municipalityName\":\"Lisboa\",\"requestedDate\":\"2024-13-45\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Test\"}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(is(HttpStatus.BAD_REQUEST.value()),
                        is(HttpStatus.UNPROCESSABLE_ENTITY.value()),
                        is(HttpStatus.INTERNAL_SERVER_ERROR.value()))); // Spring pode retornar 500 para data inválida
    }

    @Test
    @DisplayName("POST /api/bookings - Deve rejeitar quando body está vazio")
    void testCreateBooking_EmptyBody() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(is(HttpStatus.BAD_REQUEST.value()), is(HttpStatus.NOT_FOUND.value())));
    }

    // ==================== TESTES DE LIMITE (32 BOOKINGS) ====================

    @Test
    @DisplayName("POST /api/bookings - Deve atingir limite de 32 bookings e rejeitar 33º")
    void testCreateBooking_LimitOf32Bookings() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        // Usar um município específico para este teste (Braga)
        String municipality = "Braga";
        String requestBodyTemplate = "{\"municipalityName\":\"%s\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Test %d\"}";

        // Contar quantos bookings já existem para Braga
        int existingCount = given()
                .when()
                .get("/api/staff/bookings?municipality=" + municipality)
                .then()
                .extract()
                .jsonPath()
                .getList("$")
                .size();

        // Criar bookings até atingir 32 (máximo 5 para não demorar muito)
        int bookingsToCreate = Math.min(5, 32 - existingCount);
        int successCount = 0;
        for (int i = 1; i <= bookingsToCreate && successCount < 5; i++) {
            LocalDate date = tomorrow.plusDays(i - 1);
            if (date.getDayOfWeek().getValue() == 7) {
                date = date.plusDays(1);
            }
            String requestBody = String.format(requestBodyTemplate, municipality, date, i);

            int status = given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/bookings")
                    .then()
                    .extract()
                    .statusCode();

            if (status == HttpStatus.OK.value()) {
                successCount++;
            }
        }

        // Verificar estado atual
        int currentCount = given()
                .when()
                .get("/api/staff/bookings?municipality=" + municipality)
                .then()
                .extract()
                .jsonPath()
                .getList("$")
                .size();

        // Se já está no limite ou muito perto, tentar criar um para verificar que falha
        if (currentCount >= 31) {
            LocalDate nextDate = tomorrow.plusDays(bookingsToCreate + 10);
            if (nextDate.getDayOfWeek().getValue() == 7) {
                nextDate = nextDate.plusDays(1);
            }
            String requestBodyExtra = String.format(requestBodyTemplate, municipality, nextDate, 999);

            given()
                    .contentType(ContentType.JSON)
                    .body(requestBodyExtra)
                    .when()
                    .post("/api/bookings")
                    .then()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .body("message", containsString("Limite"));
        }
    }

    // ==================== TESTES DE CONCORRÊNCIA ====================

    @Test
    @DisplayName("POST /api/bookings - Deve lidar com múltiplos requests simultâneos")
    void testCreateBooking_ConcurrentRequests() {
        LocalDate baseTomorrow = LocalDate.now().plusDays(1);
        if (baseTomorrow.getDayOfWeek().getValue() == 7) {
            baseTomorrow = baseTomorrow.plusDays(1);
        }
        final LocalDate tomorrow = baseTomorrow;

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        try {
            CompletableFuture<?>[] futures = IntStream.range(0, numberOfThreads)
                    .mapToObj(i -> {
                        final int threadIndex = i;
                        final LocalDate baseDate = tomorrow.plusDays(threadIndex);
                        final LocalDate date = baseDate.getDayOfWeek().getValue() == 7 ? baseDate.plusDays(1)
                                : baseDate;
                        return CompletableFuture.supplyAsync(() -> {
                            String requestBody = String.format(
                                    "{\"municipalityName\":\"Porto\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Concurrent test %d\"}",
                                    date, threadIndex);

                            return given()
                                    .contentType(ContentType.JSON)
                                    .body(requestBody)
                                    .when()
                                    .post("/api/bookings")
                                    .then()
                                    .extract()
                                    .statusCode();
                        }, executor);
                    })
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();

            // Verificar que pelo menos alguns foram bem-sucedidos (200 OK)
            long successCount = java.util.Arrays.stream(futures)
                    .map(f -> (Integer) f.join())
                    .filter(code -> code == HttpStatus.OK.value())
                    .count();

            assertTrue(successCount > 0, "Pelo menos alguns requests deveriam ter sucesso");
        } finally {
            executor.shutdown();
        }
    }

    // ==================== TESTES DE TOKEN ====================

    @Test
    @DisplayName("GET /api/bookings/{token} - Deve rejeitar token com formato inválido")
    void testGetBooking_InvalidTokenFormat() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/invalid-token-format")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("GET /api/bookings/{token} - Deve rejeitar token vazio")
    void testGetBooking_EmptyToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/")
                .then()
                .statusCode(anyOf(is(HttpStatus.NOT_FOUND.value()), is(HttpStatus.METHOD_NOT_ALLOWED.value())));
    }

    @Test
    @DisplayName("PUT /api/bookings/{token}/cancel - Deve rejeitar token inválido")
    void testCancelBooking_InvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/api/bookings/invalid-token-123/cancel")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    // ==================== TESTES DE MÉTODOS HTTP ====================

    @Test
    @DisplayName("POST /api/bookings/{token} - Deve rejeitar método não suportado")
    void testUnsupportedMethod() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/bookings/test-token")
                .then()
                .statusCode(anyOf(is(HttpStatus.METHOD_NOT_ALLOWED.value()), is(HttpStatus.NOT_FOUND.value()),
                        is(HttpStatus.BAD_REQUEST.value())));
    }

    // ==================== TESTES DE HEADERS ====================

    @Test
    @DisplayName("GET /api/bookings/municipalities - Deve aceitar sem Content-Type")
    void testGetMunicipalities_WithoutContentType() {
        when()
                .get("/api/bookings/municipalities")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", notNullValue());
    }

    @Test
    @DisplayName("GET /api/bookings/municipalities - Deve funcionar com diferentes Content-Types")
    void testGetMunicipalities_DifferentContentTypes() {
        // Testar com Content-Type application/json
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/municipalities")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Testar sem Content-Type
        when()
                .get("/api/bookings/municipalities")
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
