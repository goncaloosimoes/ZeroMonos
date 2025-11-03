package tqs.zeromonos.isolation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;

@DataJpaTest
@DisplayName("Testes Unitários de MunicipalityRepository com JPA")
class MunicipalityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    private Municipality lisboa;
    private Municipality porto;
    private Municipality coimbra;

    @BeforeEach
    void setUp() {
        // Limpar banco de dados
        municipalityRepository.deleteAll();

        // Criar municípios
        lisboa = new Municipality("Lisboa");
        porto = new Municipality("Porto");
        coimbra = new Municipality("Coimbra");

        entityManager.persistAndFlush(lisboa);
        entityManager.persistAndFlush(porto);
        entityManager.persistAndFlush(coimbra);
        entityManager.clear();
    }

    // ==================== TESTES DE MÉTODOS HERDADOS ====================

    @Test
    @DisplayName("save - Deve salvar um novo município")
    void testSave_NewMunicipality() {
        // Arrange
        Municipality aveiro = new Municipality("Aveiro");

        // Act
        Municipality saved = municipalityRepository.save(aveiro);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("Aveiro", saved.getName());
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("save - Deve atualizar município existente")
    void testSave_UpdateMunicipality() {
        // Arrange
        String newName = "Lisboa Metropolitana";
        lisboa.setName(newName);

        // Act
        Municipality updated = municipalityRepository.save(lisboa);

        // Assert
        assertEquals(lisboa.getId(), updated.getId());
        assertEquals(newName, updated.getName());
    }

    @Test
    @DisplayName("findAll - Deve retornar todos os municípios")
    void testFindAll() {
        // Act
        List<Municipality> municipalities = municipalityRepository.findAll();

        // Assert
        assertEquals(3, municipalities.size());
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Lisboa")));
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Porto")));
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Coimbra")));
    }

    @Test
    @DisplayName("findById - Deve encontrar município por ID")
    void testFindById() {
        // Arrange
        Long lisboaId = lisboa.getId();

        // Act
        Optional<Municipality> found = municipalityRepository.findById(lisboaId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Lisboa", found.get().getName());
        assertEquals(lisboaId, found.get().getId());
    }

    @Test
    @DisplayName("findById - Deve retornar empty quando ID não existe")
    void testFindById_NotFound() {
        // Arrange
        Long nonExistentId = 99999L;

        // Act
        Optional<Municipality> found = municipalityRepository.findById(nonExistentId);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("delete - Deve deletar município")
    void testDelete() {
        // Arrange
        Long coimbraId = coimbra.getId();

        // Act
        municipalityRepository.deleteById(coimbraId);

        // Assert
        Optional<Municipality> deleted = municipalityRepository.findById(coimbraId);
        assertFalse(deleted.isPresent());
        assertEquals(2, municipalityRepository.count());
    }

    @Test
    @DisplayName("delete - Deve deletar todos os municípios")
    void testDeleteAll() {
        // Act
        municipalityRepository.deleteAll();

        // Assert
        assertEquals(0, municipalityRepository.count());
        assertTrue(municipalityRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("count - Deve contar todos os municípios")
    void testCount() {
        // Act
        long count = municipalityRepository.count();

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("existsById - Deve retornar true quando município existe")
    void testExistsById() {
        // Arrange
        Long lisboaId = lisboa.getId();

        // Act
        boolean exists = municipalityRepository.existsById(lisboaId);

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsById - Deve retornar false quando município não existe")
    void testExistsById_NotFound() {
        // Arrange
        Long nonExistentId = 99999L;

        // Act
        boolean exists = municipalityRepository.existsById(nonExistentId);

        // Assert
        assertFalse(exists);
    }

    // ==================== TESTES DE MÉTODOS CUSTOMIZADOS ====================

    @Test
    @DisplayName("findByName - Deve encontrar município por nome")
    void testFindByName() {
        // Act
        Optional<Municipality> found = municipalityRepository.findByName("Lisboa");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Lisboa", found.get().getName());
        assertEquals(lisboa.getId(), found.get().getId());
    }

    @Test
    @DisplayName("findByName - Deve encontrar município por nome (case-sensitive)")
    void testFindByName_CaseSensitive() {
        // Act
        Optional<Municipality> foundExact = municipalityRepository.findByName("Lisboa");
        Optional<Municipality> foundLowercase = municipalityRepository.findByName("lisboa");
        Optional<Municipality> foundUppercase = municipalityRepository.findByName("LISBOA");

        // Assert
        assertTrue(foundExact.isPresent());
        // A busca é case-sensitive por padrão em JPA
        assertFalse(foundLowercase.isPresent(), "Busca deve ser case-sensitive");
        assertFalse(foundUppercase.isPresent(), "Busca deve ser case-sensitive");
    }

    @Test
    @DisplayName("findByName - Deve retornar empty quando nome não existe")
    void testFindByName_NotFound() {
        // Act
        Optional<Municipality> found = municipalityRepository.findByName("MunicipioInexistente");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("findByName - Deve encontrar diferentes municípios")
    void testFindByName_DifferentMunicipalities() {
        // Act
        Optional<Municipality> foundLisboa = municipalityRepository.findByName("Lisboa");
        Optional<Municipality> foundPorto = municipalityRepository.findByName("Porto");
        Optional<Municipality> foundCoimbra = municipalityRepository.findByName("Coimbra");

        // Assert
        assertTrue(foundLisboa.isPresent());
        assertEquals("Lisboa", foundLisboa.get().getName());

        assertTrue(foundPorto.isPresent());
        assertEquals("Porto", foundPorto.get().getName());

        assertTrue(foundCoimbra.isPresent());
        assertEquals("Coimbra", foundCoimbra.get().getName());
    }

    // ==================== TESTES DE UNICIDADE ====================

    @Test
    @DisplayName("save - Deve rejeitar município com nome duplicado")
    void testSave_DuplicateName() {
        // Arrange
        Municipality duplicateLisboa = new Municipality("Lisboa");

        // Act & Assert
        // Tentar salvar um município com nome duplicado deve lançar exceção
        assertThrows(DataIntegrityViolationException.class, () -> {
            municipalityRepository.save(duplicateLisboa);
            entityManager.flush();
        }, "Deve lançar exceção ao tentar salvar nome duplicado");
    }

    @Test
    @DisplayName("save - Deve aceitar municípios com nomes diferentes")
    void testSave_DifferentNames() {
        // Arrange
        Municipality aveiro = new Municipality("Aveiro");
        Municipality braga = new Municipality("Braga");

        // Act
        Municipality savedAveiro = municipalityRepository.save(aveiro);
        Municipality savedBraga = municipalityRepository.save(braga);

        // Assert
        assertNotNull(savedAveiro.getId());
        assertNotNull(savedBraga.getId());
        assertEquals("Aveiro", savedAveiro.getName());
        assertEquals("Braga", savedBraga.getName());
        assertEquals(5, municipalityRepository.count());
    }

    // ==================== TESTES DE VALIDAÇÃO ====================

    @Test
    @DisplayName("save - Deve rejeitar município com nome null")
    void testSave_NullName() {
        // Arrange
        Municipality municipalityWithNullName = new Municipality();
        municipalityWithNullName.setName(null);

        // Act & Assert
        // JPA deve validar @Column(nullable = false)
        assertThrows(Exception.class, () -> {
            municipalityRepository.save(municipalityWithNullName);
            entityManager.flush();
        }, "Deve rejeitar município com nome null");
    }

    @Test
    @DisplayName("save - Deve aceitar município com nome vazio")
    void testSave_EmptyName() {
        // Arrange
        Municipality municipalityWithEmptyName = new Municipality("");

        // Act
        Municipality saved = municipalityRepository.save(municipalityWithEmptyName);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("", saved.getName());
    }

    @Test
    @DisplayName("save - Deve aceitar município com nome longo")
    void testSave_LongName() {
        // Arrange
        String longName = "Município com nome muito longo para testar limites de campo";
        Municipality municipalityWithLongName = new Municipality(longName);

        // Act
        Municipality saved = municipalityRepository.save(municipalityWithLongName);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(longName, saved.getName());
    }

    // ==================== TESTES DE BUSCA E FILTROS ====================

    @Test
    @DisplayName("findAll - Deve retornar municípios ordenados")
    void testFindAll_Order() {
        // Arrange
        Municipality aveiro = new Municipality("Aveiro");
        Municipality braga = new Municipality("Braga");
        municipalityRepository.save(aveiro);
        municipalityRepository.save(braga);

        // Act
        List<Municipality> municipalities = municipalityRepository.findAll();

        // Assert
        assertTrue(municipalities.size() >= 5);
        // A ordem pode variar, mas todos devem estar presentes
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Lisboa")));
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Porto")));
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Coimbra")));
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Aveiro")));
        assertTrue(municipalities.stream().anyMatch(m -> m.getName().equals("Braga")));
    }

    @Test
    @DisplayName("findByName - Deve retornar mesmo município após clear e flush")
    void testFindByName_AfterClear() {
        // Arrange
        entityManager.clear();

        // Act
        Optional<Municipality> found = municipalityRepository.findByName("Lisboa");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Lisboa", found.get().getName());
        assertNotNull(found.get().getId());
    }

    // ==================== TESTES DE PERSISTÊNCIA ====================

    @Test
    @DisplayName("save - Deve persistir município corretamente")
    void testSave_Persistence() {
        // Arrange
        Municipality newMunicipality = new Municipality("Faro");
        Municipality saved = municipalityRepository.save(newMunicipality);
        Long savedId = saved.getId();
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<Municipality> retrieved = municipalityRepository.findById(savedId);

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("Faro", retrieved.get().getName());
        assertEquals(savedId, retrieved.get().getId());
    }

    @Test
    @DisplayName("delete - Deve deletar e verificar com findByName")
    void testDelete_VerifyWithFindByName() {
        // Arrange
        Long coimbraId = coimbra.getId();

        // Act
        municipalityRepository.deleteById(coimbraId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Municipality> found = municipalityRepository.findByName("Coimbra");
        assertFalse(found.isPresent());

        Optional<Municipality> foundById = municipalityRepository.findById(coimbraId);
        assertFalse(foundById.isPresent());
    }
}
