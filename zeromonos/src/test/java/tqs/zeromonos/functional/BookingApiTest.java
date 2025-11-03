package tqs.zeromonos.functional;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import tqs.zeromonos.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@Import(TestcontainersConfiguration.class)
@DisplayName("Testes de Integração da API de Booking")
class BookingApiTest {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ==================== TESTES DE MUNICÍPIOS ====================

    @Test
    @Order(1)
    @DisplayName("GET /api/bookings/municipalities - Listar municípios")
    void testGetAvailableMunicipalities() {
        List<String> municipalities = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/municipalities")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", String.class);

        assertNotNull(municipalities);
        assertFalse(municipalities.isEmpty());
    }

    // ==================== TESTES DE CRIAÇÃO DE BOOKING ====================

    @Test
    @Order(2)
    @DisplayName("POST /api/bookings - Criar booking válido")
    void testCreateValidBooking() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // Se for domingo, avançar para segunda-feira
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Lisboa",
                "description", "Sofá velho e colchão",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "AFTERNOON");

        Map<String, Object> response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", notNullValue())
                .body("municipalityName", equalTo("Lisboa"))
                .body("description", equalTo("Sofá velho e colchão"))
                .body("requestedDate", equalTo(tomorrow.toString()))
                .body("timeSlot", equalTo("AFTERNOON"))
                .body("status", equalTo("RECEIVED"))
                .body("history", not(empty()))
                .extract()
                .jsonPath()
                .getMap(".");

        assertNotNull(response.get("token"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/bookings - Erro ao criar com município inexistente")
    void testCreateBookingWithInvalidMunicipality() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "MunicipioInexistenteXYZ",
                "description", "Teste",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "MORNING");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("não encontrado"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/bookings - Erro ao criar com data no passado")
    void testCreateBookingWithPastDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Lisboa",
                "description", "Teste",
                "requestedDate", yesterday.toString(),
                "timeSlot", "MORNING");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("passado"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/bookings - Erro ao criar com data hoje")
    void testCreateBookingWithTodayDate() {
        LocalDate today = LocalDate.now();

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Lisboa",
                "description", "Teste",
                "requestedDate", today.toString(),
                "timeSlot", "MORNING");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("mesmo dia"));
    }

    // ==================== TESTES DE CONSULTA DE BOOKING ====================

    @Test
    @Order(6)
    @DisplayName("GET /api/bookings/{token} - Consultar booking existente")
    void testGetBookingByToken() {
        // Primeiro criar um booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Porto",
                "description", "Mesa de jantar",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "EVENING");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("token");

        // Agora consultar o booking criado
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/" + token)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", equalTo(token))
                .body("municipalityName", equalTo("Porto"))
                .body("description", equalTo("Mesa de jantar"))
                .body("timeSlot", equalTo("EVENING"))
                .body("status", equalTo("RECEIVED"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/bookings/{token} - Erro ao consultar token inexistente")
    void testGetBookingWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", containsString("não encontrado"));
    }

    // ==================== TESTES DE CANCELAMENTO ====================

    @Test
    @Order(8)
    @DisplayName("PUT /api/bookings/{token}/cancel - Cancelar booking válido")
    void testCancelBooking() {
        // Criar um booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Braga",
                "description", "Cadeiras velhas",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "NIGHT");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("token");

        // Cancelar o booking
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/api/bookings/" + token + "/cancel")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verificar que o status foi alterado para CANCELLED
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/" + token)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("CANCELLED"));
    }

    // ==================== TESTES DO PAINEL STAFF ====================

    @Test
    @Order(9)
    @DisplayName("GET /api/staff/bookings - Listar todos os bookings")
    void testListAllBookings() {
        @SuppressWarnings("rawtypes")
        List<Map> bookings = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/staff/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", Map.class);

        assertNotNull(bookings);
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/staff/bookings?municipality=X - Listar bookings por município")
    void testListBookingsByMunicipality() {
        @SuppressWarnings("rawtypes")
        List<Map> bookings = given()
                .contentType(ContentType.JSON)
                .queryParam("municipality", "Lisboa")
                .when()
                .get("/api/staff/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", Map.class);

        assertNotNull(bookings);
    }

    @Test
    @Order(11)
    @DisplayName("PATCH /api/staff/bookings/{token}/status - Atualizar status do booking")
    void testUpdateBookingStatus() {
        // Criar um booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Coimbra",
                "description", "Frigorífico antigo",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "MORNING");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("token");

        // Atualizar status para ASSIGNED
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "ASSIGNED")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("ASSIGNED"));

        // Verificar que o histórico foi atualizado
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/" + token)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("history", hasSize(greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(12)
    @DisplayName("Fluxo completo: criar, consultar, atualizar status e cancelar")
    void testCompleteBookingWorkflow() {
        // 1. Criar booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Aveiro",
                "description", "Geladeira e fogão velhos",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "ANYTIME");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("RECEIVED"))
                .extract()
                .jsonPath()
                .getString("token");

        // 2. Consultar booking
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/" + token)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("token", equalTo(token))
                .body("status", equalTo("RECEIVED"));

        // 3. Atualizar para ASSIGNED
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "ASSIGNED")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("ASSIGNED"));

        // 4. Atualizar para IN_PROGRESS
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "IN_PROGRESS")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("IN_PROGRESS"));

        // 5. Atualizar para COMPLETED
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "COMPLETED")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("COMPLETED"));

        // 6. Verificar histórico final
        Map<String, Object> finalBooking = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bookings/" + token)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getMap(".");

        @SuppressWarnings("unchecked")
        List<String> history = (List<String>) finalBooking.get("history");
        assertTrue(history.size() >= 4); // RECEIVED, ASSIGNED, IN_PROGRESS, COMPLETED
    }

    // ==================== TESTES ADICIONAIS DE CASOS DE ERRO ====================

    @Test
    @Order(13)
    @DisplayName("PUT /api/bookings/{token}/cancel - Erro ao cancelar booking inexistente")
    void testCancelNonExistentBooking() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/api/bookings/00000000-0000-0000-0000-000000000000/cancel")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", containsString("não encontrado"));
    }

    @Test
    @Order(14)
    @DisplayName("PUT /api/bookings/{token}/cancel - Erro ao cancelar booking já COMPLETED")
    void testCancelCompletedBooking() {
        // Criar um booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Faro",
                "description", "Armário antigo",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "AFTERNOON");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("token");

        // Atualizar para COMPLETED
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "COMPLETED")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Tentar cancelar - deve falhar
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/api/bookings/" + token + "/cancel")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", containsString("não pode ser cancelado"));
    }

    @Test
    @Order(15)
    @DisplayName("POST /api/bookings - Erro ao criar com data domingo")
    void testCreateBookingWithSundayDate() {
        LocalDate sunday = LocalDate.now();
        // Encontrar o próximo domingo
        while (sunday.getDayOfWeek().getValue() != 7) {
            sunday = sunday.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Lisboa",
                "description", "Teste",
                "requestedDate", sunday.toString(),
                "timeSlot", "MORNING");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", containsString("fim de semana"));
    }

    @Test
    @Order(16)
    @DisplayName("POST /api/bookings - Erro ao criar com dados inválidos (campos obrigatórios)")
    void testCreateBookingWithMissingFields() {
        // Teste sem municipalityName
        Map<String, Object> requestBody1 = Map.of(
                "description", "Teste",
                "requestedDate", LocalDate.now().plusDays(2).toString(),
                "timeSlot", "MORNING");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody1)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(anyOf(equalTo(HttpStatus.BAD_REQUEST.value()),
                        equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    @Test
    @Order(17)
    @DisplayName("PATCH /api/staff/bookings/{token}/status - Erro ao atualizar booking inexistente")
    void testUpdateStatusOfNonExistentBooking() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "ASSIGNED")
                .when()
                .patch("/api/staff/bookings/00000000-0000-0000-0000-000000000000/status")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", containsString("não encontrado"));
    }

    @Test
    @Order(18)
    @DisplayName("PATCH /api/staff/bookings/{token}/status - Erro sem parâmetro status")
    void testUpdateStatusWithoutStatusParameter() {
        // Criar um booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Setúbal",
                "description", "Máquina de lavar",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "EVENING");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("token");

        // Tentar atualizar sem parâmetro status - deve falhar
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(anyOf(equalTo(HttpStatus.BAD_REQUEST.value()),
                        equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    @Test
    @Order(19)
    @DisplayName("GET /api/staff/bookings?municipality=all - Listar todos explicitamente")
    void testListBookingsWithAllParameter() {
        @SuppressWarnings("rawtypes")
        List<Map> bookings = given()
                .contentType(ContentType.JSON)
                .queryParam("municipality", "all")
                .when()
                .get("/api/staff/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", Map.class);

        assertNotNull(bookings);
    }

    @Test
    @Order(20)
    @DisplayName("GET /api/staff/bookings?municipality=X - Erro com município inexistente")
    void testListBookingsWithInvalidMunicipality() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("municipality", "MunicipioInexistenteXYZ")
                .when()
                .get("/api/staff/bookings")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", containsString("Município não encontrado"));
    }

    @Test
    @Order(21)
    @DisplayName("PUT /api/bookings/{token}/cancel - Erro ao cancelar booking já IN_PROGRESS")
    void testCancelInProgressBooking() {
        // Criar um booking
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        Map<String, Object> requestBody = Map.of(
                "municipalityName", "Bragança",
                "description", "Mesa antiga",
                "requestedDate", tomorrow.toString(),
                "timeSlot", "MORNING");

        String token = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("token");

        // Atualizar para IN_PROGRESS
        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "ASSIGNED")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(ContentType.JSON)
                .queryParam("status", "IN_PROGRESS")
                .when()
                .patch("/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Tentar cancelar - deve falhar
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/api/bookings/" + token + "/cancel")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", containsString("não pode ser cancelado"));
    }
}
