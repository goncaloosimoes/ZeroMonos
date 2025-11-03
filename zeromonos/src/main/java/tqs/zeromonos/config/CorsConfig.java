package tqs.zeromonos.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração CORS para a aplicação ZeroMonos.
 * 
 * Esta configuração permite que a API pública seja acessível a partir de
 * origens específicas.
 * Para uma API pública de serviços municipais, é necessário permitir acesso de
 * múltiplas origens,
 * mas de forma controlada através de uma lista de origens permitidas
 * (whitelist).
 * 
 * As origens permitidas podem ser configuradas através da propriedade:
 * - cors.allowed-origins (separadas por vírgula)
 * 
 * Exemplo de configuração em application.properties:
 * cors.allowed-origins=http://localhost:3000,http://localhost:8080,https://zeromonos.municipio.pt
 * 
 * IMPORTANTE: Em produção, DEVE ser restringido a origens específicas
 * conhecidas dos frontends oficiais.
 * Nunca use "*" em produção, pois isso permite acesso de qualquer origem.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Parse allowed origins from configuration (whitelist approach)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));

        // Remove espaços em branco e filtra valores vazios
        String[] originsArray = origins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        registry.addMapping("/api/**")
                .allowedOrigins(originsArray) // Whitelist de origens permitidas (não usar "*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(false); // Set to true only if credentials are needed
    }
}
