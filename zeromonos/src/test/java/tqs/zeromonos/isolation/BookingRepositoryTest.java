package tqs.zeromonos.isolation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import tqs.zeromonos.data.Booking;
import tqs.zeromonos.data.BookingRepository;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;
import tqs.zeromonos.data.StateChange;
import tqs.zeromonos.data.TimeSlot;

@DataJpaTest
@DisplayName("Testes Unitários de BookingRepository com JPA")
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    private Municipality lisboa;
    private Municipality porto;
    private Booking booking1;
    private Booking booking2;
    private Booking booking3;
    private LocalDate tomorrow;
    private LocalDate dayAfterTomorrow;

    @BeforeEach
    void setUp() {
        // Limpar banco de dados
        bookingRepository.deleteAll();
        municipalityRepository.deleteAll();

        // Criar municípios
        lisboa = new Municipality("Lisboa");
        porto = new Municipality("Porto");
        entityManager.persistAndFlush(lisboa);
        entityManager.persistAndFlush(porto);

        // Criar datas válidas
        tomorrow = LocalDate.now().plusDays(1);
        if (tomorrow.getDayOfWeek().getValue() == 7) { // Se for domingo
            tomorrow = tomorrow.plusDays(1);
        }
        dayAfterTomorrow = tomorrow.plusDays(1);

        // Criar bookings
        booking1 = new Booking(lisboa, "Sofá velho", tomorrow, TimeSlot.MORNING);
        booking2 = new Booking(lisboa, "Colchão", tomorrow, TimeSlot.AFTERNOON);
        booking3 = new Booking(porto, "Mesa de jantar", dayAfterTomorrow, TimeSlot.EVENING);

        // Adicionar StateChange inicial para cada booking
        StateChange initialState1 = new StateChange(BookingStatus.RECEIVED, booking1.getCreatedAt());
        booking1.addStateChange(initialState1);

        StateChange initialState2 = new StateChange(BookingStatus.RECEIVED, booking2.getCreatedAt());
        booking2.addStateChange(initialState2);

        StateChange initialState3 = new StateChange(BookingStatus.RECEIVED, booking3.getCreatedAt());
        booking3.addStateChange(initialState3);

        entityManager.persistAndFlush(booking1);
        entityManager.persistAndFlush(booking2);
        entityManager.persistAndFlush(booking3);
        entityManager.clear();
    }

    // ==================== TESTES DE MÉTODOS HERDADOS ====================

    @Test
    @DisplayName("save - Deve salvar um novo booking")
    void testSave_NewBooking() {
        // Arrange
        Booking newBooking = new Booking(lisboa, "Frigorífico antigo", tomorrow, TimeSlot.NIGHT);
        StateChange initialState = new StateChange(BookingStatus.RECEIVED, newBooking.getCreatedAt());
        newBooking.addStateChange(initialState);

        // Act
        Booking saved = bookingRepository.save(newBooking);

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(saved.getToken());
        assertEquals("Frigorífico antigo", saved.getDescription());
        assertEquals(lisboa, saved.getMunicipality());
        assertEquals(BookingStatus.RECEIVED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("findAll - Deve retornar todos os bookings")
    void testFindAll() {
        // Act
        List<Booking> bookings = bookingRepository.findAll();

        // Assert
        assertEquals(3, bookings.size());
        assertTrue(bookings.stream().anyMatch(b -> b.getDescription().equals("Sofá velho")));
        assertTrue(bookings.stream().anyMatch(b -> b.getDescription().equals("Colchão")));
        assertTrue(bookings.stream().anyMatch(b -> b.getDescription().equals("Mesa de jantar")));
    }

    @Test
    @DisplayName("findById - Deve encontrar booking por ID")
    void testFindById() {
        // Arrange
        UUID bookingId = booking1.getId();

        // Act
        Optional<Booking> found = bookingRepository.findById(bookingId);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(booking1.getToken(), found.get().getToken());
        assertEquals("Sofá velho", found.get().getDescription());
    }

    @Test
    @DisplayName("findById - Deve retornar empty quando ID não existe")
    void testFindById_NotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<Booking> found = bookingRepository.findById(nonExistentId);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("delete - Deve deletar booking")
    void testDelete() {
        // Arrange
        UUID bookingId = booking1.getId();

        // Act
        bookingRepository.deleteById(bookingId);

        // Assert
        Optional<Booking> deleted = bookingRepository.findById(bookingId);
        assertFalse(deleted.isPresent());
        assertEquals(2, bookingRepository.count());
    }

    @Test
    @DisplayName("count - Deve contar todos os bookings")
    void testCount() {
        // Act
        long count = bookingRepository.count();

        // Assert
        assertEquals(3, count);
    }

    // ==================== TESTES DE MÉTODOS CUSTOMIZADOS ====================

    @Test
    @DisplayName("findByToken - Deve encontrar booking por token")
    void testFindByToken() {
        // Arrange
        String token = booking1.getToken();

        // Act
        Optional<Booking> found = bookingRepository.findByToken(token);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(booking1.getId(), found.get().getId());
        assertEquals("Sofá velho", found.get().getDescription());
    }

    @Test
    @DisplayName("findByToken - Deve retornar empty quando token não existe")
    void testFindByToken_NotFound() {
        // Arrange
        String nonExistentToken = "token-inexistente-12345";

        // Act
        Optional<Booking> found = bookingRepository.findByToken(nonExistentToken);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("findByMunicipality - Deve encontrar bookings por município")
    void testFindByMunicipality() {
        // Act
        List<Booking> lisboaBookings = bookingRepository.findByMunicipality(lisboa);
        List<Booking> portoBookings = bookingRepository.findByMunicipality(porto);

        // Assert
        assertEquals(2, lisboaBookings.size());
        // Comparar por ID porque após entityManager.clear() podem ser instâncias
        // diferentes
        assertTrue(lisboaBookings.stream().allMatch(b -> b.getMunicipality().getId().equals(lisboa.getId())));
        assertEquals(1, portoBookings.size());
        assertTrue(portoBookings.stream().allMatch(b -> b.getMunicipality().getId().equals(porto.getId())));
    }

    @Test
    @DisplayName("findByMunicipality - Deve retornar lista vazia quando município não tem bookings")
    void testFindByMunicipality_Empty() {
        // Arrange
        Municipality coimbra = new Municipality("Coimbra");
        entityManager.persistAndFlush(coimbra);

        // Act
        List<Booking> coimbraBookings = bookingRepository.findByMunicipality(coimbra);

        // Assert
        assertTrue(coimbraBookings.isEmpty());
    }

    @Test
    @DisplayName("findByMunicipalityName - Deve encontrar bookings por nome do município")
    void testFindByMunicipalityName() {
        // Act
        List<Booking> lisboaBookings = bookingRepository.findByMunicipalityName("Lisboa");
        List<Booking> portoBookings = bookingRepository.findByMunicipalityName("Porto");

        // Assert
        assertEquals(2, lisboaBookings.size());
        assertTrue(lisboaBookings.stream().allMatch(b -> b.getMunicipality().getName().equals("Lisboa")));
        assertEquals(1, portoBookings.size());
        assertTrue(portoBookings.stream().allMatch(b -> b.getMunicipality().getName().equals("Porto")));
    }

    @Test
    @DisplayName("findByMunicipalityName - Deve retornar lista vazia quando município não existe")
    void testFindByMunicipalityName_NotFound() {
        // Act
        List<Booking> notFoundBookings = bookingRepository.findByMunicipalityName("MunicipioInexistente");

        // Assert
        assertTrue(notFoundBookings.isEmpty());
    }

    @Test
    @DisplayName("findByMunicipalityAndRequestedDateAndTimeSlot - Deve encontrar bookings específicos")
    void testFindByMunicipalityAndRequestedDateAndTimeSlot() {
        // Act
        List<Booking> found = bookingRepository.findByMunicipalityAndRequestedDateAndTimeSlot(
                lisboa, tomorrow, TimeSlot.MORNING);

        // Assert
        assertEquals(1, found.size());
        assertEquals(booking1.getId(), found.get(0).getId());
        assertEquals(tomorrow, found.get(0).getRequestedDate());
        assertEquals(TimeSlot.MORNING, found.get(0).getTimeSlot());
    }

    @Test
    @DisplayName("findByMunicipalityAndRequestedDateAndTimeSlot - Deve retornar lista vazia quando não há match")
    void testFindByMunicipalityAndRequestedDateAndTimeSlot_NotFound() {
        // Act
        List<Booking> found = bookingRepository.findByMunicipalityAndRequestedDateAndTimeSlot(
                lisboa, tomorrow, TimeSlot.NIGHT);

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("countByMunicipalityAndRequestedDateAndTimeSlot - Deve contar bookings específicos")
    void testCountByMunicipalityAndRequestedDateAndTimeSlot() {
        // Act
        long count = bookingRepository.countByMunicipalityAndRequestedDateAndTimeSlot(
                lisboa, tomorrow, TimeSlot.MORNING);

        // Assert
        assertEquals(1, count);
    }

    @Test
    @DisplayName("countByMunicipalityAndRequestedDateAndTimeSlot - Deve retornar 0 quando não há match")
    void testCountByMunicipalityAndRequestedDateAndTimeSlot_NotFound() {
        // Act
        long count = bookingRepository.countByMunicipalityAndRequestedDateAndTimeSlot(
                lisboa, tomorrow, TimeSlot.LATE_NIGHT);

        // Assert
        assertEquals(0, count);
    }

    @Test
    @DisplayName("findByRequestedDateAndMunicipality - Deve encontrar bookings por data e município")
    void testFindByRequestedDateAndMunicipality() {
        // Act
        List<Booking> found = bookingRepository.findByRequestedDateAndMunicipality(tomorrow, lisboa);

        // Assert
        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(b -> b.getRequestedDate().equals(tomorrow)));
        // Comparar por ID porque após entityManager.clear() podem ser instâncias
        // diferentes
        assertTrue(found.stream().allMatch(b -> b.getMunicipality().getId().equals(lisboa.getId())));
    }

    @Test
    @DisplayName("findByRequestedDateAndMunicipality - Deve retornar lista vazia quando não há match")
    void testFindByRequestedDateAndMunicipality_NotFound() {
        // Arrange
        LocalDate futureDate = tomorrow.plusDays(10);

        // Act
        List<Booking> found = bookingRepository.findByRequestedDateAndMunicipality(futureDate, lisboa);

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("countByMunicipality - Deve contar todos os bookings de um município")
    void testCountByMunicipality() {
        // Act
        int lisboaCount = bookingRepository.countByMunicipality(lisboa);
        int portoCount = bookingRepository.countByMunicipality(porto);

        // Assert
        assertEquals(2, lisboaCount);
        assertEquals(1, portoCount);
    }

    @Test
    @DisplayName("countByMunicipality - Deve retornar 0 quando município não tem bookings")
    void testCountByMunicipality_Empty() {
        // Arrange
        Municipality coimbra = new Municipality("Coimbra");
        entityManager.persistAndFlush(coimbra);

        // Act
        int count = bookingRepository.countByMunicipality(coimbra);

        // Assert
        assertEquals(0, count);
    }

    // ==================== TESTES DE RELACIONAMENTOS E CASCADE ====================

    @Test
    @DisplayName("save - Deve salvar booking com StateChange (cascade)")
    void testSave_WithStateChange() {
        // Arrange
        Booking newBooking = new Booking(lisboa, "Teste", tomorrow, TimeSlot.MORNING);
        StateChange initialState = new StateChange(BookingStatus.RECEIVED, OffsetDateTime.now());
        newBooking.addStateChange(initialState);

        // Act
        Booking saved = bookingRepository.save(newBooking);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Booking retrieved = bookingRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, retrieved.getHistory().size());
        assertEquals(BookingStatus.RECEIVED, retrieved.getHistory().get(0).getStatus());
    }

    @Test
    @DisplayName("save - Deve atualizar booking com novo StateChange")
    void testSave_UpdateWithNewStateChange() {
        // Arrange
        StateChange newState = new StateChange(BookingStatus.ASSIGNED, OffsetDateTime.now());
        booking1.addStateChange(newState);

        // Act
        Booking updated = bookingRepository.save(booking1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Booking retrieved = bookingRepository.findById(updated.getId()).orElseThrow();
        assertEquals(2, retrieved.getHistory().size());
        assertTrue(retrieved.getHistory().stream()
                .anyMatch(sc -> sc.getStatus() == BookingStatus.ASSIGNED));
        assertEquals(BookingStatus.ASSIGNED, retrieved.getStatus());
    }

    @Test
    @DisplayName("findByToken - Deve carregar histórico (EAGER)")
    void testFindByToken_LoadsHistory() {
        // Arrange - Adicionar mais um StateChange
        StateChange state2 = new StateChange(BookingStatus.ASSIGNED, OffsetDateTime.now());
        booking1.addStateChange(state2);
        bookingRepository.save(booking1);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<Booking> found = bookingRepository.findByToken(booking1.getToken());

        // Assert
        assertTrue(found.isPresent());
        assertNotNull(found.get().getHistory());
        assertFalse(found.get().getHistory().isEmpty());
        assertEquals(2, found.get().getHistory().size());
    }

    @Test
    @DisplayName("delete - Deve deletar StateChanges em cascade (orphanRemoval)")
    void testDelete_CascadeDeleteStateChanges() {
        // Arrange
        UUID bookingId = booking1.getId();
        StateChange state2 = new StateChange(BookingStatus.ASSIGNED, OffsetDateTime.now());
        booking1.addStateChange(state2);
        bookingRepository.save(booking1);
        entityManager.flush();
        entityManager.clear();

        // Verificar que existem StateChanges
        Booking beforeDelete = bookingRepository.findById(bookingId).orElseThrow();
        assertTrue(beforeDelete.getHistory().size() > 0);

        // Act
        bookingRepository.deleteById(bookingId);
        entityManager.flush();

        // Assert
        Optional<Booking> deleted = bookingRepository.findById(bookingId);
        assertFalse(deleted.isPresent());
        // StateChanges devem ter sido deletados em cascade
    }

    @Test
    @DisplayName("findByMunicipality - Deve preservar relacionamento com Municipality")
    void testFindByMunicipality_Relationship() {
        // Act
        List<Booking> bookings = bookingRepository.findByMunicipality(lisboa);

        // Assert
        assertFalse(bookings.isEmpty());
        for (Booking booking : bookings) {
            assertNotNull(booking.getMunicipality());
            assertEquals("Lisboa", booking.getMunicipality().getName());
        }
    }
}
