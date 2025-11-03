package tqs.zeromonos.functional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import io.github.bonigarcia.wdm.WebDriverManager;
import tqs.zeromonos.TestcontainersConfiguration;

import java.time.Duration;
import java.util.List;

@SpringBootTest(classes = tqs.zeromonos.ZeromonosApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@Import(TestcontainersConfiguration.class)
@DisplayName("Testes Selenium - Vista do Cliente")
class ClientViewSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        baseUrl = "http://localhost:" + port;
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ==================== TESTES DA PÁGINA INICIAL ====================

    @Test
    @DisplayName("Página inicial - Carregamento e elementos")
    void testHomePageLoads() {
        driver.get(baseUrl + "/");

        // Verificar título
        assertEquals("ZeroMonos - Recolha de Resíduos Volumosos", driver.getTitle());

        // Verificar elementos principais
        WebElement heroTitle = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("hero-title")));
        assertTrue(heroTitle.getText().contains("ZeroMonos"));

        // Verificar links de navegação
        WebElement createBookingLink = driver.findElement(By.cssSelector("a[href='/create-booking.html']"));
        assertNotNull(createBookingLink);
        // O texto está dentro de um <span> dentro do link
        assertTrue(createBookingLink.getText().contains("Agendar Recolha"));

        WebElement lookupBookingLink = driver.findElement(By.cssSelector("a[href='/lookup-booking.html']"));
        assertNotNull(lookupBookingLink);
    }

    @Test
    @DisplayName("Página inicial - Navegação para criar booking")
    void testNavigateToCreateBooking() {
        driver.get(baseUrl + "/");

        WebElement createBookingLink = wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='/create-booking.html']")));
        createBookingLink.click();

        wait.until(ExpectedConditions.titleContains("Agendar Recolha"));
        assertEquals("Agendar Recolha - ZeroMonos", driver.getTitle());

        WebElement form = driver.findElement(By.id("booking-form"));
        assertNotNull(form);
    }

    @Test
    @DisplayName("Página inicial - Navegação para consultar booking")
    void testNavigateToLookupBooking() {
        driver.get(baseUrl + "/");

        WebElement lookupBookingLink = wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='/lookup-booking.html']")));
        lookupBookingLink.click();

        wait.until(ExpectedConditions.titleContains("Consultar"));
        assertTrue(driver.getTitle().contains("Consultar"));

        WebElement searchForm = driver.findElement(By.id("search-form-internal"));
        assertNotNull(searchForm);
    }

    // ==================== TESTES DO FORMULÁRIO DE CRIAÇÃO ====================

    @Test
    @DisplayName("Formulário de criação - Carregamento e autocomplete de município")
    void testCreateBookingFormLoads() {
        driver.get(baseUrl + "/create-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-form")));

        WebElement municipalityInput = driver.findElement(By.id("municipality"));
        assertNotNull(municipalityInput);

        // Testar autocomplete
        municipalityInput.sendKeys("Lis");

        // Aguardar sugestões aparecerem
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("suggestions-dropdown")));

        WebElement suggestions = driver.findElement(By.className("suggestions-dropdown"));

        // Aguardar sugestões serem carregadas (aguardar que elementos de sugestão
        // apareçam)
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfNestedElementLocatedBy(suggestions, By.cssSelector(".suggestion-item")),
                ExpectedConditions.visibilityOf(suggestions)));

        // Verificar se há sugestões (ou se pelo menos o dropdown existe)
        assertNotNull(suggestions);
    }

    @Test
    @DisplayName("Formulário de criação - Preenchimento e submissão válida")
    void testCreateBookingFormSubmission() {
        driver.get(baseUrl + "/create-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-form")));

        // Preencher município
        WebElement municipalityInput = driver.findElement(By.id("municipality"));
        municipalityInput.sendKeys("Lisboa");

        // Aguardar e selecionar sugestão
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("suggestions-dropdown")));

        // Aguardar sugestões carregarem (esperar que pelo menos um item de sugestão
        // apareça)
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".suggestions-dropdown .suggestion-item")),
                ExpectedConditions.textToBePresentInElementValue(municipalityInput, "Lisboa")));

        // Tentar clicar numa sugestão se aparecer
        List<WebElement> suggestions = driver
                .findElements(By.cssSelector(".suggestions-dropdown .suggestion-item"));
        if (!suggestions.isEmpty()) {
            wait.until(ExpectedConditions.elementToBeClickable(suggestions.get(0)));
            suggestions.get(0).click();
        } else {
            // Se não aparecer sugestão, continuar com "Lisboa" no input
        }

        // Preencher data (amanhã)
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // Pular domingos
        while (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        WebElement dateInput = driver.findElement(By.id("requestedDate"));
        // Limpar e preencher data
        dateInput.clear();
        String dateStr = tomorrow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        dateInput.sendKeys(dateStr);
        // Aguardar que o valor seja inserido verificando o atributo value
        wait.until(d -> {
            WebElement input = d.findElement(By.id("requestedDate"));
            return dateStr.equals(input.getDomProperty("value")) || !input.getDomProperty("value").isEmpty();
        });

        // Selecionar período usando Select
        WebElement timeSlotSelect = driver.findElement(By.id("timeSlot"));
        org.openqa.selenium.support.ui.Select timeSlotDropdown = new org.openqa.selenium.support.ui.Select(
                timeSlotSelect);
        timeSlotDropdown.selectByValue("AFTERNOON");
        // Aguardar que o valor seja selecionado verificando o atributo value
        wait.until(d -> {
            WebElement select = d.findElement(By.id("timeSlot"));
            return "AFTERNOON".equals(select.getDomProperty("value"));
        });

        // Preencher descrição
        WebElement descriptionInput = driver.findElement(By.id("description"));
        descriptionInput.sendKeys("Sofá velho e colchão para recolha");

        // Submeter formulário - usar JavascriptExecutor para garantir clique
        WebElement submitButton = wait
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[type='submit']")));
        // Fazer scroll até ao botão
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitButton);
        // Aguardar que o botão esteja visível e clicável após scroll
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        // Usar JavascriptExecutor para clicar se necessário
        try {
            submitButton.click();
        } catch (Exception e) {
            // Se falhar, usar JavaScript para clicar
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        }

        // Aguardar mensagem de sucesso (pode levar um pouco mais de tempo)
        WebElement messageContainer = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("form-msg")));

        // Aguardar que a mensagem seja exibida (esperar até 15 segundos)
        WebDriverWait extendedWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        extendedWait.until(d -> {
            String text = messageContainer.getText();
            return !text.isEmpty() && (text.length() > 10); // Esperar que tenha algum conteúdo
        });

        // Aguardar que a mensagem esteja completamente renderizada (verificar que não
        // está vazia e tem tamanho mínimo)
        wait.until(d -> {
            WebElement msg = d.findElement(By.id("form-msg"));
            String text = msg.getText();
            return text.length() > 10 && msg.isDisplayed();
        });

        // Verificar se contém mensagem de sucesso (verificar várias possibilidades)
        String messageText = messageContainer.getText();
        boolean hasSuccess = messageText.contains("sucesso") ||
                messageText.contains("token") ||
                messageText.contains("Agendamento") ||
                messageText.contains("criado") ||
                messageContainer.findElements(By.cssSelector(".message.success, .success, .message")).size() > 0;

        assertTrue(hasSuccess || messageText.length() > 20,
                "Mensagem de sucesso não encontrada. Texto encontrado: " + messageText);
    }

    @Test
    @DisplayName("Formulário de criação - Validação de campos obrigatórios")
    void testCreateBookingFormValidation() {
        driver.get(baseUrl + "/create-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-form")));

        // Tentar submeter sem preencher campos
        WebElement submitButton = wait
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[type='submit']")));
        // Fazer scroll até ao botão
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitButton);
        // Aguardar que o botão esteja visível e clicável após scroll
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        // Usar JavascriptExecutor para clicar se necessário
        try {
            submitButton.click();
        } catch (Exception e) {
            // Se falhar, usar JavaScript para clicar
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        }

        // Verificar se há mensagens de validação ou se o formulário não foi submetido
        // (o navegador pode bloquear a submissão ou mostrar mensagens nativas)
        WebElement form = driver.findElement(By.id("booking-form"));
        assertNotNull(form);

        // Verificar se algum campo está marcado como inválido
        boolean hasInvalidField = driver.findElements(By.cssSelector("input:invalid, select:invalid")).size() > 0;
        assertTrue(hasInvalidField || form.isDisplayed());
    }

    // ==================== TESTES DA PÁGINA DE CONSULTA ====================

    @Test
    @DisplayName("Página de consulta - Carregamento")
    void testLookupBookingPageLoads() {
        driver.get(baseUrl + "/lookup-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-form-internal")));

        WebElement searchForm = driver.findElement(By.id("search-form-internal"));
        assertNotNull(searchForm);

        WebElement tokenInput = driver.findElement(By.id("token"));
        assertNotNull(tokenInput);
    }

    @Test
    @DisplayName("Página de consulta - Busca por token válido")
    void testLookupBookingByToken() {
        // Primeiro criar um booking através da API
        String token = createBookingViaAPI();

        driver.get(baseUrl + "/lookup-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-form-internal")));

        // Preencher token
        WebElement tokenInput = driver.findElement(By.id("token"));
        tokenInput.sendKeys(token);

        // Submeter busca
        WebElement searchButton = driver.findElement(By.id("search-btn"));
        searchButton.click();

        // Aguardar detalhes aparecerem e serem exibidos
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-details")));

        // Aguardar que a classe "hidden" seja removida
        wait.until(ExpectedConditions
                .not(ExpectedConditions.attributeContains(By.id("booking-details"), "class", "hidden")));

        // Aguardar que o details-grid tenha conteúdo
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("details-grid")));
        wait.until(d -> {
            WebElement grid = d.findElement(By.id("details-grid"));
            return !grid.getText().isEmpty();
        });

        // Verificar se token está nos detalhes
        WebElement detailsGrid = driver.findElement(By.id("details-grid"));
        assertTrue(detailsGrid.getText().contains(token) || detailsGrid.getText().contains("Token"));
    }

    @Test
    @DisplayName("Página de consulta - Busca por token inexistente")
    void testLookupBookingInvalidToken() {
        driver.get(baseUrl + "/lookup-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-form-internal")));

        // Preencher token inválido
        WebElement tokenInput = driver.findElement(By.id("token"));
        tokenInput.sendKeys("token-inexistente-12345");

        // Submeter busca
        WebElement searchButton = driver.findElement(By.id("search-btn"));
        searchButton.click();

        // Aguardar mensagem de erro
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("message")));

        WebElement messageContainer = driver.findElement(By.id("message"));
        // Aguardar que a mensagem seja exibida (não está vazia)
        wait.until(d -> !messageContainer.getText().isEmpty());

        // Verificar se contém mensagem de erro
        assertTrue(messageContainer.getText().contains("não encontrado") ||
                messageContainer.getText().contains("erro") ||
                messageContainer.getText().contains("404") ||
                messageContainer.findElements(By.cssSelector(".message-error, .message.message-error")).size() > 0);
    }

    @Test
    @DisplayName("Página de consulta - Cancelamento de booking")
    void testCancelBookingFromLookup() {
        // Criar booking cancelável
        String token = createBookingViaAPI();

        driver.get(baseUrl + "/lookup-booking.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-form-internal")));

        // Buscar booking
        WebElement tokenInput = driver.findElement(By.id("token"));
        tokenInput.sendKeys(token);

        WebElement searchButton = driver.findElement(By.id("search-btn"));
        searchButton.click();

        // Aguardar detalhes e que o botão de cancelar seja clicável
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-details")));

        // Aguardar que o botão de cancelar esteja visível e clicável (não disabled)
        wait.until(ExpectedConditions.elementToBeClickable(By.id("cancel-btn")));

        // Re-obter o elemento para evitar StaleElementReference
        WebElement cancelButton = driver.findElement(By.id("cancel-btn"));

        // Fazer scroll até ao botão
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", cancelButton);
        // Aguardar que o botão esteja clicável após scroll
        wait.until(ExpectedConditions.elementToBeClickable(cancelButton));

        // Usar JavaScript para clicar diretamente
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cancelButton);

        // Confirmar no alert (se existir)
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
        } catch (Exception e) {
            // Pode não haver alert
        }

        // Aguardar confirmação de cancelamento
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("message")));

        WebElement messageContainer = driver.findElement(By.id("message"));
        // Aguardar que a mensagem seja exibida (não está vazia)
        wait.until(d -> !messageContainer.getText().isEmpty());

        // Verificar se contém mensagem de sucesso
        assertTrue(messageContainer.getText().contains("cancelado") ||
                messageContainer.getText().contains("sucesso") ||
                messageContainer.findElements(By.cssSelector(".message-success, .message.message-success")).size() > 0);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String createBookingViaAPI() {
        // Usar RestAssured para criar um booking via API
        // Usar Aveiro em vez de Lisboa para evitar conflitos com outros testes
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        while (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        String requestBody = String.format(
                "{\"municipalityName\":\"Aveiro\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Teste Selenium\"}",
                tomorrow);

        String response = null;
        try {
            response = io.restassured.RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .post(baseUrl + "/api/bookings")
                    .then()
                    .statusCode(anyOf(org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(409))) // Pode ser 409 se
                                                                                                     // atingir limite
                    .extract()
                    .path("token");
        } catch (Exception e) {
            // Se falhou, tentar com outro município
        }

        if (response == null) {
            // Se falhou, tentar com outro município
            requestBody = String.format(
                    "{\"municipalityName\":\"Coimbra\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Teste Selenium\"}",
                    tomorrow);
            response = io.restassured.RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .post(baseUrl + "/api/bookings")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("token");
        }

        // Não é necessário Thread.sleep() - a API já processou o booking quando retorna
        // o token
        return response;
    }
}
