package tqs.zeromonos.services;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import org.springframework.stereotype.Service;
import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;

@Service
public class MunicipalityImportService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(MunicipalityImportService.class);

    private final WebClient webClient;
    private final MunicipalityRepository municipalityRepository;

    @Value("${municipalities.api.url}")
    private String apiUrl;

    @Value("${municipalities.request.timeout-ms:20000}")
    private long timeout;

    public MunicipalityImportService(WebClient.Builder wcBuilder, MunicipalityRepository municipalityRepository) {
        this.webClient = wcBuilder.build();
        this.municipalityRepository = municipalityRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            getAndStoreMunicipalities();
        } catch (Exception e) {
            logger.warn("ERRO: Não foi possível importar os municípios ({})", e.toString());
        }
    }

    public void getAndStoreMunicipalities() {
        logger.info("Iniciando importação de municípios a partir de: {}", apiUrl);

        try {
            // Obter lista de municípios (strings simples)
            List<String> names = webClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {
                    })
                    .block(Duration.ofMillis(timeout));

            if (names == null || names.isEmpty()) {
                logger.warn("Nenhum município recebido da API.");
                loadHardCodedMunicipalities();
                return;
            }

            int createdCount = 0;
            int existingCount = 0;

            for (String rawName : names) {
                if (rawName == null || rawName.isBlank())
                    continue;

                String name = rawName.trim();

                if (municipalityRepository.findByName(name).isEmpty()) {
                    municipalityRepository.save(new Municipality(name));
                    createdCount++;
                } else {
                    existingCount++;
                }
            }

            logger.info("Importação concluída. Novos: {}, Existentes: {}, Total recebido: {}",
                    createdCount, existingCount, names.size());

        } catch (WebClientResponseException ex) {
            logger.error("ERRO: Falha HTTP ao obter municípios ({}): {}", ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            loadHardCodedMunicipalities();

        } catch (Exception ex) {
            logger.error("ERRO: Erro inesperado ao importar municípios: {}", ex.getMessage(), ex);
            loadHardCodedMunicipalities();
        }
    }

    private void loadHardCodedMunicipalities() {
        logger.info("Lista de municípios pré-programados a ser enviada para o repositório");

        String[] portugueseMunicipalities = {
                // Principais cidades
                "Lisboa", "Porto", "Braga", "Coimbra", "Faro", "Aveiro",
                "Leiria", "Santarém", "Setúbal", "Viana do Castelo",
                "Vila Real", "Bragança", "Guarda", "Castelo Branco",
                "Portalegre", "Évora", "Beja", "Funchal", "Ponta Delgada",

                // Área Metropolitana de Lisboa
                "Almada", "Amadora", "Cascais", "Loures", "Odivelas",
                "Oeiras", "Seixal", "Sintra", "Barreiro", "Montijo",
                "Moita", "Palmela", "Sesimbra", "Alcochete",

                // Norte
                "Vila Nova de Gaia", "Gondomar", "Matosinhos", "Maia",
                "Trofa", "Santo Tirso", "Valongo", "Vila do Conde",
                "Póvoa de Varzim", "Famalicão", "Guimarães", "Vizela",
                "Felgueiras", "Amarante", "Marco de Canaveses", "Paredes",
                "Penafiel", "Arouca", "Oliveira de Azeméis", "Espinho",
                "Santa Maria da Feira", "Vale de Cambra", "Lousada",
                "Fafe", "Barcelos", "Esposende", "Vila Nova de Cerveira",
                "Monção", "Melgaço", "Arcos de Valdevez", "Ponte de Lima",
                "Ponte da Barca",

                // Centro
                "Tomar", "Abrantes", "Torres Novas", "Ourém",
                "Figueira da Foz", "Mealhada", "Águeda", "Sever do Vouga",
                "Estarreja", "Ílhavo", "Cantanhede", "Oliveira do Bairro",
                "Viseu", "Tondela", "Mangualde", "Nelas", "Carregal do Sal",
                "Fundão", "Covilhã", "Belmonte", "Trancoso", "Pinhel",
                "Sabugal", "Mêda", "Almeida",

                // Alentejo
                "Sines", "Grândola", "Alcácer do Sal", "Serpa",
                "Moura", "Reguengos de Monsaraz", "Montemor-o-Novo",
                "Vendas Novas", "Estremoz", "Elvas", "Campo Maior",

                // Algarve
                "Albufeira", "Lagos", "Loulé", "Portimão", "Tavira",
                "Silves", "Vila Real de Santo António", "Lagoa", "Olhão",

                // Regiões Autónomas
                "Angra do Heroísmo", "Horta", "Ribeira Grande", "Vila Franca do Campo",
                "Machico", "Câmara de Lobos", "Santa Cruz (Madeira)"
        };

        int createdCount = 0;
        for (String municipality : portugueseMunicipalities) {
            if (municipalityRepository.findByName(municipality).isEmpty()) {
                municipalityRepository.save(new Municipality(municipality));
                createdCount++;
            }
        }
        logger.info("{} municípios pré-programados enviados para o repositório", createdCount);
    }
}
