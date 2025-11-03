package tqs.zeromonos.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tqs.zeromonos.data.Booking;
import tqs.zeromonos.data.BookingRepository;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;
import tqs.zeromonos.data.StateChange;
import tqs.zeromonos.data.TimeSlot;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingServiceImplementation;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários de BookingServiceImplementation com Mocks")
class BookingServiceImplementationTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @InjectMocks
    private BookingServiceImplementation bookingService;

    private Municipality mockMunicipality;
    private Booking mockBooking;
    private BookingRequestDTO requestDTO;
    private LocalDate validDate;

    @BeforeEach
    void setUp() {
        // Criar município mock
        mockMunicipality = new Municipality("Lisboa");

        // Criar data válida (amanhã, garantindo que não é domingo)
        validDate = LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1);
        if (validDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            validDate = validDate.plusDays(1);
        }

        // Criar booking mock
        mockBooking = new Booking(mockMunicipality, "Sofá velho", validDate, TimeSlot.AFTERNOON);
        // O token é gerado automaticamente no construtor
        mockBooking.setStatus(BookingStatus.RECEIVED);
        mockBooking.setCreatedAt(OffsetDateTime.now());

        // Adicionar StateChange inicial
        StateChange initialState = new StateChange(BookingStatus.RECEIVED, mockBooking.getCreatedAt());
        mockBooking.addStateChange(initialState);

        // Criar DTO de requisição
        requestDTO = new BookingRequestDTO();
        requestDTO.setMunicipalityName("Lisboa");
        requestDTO.setDescription("Sofá velho");
        requestDTO.setRequestedDate(validDate);
        requestDTO.setTimeSlot(TimeSlot.AFTERNOON);
    }

    // ==================== TESTES DE createBooking ====================

    @Test
    @DisplayName("createBooking - Deve criar booking válido com sucesso")
    void testCreateBooking_Success() {
        // Arrange
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(mockMunicipality));
        when(bookingRepository.countByMunicipality(mockMunicipality)).thenReturn(10);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponseDTO result = bookingService.createBooking(requestDTO);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals("Lisboa", result.getMunicipalityName());
        assertEquals("Sofá velho", result.getDescription());
        assertEquals(validDate, result.getRequestedDate());
        assertEquals(TimeSlot.AFTERNOON, result.getTimeSlot());
        assertEquals(BookingStatus.RECEIVED, result.getStatus());
        assertNotNull(result.getHistory());
        assertFalse(result.getHistory().isEmpty());

        verify(municipalityRepository, times(1)).findByName("Lisboa");
        verify(bookingRepository, times(1)).countByMunicipality(mockMunicipality);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - Deve lançar exceção quando município não existe")
    void testCreateBooking_MunicipalityNotFound() {
        // Arrange
        when(municipalityRepository.findByName("MunicipioInexistente")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(createRequestDTO("MunicipioInexistente", validDate)),
                "Deve lançar IllegalArgumentException quando município não existe");

        assertEquals("Município 'MunicipioInexistente' não encontrado", exception.getMessage());
        verify(municipalityRepository, times(1)).findByName("MunicipioInexistente");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - Deve lançar exceção quando data está no passado")
    void testCreateBooking_PastDate() {
        // Arrange
        LocalDate yesterday = LocalDate.now(ZoneId.of("Europe/Lisbon")).minusDays(1);
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(mockMunicipality));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(createRequestDTO("Lisboa", yesterday)),
                "Deve lançar IllegalArgumentException para data no passado");

        assertEquals("A data solicitada não pode ser no passado", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - Deve lançar exceção quando data é hoje")
    void testCreateBooking_TodayDate() {
        // Arrange
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Lisbon"));
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(mockMunicipality));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(createRequestDTO("Lisboa", today)),
                "Deve lançar IllegalArgumentException para data hoje");

        assertEquals("A data solicitada não pode ser no mesmo dia", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - Deve lançar exceção quando data é domingo")
    void testCreateBooking_SundayDate() {
        // Arrange
        LocalDate sunday = findNextSunday();
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(mockMunicipality));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(createRequestDTO("Lisboa", sunday)),
                "Deve lançar IllegalArgumentException para domingo");

        assertEquals("Não são feitas recolhas ao fim de semana", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - Deve lançar exceção quando limite de bookings é atingido")
    void testCreateBooking_MaxBookingsReached() {
        // Arrange
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(mockMunicipality));
        when(bookingRepository.countByMunicipality(mockMunicipality)).thenReturn(32);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(requestDTO),
                "Deve lançar IllegalStateException quando limite é atingido");

        assertEquals("Limite de 32 agendamentos atingido para o município 'Lisboa'", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ==================== TESTES DE getBookingByToken ====================

    @Test
    @DisplayName("getBookingByToken - Deve retornar booking quando token existe")
    void testGetBookingByToken_Success() {
        // Arrange
        String token = mockBooking.getToken(); // Usar o token gerado automaticamente
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));

        // Act
        BookingResponseDTO result = bookingService.getBookingByToken(token);

        // Assert
        assertNotNull(result);
        assertEquals(token, result.getToken());
        assertEquals("Lisboa", result.getMunicipalityName());
        assertEquals(BookingStatus.RECEIVED, result.getStatus());

        verify(bookingRepository, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar exceção quando token é null")
    void testGetBookingByToken_NullToken() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.getBookingByToken(null),
                "Deve lançar IllegalArgumentException para token null");

        assertEquals("Token inválido ou vazio", exception.getMessage());
        verify(bookingRepository, never()).findByToken(anyString());
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar exceção quando token é vazio")
    void testGetBookingByToken_EmptyToken() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.getBookingByToken("   "),
                "Deve lançar IllegalArgumentException para token vazio");

        assertEquals("Token inválido ou vazio", exception.getMessage());
        verify(bookingRepository, never()).findByToken(anyString());
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar exceção quando token não existe")
    void testGetBookingByToken_TokenNotFound() {
        // Arrange
        String token = "token-inexistente";
        when(bookingRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> bookingService.getBookingByToken(token),
                "Deve lançar NoSuchElementException quando token não existe");

        assertEquals("Agendamento não encontrado para o token fornecido", exception.getMessage());
        verify(bookingRepository, times(1)).findByToken(token);
    }

    // ==================== TESTES DE cancelBooking ====================

    @Test
    @DisplayName("cancelBooking - Deve cancelar booking com status RECEIVED")
    void testCancelBooking_WithReceivedStatus() {
        // Arrange
        String token = mockBooking.getToken();
        mockBooking.setStatus(BookingStatus.RECEIVED);
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        bookingService.cancelBooking(token);

        // Assert
        verify(bookingRepository, times(1)).findByToken(token);
        verify(bookingRepository, times(1)).save(any(Booking.class));
        assertEquals(BookingStatus.CANCELLED, mockBooking.getStatus());
    }

    @Test
    @DisplayName("cancelBooking - Deve cancelar booking com status ASSIGNED")
    void testCancelBooking_WithAssignedStatus() {
        // Arrange
        String token = mockBooking.getToken();
        mockBooking.setStatus(BookingStatus.ASSIGNED);
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        bookingService.cancelBooking(token);

        // Assert
        verify(bookingRepository, times(1)).findByToken(token);
        verify(bookingRepository, times(1)).save(any(Booking.class));
        assertEquals(BookingStatus.CANCELLED, mockBooking.getStatus());
    }

    @Test
    @DisplayName("cancelBooking - Deve lançar exceção quando booking não existe")
    void testCancelBooking_BookingNotFound() {
        // Arrange
        String token = "token-inexistente";
        when(bookingRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> bookingService.cancelBooking(token),
                "Deve lançar NoSuchElementException quando booking não existe");

        assertEquals("Agendamento não encontrado para o token fornecido", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("cancelBooking - Deve lançar exceção quando status não permite cancelamento")
    void testCancelBooking_InvalidStatus() {
        // Arrange
        String token = mockBooking.getToken();
        mockBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelBooking(token),
                "Deve lançar IllegalStateException quando status não permite cancelamento");

        assertEquals("O agendamento não pode ser cancelado no estado atual", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ==================== TESTES DE getAvailableMunicipalities
    // ====================

    @Test
    @DisplayName("getAvailableMunicipalities - Deve retornar lista de municípios")
    void testGetAvailableMunicipalities_Success() {
        // Arrange
        List<Municipality> municipalities = new ArrayList<>();
        municipalities.add(new Municipality("Lisboa"));
        municipalities.add(new Municipality("Porto"));
        municipalities.add(new Municipality("Coimbra"));
        when(municipalityRepository.findAll()).thenReturn(municipalities);

        // Act
        List<String> result = bookingService.getAvailableMunicipalities();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Lisboa"));
        assertTrue(result.contains("Porto"));
        assertTrue(result.contains("Coimbra"));

        verify(municipalityRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAvailableMunicipalities - Deve retornar lista vazia quando não há municípios")
    void testGetAvailableMunicipalities_EmptyList() {
        // Arrange
        when(municipalityRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<String> result = bookingService.getAvailableMunicipalities();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(municipalityRepository, times(1)).findAll();
    }

    // ==================== TESTES DE listForStaff ====================

    @Test
    @DisplayName("listForStaff - Deve retornar todos os bookings quando municipality é 'all'")
    void testListForStaff_AllMunicipalities() {
        // Arrange
        List<Booking> bookings = new ArrayList<>();
        bookings.add(mockBooking);
        when(bookingRepository.findAll()).thenReturn(bookings);

        // Act
        List<BookingResponseDTO> result = bookingService.listForStaff("all");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockBooking.getToken(), result.get(0).getToken());

        verify(bookingRepository, times(1)).findAll();
        verify(bookingRepository, never()).findByMunicipality(any());
    }

    @Test
    @DisplayName("listForStaff - Deve retornar bookings filtrados por município")
    void testListForStaff_ByMunicipality() {
        // Arrange
        List<Booking> bookings = new ArrayList<>();
        bookings.add(mockBooking);
        when(municipalityRepository.findByName("Lisboa")).thenReturn(Optional.of(mockMunicipality));
        when(bookingRepository.findByMunicipality(mockMunicipality)).thenReturn(bookings);

        // Act
        List<BookingResponseDTO> result = bookingService.listForStaff("Lisboa");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(municipalityRepository, times(1)).findByName("Lisboa");
        verify(bookingRepository, times(1)).findByMunicipality(mockMunicipality);
        verify(bookingRepository, never()).findAll();
    }

    @Test
    @DisplayName("listForStaff - Deve lançar exceção quando município não existe")
    void testListForStaff_MunicipalityNotFound() {
        // Arrange
        when(municipalityRepository.findByName("MunicipioInexistente")).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> bookingService.listForStaff("MunicipioInexistente"),
                "Deve lançar NoSuchElementException quando município não existe");

        assertEquals("Município não encontrado: MunicipioInexistente", exception.getMessage());
        verify(bookingRepository, never()).findByMunicipality(any());
    }

    @Test
    @DisplayName("listForStaff - Deve retornar lista vazia quando não há bookings")
    void testListForStaff_EmptyResult() {
        // Arrange
        when(bookingRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<BookingResponseDTO> result = bookingService.listForStaff(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(bookingRepository, times(1)).findAll();
    }

    // ==================== TESTES DE updateBookingStatusForStaff
    // ====================

    @Test
    @DisplayName("updateBookingStatusForStaff - Deve atualizar status com sucesso")
    void testUpdateBookingStatusForStaff_Success() {
        // Arrange
        String token = mockBooking.getToken();
        BookingStatus newStatus = BookingStatus.ASSIGNED;
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponseDTO result = bookingService.updateBookingStatusForStaff(token, newStatus);

        // Assert
        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());
        verify(bookingRepository, times(1)).findByToken(token);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingStatusForStaff - Deve lançar exceção quando booking não existe")
    void testUpdateBookingStatusForStaff_BookingNotFound() {
        // Arrange
        String token = "token-inexistente";
        when(bookingRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> bookingService.updateBookingStatusForStaff(token, BookingStatus.ASSIGNED),
                "Deve lançar NoSuchElementException quando booking não existe");

        assertEquals("Agendamento não encontrado", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("updateBookingStatusForStaff - Deve adicionar StateChange ao histórico")
    void testUpdateBookingStatusForStaff_AddsToHistory() {
        // Arrange
        String token = mockBooking.getToken();
        BookingStatus newStatus = BookingStatus.IN_PROGRESS;
        int initialHistorySize = mockBooking.getHistory().size();
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        bookingService.updateBookingStatusForStaff(token, newStatus);

        // Assert
        assertTrue(mockBooking.getHistory().size() > initialHistorySize);
        assertEquals(newStatus, mockBooking.getStatus());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private BookingRequestDTO createRequestDTO(String municipalityName, LocalDate date) {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setMunicipalityName(municipalityName);
        dto.setDescription("Teste");
        dto.setRequestedDate(date);
        dto.setTimeSlot(TimeSlot.MORNING);
        return dto;
    }

    private LocalDate findNextSunday() {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
