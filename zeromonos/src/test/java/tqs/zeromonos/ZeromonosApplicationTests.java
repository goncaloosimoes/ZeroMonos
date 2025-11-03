package tqs.zeromonos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ZeromonosApplicationTests {

	/**
	 * Teste que verifica se o contexto do Spring Boot carrega corretamente.
	 * O método está vazio porque o próprio ato de carregar o contexto é o teste.
	 * Se houver erros na configuração, dependências ou beans, o Spring falhará
	 * ao iniciar e o teste falhará automaticamente com uma exceção.
	 */
	@Test
	void contextLoads() {
		// Método intencionalmente vazio - o teste passa se o contexto carrega sem erros
	}

}
