package tqs.zeromonos.cucumber;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import io.cucumber.spring.CucumberContextConfiguration;
import tqs.zeromonos.TestcontainersConfiguration;
import tqs.zeromonos.ZeromonosApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = ZeromonosApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties")
@Import(TestcontainersConfiguration.class)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        // Port é injetado pelo Spring
    }
}
