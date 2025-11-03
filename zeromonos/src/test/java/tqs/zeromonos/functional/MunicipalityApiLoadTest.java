package tqs.zeromonos.functional;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import tqs.zeromonos.TestcontainersConfiguration;
import tqs.zeromonos.data.MunicipalityRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@Import(TestcontainersConfiguration.class)
@DisplayName("Testes de Carga de Municípios da API Externa")
class MunicipalityApiLoadTest {

        @LocalServerPort
        private int port;

        @Autowired
        private MunicipalityRepository municipalityRepository;

        private String baseUrl;

        private static final int EXPECTED_MUNICIPALITIES_COUNT = 308;

        @BeforeEach
        void setUp() {
                baseUrl = "http://localhost:" + port;
                RestAssured.baseURI = baseUrl;
                RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        }

        @Test
        @DisplayName("Verificar que a API externa carregou exatamente 308 municípios no repositório")
        void testExternalApiLoaded308Municipalities() {
                // Aguardar que o ApplicationRunner execute e carregue os municípios
                // A espera condicional garante que a importação terminou sem usar
                // Thread.sleep()
                Awaitility.await()
                                .atMost(Duration.ofSeconds(30))
                                .pollInterval(Duration.ofMillis(100))
                                .until(() -> municipalityRepository.count() >= EXPECTED_MUNICIPALITIES_COUNT);

                // Verificar através do repositório diretamente
                long count = municipalityRepository.count();

                assertEquals(EXPECTED_MUNICIPALITIES_COUNT, count,
                                String.format("Esperado %d municípios, mas foram encontrados %d no repositório",
                                                EXPECTED_MUNICIPALITIES_COUNT, count));

                // Verificar que não está vazio
                assertTrue(count > 0, "O repositório não deve estar vazio após a importação");

                // Verificar que tem pelo menos alguns municípios conhecidos
                assertTrue(municipalityRepository.findByName("Lisboa").isPresent(),
                                "Lisboa deve estar presente na lista de municípios");
                assertTrue(municipalityRepository.findByName("Porto").isPresent(),
                                "Porto deve estar presente na lista de municípios");
                assertTrue(municipalityRepository.findByName("Aveiro").isPresent(),
                                "Aveiro deve estar presente na lista de municípios");
        }

        @Test
        @DisplayName("Verificar que a API REST retorna 308 municípios")
        void testApiReturns308Municipalities() {
                // Aguardar que o ApplicationRunner execute e carregue os municípios
                Awaitility.await()
                                .atMost(Duration.ofSeconds(30))
                                .pollInterval(Duration.ofMillis(100))
                                .until(() -> municipalityRepository.count() >= EXPECTED_MUNICIPALITIES_COUNT);

                // Verificar através da API REST
                List<String> municipalities = given()
                                .contentType(ContentType.JSON)
                                .when()
                                .get("/api/bookings/municipalities")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .body("size()", equalTo(EXPECTED_MUNICIPALITIES_COUNT))
                                .extract()
                                .jsonPath()
                                .getList(".", String.class);

                assertNotNull(municipalities, "A lista de municípios não deve ser null");
                assertEquals(EXPECTED_MUNICIPALITIES_COUNT, municipalities.size(),
                                String.format("Esperado %d municípios na resposta da API, mas foram retornados %d",
                                                EXPECTED_MUNICIPALITIES_COUNT, municipalities.size()));

                // Verificar que contém municípios conhecidos
                assertTrue(municipalities.contains("Lisboa"),
                                "A lista deve conter Lisboa");
                assertTrue(municipalities.contains("Porto"),
                                "A lista deve conter Porto");
                assertTrue(municipalities.contains("Aveiro"),
                                "A lista deve conter Aveiro");

                // Verificar que não contém valores inválidos
                assertTrue(municipalities.stream().noneMatch(s -> s == null || s.isBlank()),
                                "A lista não deve conter valores null ou vazios");
        }

        @Test
        @DisplayName("Verificar que o repositório e a API REST têm a mesma contagem de municípios")
        void testRepositoryAndApiHaveSameCount() {
                // Aguardar que o ApplicationRunner execute e carregue os municípios
                Awaitility.await()
                                .atMost(Duration.ofSeconds(30))
                                .pollInterval(Duration.ofMillis(100))
                                .until(() -> municipalityRepository.count() >= EXPECTED_MUNICIPALITIES_COUNT);

                // Contar no repositório
                long repositoryCount = municipalityRepository.count();

                // Contar através da API
                List<String> municipalities = given()
                                .contentType(ContentType.JSON)
                                .when()
                                .get("/api/bookings/municipalities")
                                .then()
                                .statusCode(HttpStatus.OK.value())
                                .extract()
                                .jsonPath()
                                .getList(".", String.class);

                int apiCount = municipalities.size();

                assertEquals(repositoryCount, apiCount,
                                String.format("O repositório tem %d municípios mas a API retorna %d. Devem ser iguais.",
                                                repositoryCount, apiCount));

                // Verificar que ambos têm o valor esperado
                assertEquals(EXPECTED_MUNICIPALITIES_COUNT, repositoryCount,
                                "O repositório deve ter " + EXPECTED_MUNICIPALITIES_COUNT + " municípios");
                assertEquals(EXPECTED_MUNICIPALITIES_COUNT, apiCount,
                                "A API deve retornar " + EXPECTED_MUNICIPALITIES_COUNT + " municípios");
        }
}
