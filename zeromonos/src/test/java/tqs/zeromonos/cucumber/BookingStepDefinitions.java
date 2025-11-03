package tqs.zeromonos.cucumber;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import tqs.zeromonos.data.BookingRepository;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;

public class BookingStepDefinitions {

    @Autowired
    private CucumberSpringConfiguration springConfig;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    private Response response;
    private String savedToken;
    private LocalDate tomorrow;

    @Before
    public void setUp() {
        int port = springConfig.getPort();
        RestAssured.baseURI = "http://localhost:" + port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Limpar dados de teste
        bookingRepository.deleteAll();
        municipalityRepository.deleteAll();

        // Criar municípios padrão
        createMunicipalityIfNotExists("Lisboa");
        createMunicipalityIfNotExists("Porto");
        createMunicipalityIfNotExists("Aveiro");

        // Calcular datas
        tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek() == DayOfWeek.SUNDAY) {
            tomorrow = tomorrow.plusDays(1);
        }
    }

    private void createMunicipalityIfNotExists(String name) {
        if (municipalityRepository.findByName(name).isEmpty()) {
            Municipality municipality = new Municipality(name);
            municipalityRepository.save(municipality);
        }
    }

    private LocalDate getNextSunday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    // ==================== GIVEN ====================

    @Given("que o sistema está disponível")
    public void queOSistemaEstaDisponivel() {
        // Sistema já está disponível através do SpringBootTest
    }

    @Given("que existe um município chamado {string}")
    public void queExisteUmMunicipioChamado(String municipalityName) {
        createMunicipalityIfNotExists(municipalityName);
    }

    @Given("que a data de amanhã não é domingo")
    public void queADataDeAmanhaNaoEDomingo() {
        // Já calculado no setUp
        assertNotEquals(DayOfWeek.SUNDAY, tomorrow.getDayOfWeek());
    }

    @Given("que existe um agendamento com token {string}")
    public void queExisteUmAgendamentoComToken(String token) {
        Municipality lisboa = municipalityRepository.findByName("Lisboa")
                .orElseGet(() -> {
                    Municipality m = new Municipality("Lisboa");
                    municipalityRepository.save(m);
                    return m;
                });

        tqs.zeromonos.data.Booking booking = new tqs.zeromonos.data.Booking(
                lisboa, "Teste", tomorrow, tqs.zeromonos.data.TimeSlot.MORNING);
        // Não podemos definir o token diretamente, então criamos e procuramos
        bookingRepository.save(booking);
        savedToken = booking.getToken();
    }

    @Given("que existe um agendamento cancelável com token {string}")
    public void queExisteUmAgendamentoCancelavelComToken(String token) {
        Municipality lisboa = municipalityRepository.findByName("Lisboa")
                .orElseGet(() -> {
                    Municipality m = new Municipality("Lisboa");
                    municipalityRepository.save(m);
                    return m;
                });

        tqs.zeromonos.data.Booking booking = new tqs.zeromonos.data.Booking(
                lisboa, "Teste Cancelável", tomorrow, tqs.zeromonos.data.TimeSlot.MORNING);
        bookingRepository.save(booking);
        savedToken = booking.getToken();
    }

    @Given("que existe um agendamento cancelado com token {string}")
    public void queExisteUmAgendamentoCanceladoComToken(String token) {
        Municipality lisboa = municipalityRepository.findByName("Lisboa")
                .orElseGet(() -> {
                    Municipality m = new Municipality("Lisboa");
                    municipalityRepository.save(m);
                    return m;
                });

        tqs.zeromonos.data.Booking booking = new tqs.zeromonos.data.Booking(
                lisboa, "Teste Cancelado", tomorrow, tqs.zeromonos.data.TimeSlot.MORNING);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        savedToken = booking.getToken();
    }

    @Given("que existem agendamentos no sistema")
    public void queExistemAgendamentosNoSistema() {
        Municipality lisboa = municipalityRepository.findByName("Lisboa")
                .orElseGet(() -> {
                    Municipality m = new Municipality("Lisboa");
                    municipalityRepository.save(m);
                    return m;
                });

        tqs.zeromonos.data.Booking booking1 = new tqs.zeromonos.data.Booking(
                lisboa, "Agendamento 1", tomorrow, tqs.zeromonos.data.TimeSlot.MORNING);
        tqs.zeromonos.data.Booking booking2 = new tqs.zeromonos.data.Booking(
                lisboa, "Agendamento 2", tomorrow.plusDays(1), tqs.zeromonos.data.TimeSlot.AFTERNOON);

        bookingRepository.save(booking1);
        bookingRepository.save(booking2);
    }

    @Given("que existem agendamentos para {string}")
    public void queExistemAgendamentosPara(String municipalityName) {
        Municipality municipality = municipalityRepository.findByName(municipalityName)
                .orElseGet(() -> {
                    Municipality m = new Municipality(municipalityName);
                    municipalityRepository.save(m);
                    return m;
                });

        tqs.zeromonos.data.Booking booking = new tqs.zeromonos.data.Booking(
                municipality, "Agendamento para " + municipalityName, tomorrow,
                tqs.zeromonos.data.TimeSlot.MORNING);
        bookingRepository.save(booking);
    }

    // ==================== WHEN ====================

    @When("faço uma requisição GET para {string}")
    public void facoUmaRequisicaoGETPara(String endpoint) {
        response = given()
                .contentType(ContentType.JSON)
                .when()
                .get(endpoint);
    }

    @When("faço uma requisição GET para {string} com parâmetro {string}")
    public void facoUmaRequisicaoGETParaComParametro(String endpoint, String param) {
        String[] parts = param.split("=");
        String key = parts[0];
        String value = parts[1];

        response = given()
                .contentType(ContentType.JSON)
                .queryParam(key, value)
                .when()
                .get(endpoint);
    }

    @When("crio um agendamento com os seguintes dados:")
    public void crioUmAgendamentoComOsSeguintesDados(io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String municipality = data.get("município");
        String description = data.get("descrição");
        String dateStr = data.get("data solicitada");
        String timeSlot = data.get("período");

        LocalDate requestedDate;
        if ("amanhã".equals(dateStr)) {
            requestedDate = tomorrow;
        } else {
            requestedDate = LocalDate.parse(dateStr);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("municipalityName", municipality);
        requestBody.put("description", description);
        requestBody.put("requestedDate", requestedDate.toString());
        requestBody.put("timeSlot", timeSlot);

        response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings");

        if (response.getStatusCode() == 200) {
            savedToken = response.jsonPath().getString("token");
        }
    }

    @When("tento criar um agendamento com os seguintes dados:")
    public void tentoCriarUmAgendamentoComOsSeguintesDados(io.cucumber.datatable.DataTable dataTable) {
        crioUmAgendamentoComOsSeguintesDados(dataTable);
    }

    @When("tento criar um agendamento para domingo")
    public void tentoCriarUmAgendamentoParaDomingo() {
        LocalDate sunday = getNextSunday();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("municipalityName", "Lisboa");
        requestBody.put("description", "Teste Domingo");
        requestBody.put("requestedDate", sunday.toString());
        requestBody.put("timeSlot", "MORNING");

        response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/bookings");
    }

    @When("faço uma requisição PUT para {string}")
    public void facoUmaRequisicaoPUTPara(String endpoint) {
        response = given()
                .contentType(ContentType.JSON)
                .when()
                .put(endpoint);
    }

    @When("faço uma requisição PATCH para {string} com status {string}")
    public void facoUmaRequisicaoPATCHParaComStatus(String endpoint, String status) {
        response = given()
                .contentType(ContentType.JSON)
                .queryParam("status", status)
                .when()
                .patch(endpoint);
    }

    @When("guardo o token do agendamento")
    public void guardoOTokenDoAgendamento() {
        // Token já foi guardado quando criamos o agendamento
        assertNotNull(savedToken, "Token deve estar guardado após criar agendamento");
    }

    @When("atualizo o status do agendamento para {string}")
    public void atualizoOStatusDoAgendamentoPara(String status) {
        String endpoint = "/api/staff/bookings/" + savedToken + "/status";
        facoUmaRequisicaoPATCHParaComStatus(endpoint, status);
    }

    @When("consulto o agendamento pelo token guardado")
    public void consultoOAgendamentoPeloTokenGuardado() {
        facoUmaRequisicaoGETPara("/api/bookings/" + savedToken);
    }

    @When("consulto o agendamento pelo token {string}")
    public void consultoOAgendamentoPeloToken(String token) {
        facoUmaRequisicaoGETPara("/api/bookings/" + token);
    }

    // ==================== THEN ====================

    @Then("o status da resposta deve ser {int}")
    public void oStatusDaRespostaDeveSer(int statusCode) {
        response.then().statusCode(statusCode);
    }

    @Then("a resposta deve conter uma lista de municípios")
    public void aRespostaDeveConterUmaListaDeMunicipios() {
        List<String> municipalities = response.jsonPath().getList(".", String.class);
        assertNotNull(municipalities);
        assertFalse(municipalities.isEmpty());
    }

    @Then("a lista deve conter pelo menos {string} e {string}")
    public void aListaDeveConterPeloMenosE(String item1, String item2) {
        List<String> items = response.jsonPath().getList(".", String.class);
        assertTrue(items.contains(item1), "Lista deve conter " + item1);
        assertTrue(items.contains(item2), "Lista deve conter " + item2);
    }

    @Then("a resposta deve conter um token")
    public void aRespostaDeveConterUmToken() {
        response.then().body("token", notNullValue());
        savedToken = response.jsonPath().getString("token");
    }

    @Then("o status do agendamento deve ser {string}")
    public void oStatusDoAgendamentoDeveSer(String status) {
        response.then().body("status", equalTo(status));
    }

    @Then("o município deve ser {string}")
    public void oMunicipioDeveSer(String municipality) {
        response.then().body("municipalityName", equalTo(municipality));
    }

    @Then("a mensagem de erro deve conter {string}")
    public void aMensagemDeErroDeveConter(String expectedMessage) {
        String actualMessage = response.jsonPath().getString("message");
        assertNotNull(actualMessage, "Mensagem de erro não deve ser null");
        assertTrue(actualMessage.contains(expectedMessage),
                "Mensagem deve conter '" + expectedMessage + "', mas era: " + actualMessage);
    }

    @Then("o token na resposta deve ser {string}")
    public void oTokenNaRespostaDeveSer(String expectedToken) {
        response.then().body("token", equalTo(expectedToken));
    }

    @Then("a resposta deve conter uma lista de agendamentos")
    public void aRespostaDeveConterUmaListaDeAgendamentos() {
        List<?> bookings = response.jsonPath().getList(".");
        assertNotNull(bookings);
    }

    @Then("a lista deve ter pelo menos {int} agendamento")
    public void aListaDeveTerPeloMenosAgendamento(int minCount) {
        List<?> bookings = response.jsonPath().getList(".");
        assertTrue(bookings.size() >= minCount,
                "Lista deve ter pelo menos " + minCount + " agendamentos, mas tinha " + bookings.size());
    }

    @Then("todos os agendamentos devem ser do município {string}")
    public void todosOsAgendamentosDevemSerDoMunicipio(String municipalityName) {
        List<Map<String, Object>> bookings = response.jsonPath().getList(".");
        for (Map<String, Object> booking : bookings) {
            assertEquals(municipalityName, booking.get("municipalityName"),
                    "Todos os agendamentos devem ser do município " + municipalityName);
        }
    }

    @Then("o status do agendamento na resposta deve ser {string}")
    public void oStatusDoAgendamentoNaRespostaDeveSer(String status) {
        response.then().body("status", equalTo(status));
    }

    @Then("o histórico deve conter pelo menos {int} mudanças de estado")
    public void oHistoricoDeveConterPeloMenosMudancasDeEstado(int minChanges) {
        List<String> history = response.jsonPath().getList("history", String.class);
        assertNotNull(history, "Histórico não deve ser null");
        assertTrue(history.size() >= minChanges,
                "Histórico deve ter pelo menos " + minChanges + " mudanças, mas tinha " + history.size());
    }

    @Then("que o status do agendamento foi atualizado para {string}")
    public void queOStatusDoAgendamentoFoiAtualizadoPara(String status) {
        // Este step é executado antes de atualizar novamente
        // O status já foi atualizado no step anterior
    }
}
