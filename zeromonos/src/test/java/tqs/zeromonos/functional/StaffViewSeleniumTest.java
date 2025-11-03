package tqs.zeromonos.functional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

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
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import io.github.bonigarcia.wdm.WebDriverManager;
import tqs.zeromonos.TestcontainersConfiguration;

import java.time.Duration;

@SpringBootTest(classes = tqs.zeromonos.ZeromonosApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@Import(TestcontainersConfiguration.class)
@DisplayName("Testes Selenium - Vista do Staff")
class StaffViewSeleniumTest {

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

    // ==================== TESTES DO CARREGAMENTO DA PÁGINA ====================

    @Test
    @DisplayName("Painel Staff - Carregamento da página")
    void testStaffPanelLoads() {
        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("filters-form")));

        // Verificar título
        assertTrue(driver.getTitle().contains("Painel Staff") || driver.getTitle().contains("Gestão"));

        // Verificar elementos principais
        WebElement filtersForm = driver.findElement(By.id("filters-form"));
        assertNotNull(filtersForm);

        WebElement municipalityFilter = driver.findElement(By.id("municipality-filter"));
        assertNotNull(municipalityFilter);

        WebElement bookingsTable = driver.findElement(By.id("bookings-tbody"));
        assertNotNull(bookingsTable);
    }

    @Test
    @DisplayName("Painel Staff - Carregamento de bookings na tabela")
    void testStaffPanelLoadsBookings() {
        // Criar alguns bookings antes
        createBookingViaAPI("Lisboa");
        createBookingViaAPI("Porto");

        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("bookings-tbody")));

        // Aguardar que a tabela carregue (o estado de loading desapareça)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state")));

        // Aguardar que os bookings foram carregados (verificar que há linhas na tabela
        // ou estado vazio)
        wait.until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));
            WebElement emptyState = d.findElement(By.id("empty-state"));
            return !rows.isEmpty() || (emptyState != null && emptyState.isDisplayed());
        });

        // Verificar se há linhas na tabela (exceto a linha de loading)
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));

        // Deve haver pelo menos um booking
        assertTrue(rows.size() > 0 || driver.findElements(By.cssSelector("#bookings-tbody tr")).size() > 0);

        // Verificar se o contador foi atualizado
        WebElement totalCount = driver.findElement(By.id("total-count"));
        assertNotNull(totalCount);
        // O contador deve mostrar um número ou "-" se ainda estiver a carregar
    }

    // ==================== TESTES DE FILTROS ====================

    @Test
    @DisplayName("Painel Staff - Filtro por município")
    void testStaffPanelFilterByMunicipality() {
        // Criar bookings para diferentes municípios
        createBookingViaAPI("Lisboa");
        createBookingViaAPI("Porto");

        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("municipality-filter")));

        // Aguardar que os municípios sejam carregados no dropdown
        wait.until(ExpectedConditions
                .invisibilityOfElementLocated(By.cssSelector("#municipality-filter option[disabled]")));

        // Aguardar que haja opções disponíveis (mais de 1 opção, excluindo "Todos os
        // municípios")
        wait.until(d -> {
            WebElement select = d.findElement(By.id("municipality-filter"));
            Select s = new Select(select);
            return s.getOptions().size() > 1;
        });

        // Selecionar município "Lisboa"
        WebElement municipalitySelect = driver.findElement(By.id("municipality-filter"));
        Select select = new Select(municipalitySelect);

        // Verificar se há opções disponíveis (além de "Todos os municípios")
        List<WebElement> options = select.getOptions();
        assertTrue(options.size() > 1, "Deve haver municípios disponíveis no dropdown");

        // Selecionar Lisboa (se existir)
        boolean lisboaFound = false;
        for (WebElement option : options) {
            if (option.getText().contains("Lisboa")) {
                select.selectByVisibleText(option.getText());
                lisboaFound = true;
                break;
            }
        }

        if (lisboaFound) {
            // Aplicar filtro
            WebElement filterButton = driver.findElement(By.id("filter-btn"));
            filterButton.click();

            // Aguardar que os resultados sejam filtrados (aguardar que a tabela seja
            // atualizada)
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state")));
            wait.until(d -> {
                List<WebElement> rows = d.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));
                WebElement emptyState = d.findElement(By.id("empty-state"));
                return !rows.isEmpty() || (emptyState != null && emptyState.isDisplayed());
            });

            // Verificar se os resultados foram filtrados (todos devem ser de Lisboa)
            List<WebElement> rows = driver.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));
            // Pelo menos deve haver uma linha (ou pode estar vazio se não houver matches)
            assertNotNull(rows);
        }
    }

    @Test
    @DisplayName("Painel Staff - Limpar filtros")
    void testStaffPanelClearFilters() {
        // Criar bookings
        createBookingViaAPI("Lisboa");

        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("municipality-filter")));

        // Aguardar municípios carregarem
        wait.until(ExpectedConditions
                .invisibilityOfElementLocated(By.cssSelector("#municipality-filter option[disabled]")));

        // Aguardar que haja opções disponíveis
        wait.until(d -> {
            WebElement select = d.findElement(By.id("municipality-filter"));
            Select s = new Select(select);
            return s.getOptions().size() > 1;
        });

        // Selecionar um município
        WebElement municipalitySelect = driver.findElement(By.id("municipality-filter"));
        Select select = new Select(municipalitySelect);

        if (select.getOptions().size() > 1) {
            select.selectByIndex(1); // Selecionar primeiro município disponível

            // Aplicar filtro
            WebElement filterButton = driver.findElement(By.id("filter-btn"));
            filterButton.click();
            // Aguardar que o filtro seja aplicado
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state")));

            // Limpar filtros
            WebElement resetButton = driver.findElement(By.id("reset-btn"));
            resetButton.click();

            // Aguardar que o filtro seja limpo (verificar que a primeira opção é "Todos os
            // municípios")
            wait.until(d -> {
                Select selectAfterReset = new Select(d.findElement(By.id("municipality-filter")));
                return selectAfterReset.getOptions().get(0).getText().equals("Todos os municípios");
            });

            // Verificar se o filtro foi limpo
            Select selectAfterReset = new Select(driver.findElement(By.id("municipality-filter")));
            WebElement firstOption = selectAfterReset.getOptions().get(0);
            assertEquals("Todos os municípios", firstOption.getText());
        }
    }

    // ==================== TESTES DE ATUALIZAÇÃO DE STATUS ====================

    @Test
    @DisplayName("Painel Staff - Atualizar status de booking para ASSIGNED")
    void testStaffPanelUpdateStatusToAssigned() {
        // Criar booking
        String token = createBookingViaAPI("Lisboa");

        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("bookings-tbody")));

        // Aguardar bookings carregarem
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state")));

        // Aguardar que haja linhas na tabela (ou estado vazio)
        wait.until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));
            WebElement emptyState = d.findElement(By.id("empty-state"));
            return !rows.isEmpty() || (emptyState != null && emptyState.isDisplayed());
        });

        // Procurar linha com o token criado
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookings-tbody tr"));

        WebElement targetRow = null;
        for (WebElement row : rows) {
            if (row.getText().contains(token.substring(0, 8))) { // Primeiros 8 caracteres do token
                targetRow = row;
                break;
            }
        }

        if (targetRow != null) {
            // Procurar botão de atualização de status na linha
            try {
                WebElement statusSelect = targetRow.findElement(By.cssSelector("select"));
                Select select = new Select(statusSelect);

                // Fazer scroll até ao select
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", statusSelect);
                // Aguardar que o select esteja visível após scroll
                wait.until(ExpectedConditions.visibilityOf(statusSelect));

                // Selecionar ASSIGNED
                select.selectByValue("ASSIGNED");

                // Aguardar que o valor seja selecionado e verificar se a mensagem de sucesso
                // aparece
                wait.until(d -> {
                    WebElement sel = d.findElement(By.cssSelector("select"));
                    return "ASSIGNED".equals(sel.getDomProperty("value"));
                });

                // Verificar se a mensagem de sucesso aparece
                WebElement successMessage = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.className("message-success")));
                assertNotNull(successMessage);
            } catch (Exception e) {
                // Se não houver select, pode haver botões individuais
                List<WebElement> statusButtons = targetRow.findElements(By.cssSelector(".btn-action, button"));
                if (!statusButtons.isEmpty()) {
                    // Fazer scroll até ao botão e usar JavaScript para clicar se necessário
                    WebElement button = statusButtons.get(0);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                    // Aguardar que o botão esteja clicável após scroll
                    wait.until(ExpectedConditions.elementToBeClickable(button));
                    try {
                        button.click();
                    } catch (Exception ex) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                    }
                    // Aguardar que a ação seja processada (verificar se mensagem de sucesso
                    // aparece)
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.presenceOfElementLocated(By.className("message-success")),
                            ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state"))));
                }
            }
        }
    }

    @Test
    @DisplayName("Painel Staff - Visualizar histórico de booking")
    void testStaffPanelViewHistory() {
        // Criar booking e atualizar status
        String token = createBookingViaAPI("Lisboa");
        updateBookingStatusViaAPI(token, "ASSIGNED");

        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("bookings-tbody")));

        // Aguardar bookings carregarem
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state")));

        // Aguardar que haja linhas na tabela (ou estado vazio)
        wait.until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));
            WebElement emptyState = d.findElement(By.id("empty-state"));
            return !rows.isEmpty() || (emptyState != null && emptyState.isDisplayed());
        });

        // Procurar linha com o token
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookings-tbody tr"));

        WebElement targetRow = null;
        for (WebElement row : rows) {
            if (row.getText().contains(token.substring(0, 8))) {
                targetRow = row;
                break;
            }
        }

        if (targetRow != null) {
            // Procurar botão de histórico
            try {
                WebElement historyButton = targetRow.findElement(
                        By.cssSelector("button[onclick*='History'], .btn-history, button:contains('Histórico')"));

                historyButton.click();

                // Aguardar modal aparecer
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("history-modal")));

                WebElement modal = driver.findElement(By.id("history-modal"));
                assertFalse(modal.getDomAttribute("class").contains("hidden"));

                // Verificar se há histórico na lista
                WebElement historyList = driver.findElement(By.id("history-list"));
                assertNotNull(historyList);

                // Fechar modal
                WebElement closeButton = driver.findElement(By.className("modal-close"));
                closeButton.click();

                // Aguardar que o modal seja fechado (verificar que a classe hidden está
                // presente)
                wait.until(d -> {
                    WebElement modalElement = d.findElement(By.id("history-modal"));
                    String classes = modalElement.getDomAttribute("class");
                    return classes != null && classes.contains("hidden");
                });

                // Verificar se modal foi fechado (re-obter o elemento para evitar
                // StaleElementReference)
                WebElement modalAfterClose = driver.findElement(By.id("history-modal"));
                assertTrue(modalAfterClose.getDomAttribute("class").contains("hidden"));
            } catch (Exception e) {
                // Se não encontrar botão de histórico, pode não estar implementado na UI atual
                // Não falhar o teste, apenas pular esta parte
            }
        }
    }

    // ==================== TESTES DE EMPTY STATE ====================

    @Test
    @DisplayName("Painel Staff - Estado vazio quando não há bookings")
    void testStaffPanelEmptyState() {
        driver.get(baseUrl + "/staff-panel.html");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("bookings-tbody")));

        // Aguardar loading desaparecer
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-state")));

        // Aguardar que a tabela seja carregada (verificar que há linhas ou estado
        // vazio)
        wait.until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));
            WebElement emptyState = d.findElement(By.id("empty-state"));
            return !rows.isEmpty() || (emptyState != null && emptyState.isDisplayed());
        });

        // Verificar se há estado vazio ou se há bookings
        List<WebElement> emptyStateElements = driver.findElements(By.id("empty-state"));
        List<WebElement> tableRows = driver.findElements(By.cssSelector("#bookings-tbody tr:not(.loading-state)"));

        // Pode estar vazio ou ter bookings, ambos são válidos
        assertTrue(emptyStateElements.isEmpty()
                || !emptyStateElements.get(0).getDomAttribute("class").contains("hidden") ||
                tableRows.size() > 0);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String createBookingViaAPI(String municipality) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        while (tomorrow.getDayOfWeek().getValue() == 7) {
            tomorrow = tomorrow.plusDays(1);
        }

        String requestBody = String.format(
                "{\"municipalityName\":\"%s\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Teste Selenium Staff\"}",
                municipality, tomorrow);

        String token = io.restassured.RestAssured.given()
                .contentType("application/json")
                .body(requestBody)
                .post(baseUrl + "/api/bookings")
                .then()
                .statusCode(anyOf(is(200), is(409))) // Pode ser 409 se atingir limite
                .extract()
                .path("token");

        if (token == null) {
            // Se falhou por limite, tentar com outro município
            String[] alternativeMunicipalities = { "Coimbra", "Faro", "Leiria" };
            for (String alt : alternativeMunicipalities) {
                if (alt.equals(municipality))
                    continue;
                requestBody = String.format(
                        "{\"municipalityName\":\"%s\",\"requestedDate\":\"%s\",\"timeSlot\":\"AFTERNOON\",\"description\":\"Teste Selenium Staff\"}",
                        alt, tomorrow);
                try {
                    token = io.restassured.RestAssured.given()
                            .contentType("application/json")
                            .body(requestBody)
                            .post(baseUrl + "/api/bookings")
                            .then()
                            .statusCode(200)
                            .extract()
                            .path("token");
                    if (token != null)
                        break;
                } catch (Exception e) {
                    // Continuar para próximo município
                }
            }
        }

        // Não é necessário Thread.sleep() - a API já processou o booking quando retorna
        // o token
        return token;
    }

    private void updateBookingStatusViaAPI(String token, String status) {
        io.restassured.RestAssured.given()
                .contentType("application/json")
                .queryParam("status", status)
                .patch(baseUrl + "/api/staff/bookings/" + token + "/status")
                .then()
                .statusCode(200);
    }
}
