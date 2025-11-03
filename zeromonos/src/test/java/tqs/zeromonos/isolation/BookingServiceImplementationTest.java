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
import org.mockito.MockedStatic;
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
import tqs.zeromonos.dto.DtoConversionException;
import tqs.zeromonos.services.BookingServiceException;
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
        BookingRequestDTO request = createRequestDTO("MunicipioInexistente", validDate);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request),
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
        BookingRequestDTO request = createRequestDTO("Lisboa", yesterday);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request),
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
        BookingRequestDTO request = createRequestDTO("Lisboa", today);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request),
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
        BookingRequestDTO request = createRequestDTO("Lisboa", sunday);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(request),
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
    @DisplayName("getBookingByToken - Deve lançar BookingServiceException quando ocorre erro na conversão")
    void testGetBookingByToken_ConversionError() {
        // Arrange
        String token = mockBooking.getToken();

        // Criar um Booking que cause erro na conversão
        // Usar um spy para fazer getHistory() lançar uma exceção genérica durante a
        // conversão
        Booking problematicBooking = spy(mockBooking);
        when(problematicBooking.getHistory()).thenThrow(new RuntimeException("Erro ao obter histórico"));
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(problematicBooking));

        // Act & Assert
        // O erro em getHistory() será apanhado por fromEntity e lançado como
        // DtoConversionException
        // que será apanhado pelo catch de Exception em getBookingByToken e lançado como
        // BookingServiceException
        BookingServiceException exception = assertThrows(
                BookingServiceException.class,
                () -> bookingService.getBookingByToken(token),
                "Deve lançar BookingServiceException quando ocorre erro na conversão");

        assertTrue(exception.getMessage().contains("Erro ao buscar reserva por token"));
        assertNotNull(exception.getCause());
        verify(bookingRepository, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar BookingServiceException quando ocorre erro inesperado na conversão")
    void testGetBookingByToken_UnexpectedConversionError() {
        // Arrange
        String token = mockBooking.getToken();

        // Criar um Booking que cause erro durante a conversão
        // Usar um spy para fazer getMunicipality().getName() lançar uma exceção
        // genérica
        Booking problematicBooking = spy(mockBooking);
        Municipality mockMunicipalitySpy = spy(mockMunicipality);
        when(problematicBooking.getMunicipality()).thenReturn(mockMunicipalitySpy);
        when(mockMunicipalitySpy.getName()).thenThrow(new RuntimeException("Erro ao obter nome do município"));
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(problematicBooking));

        // Act & Assert
        // O erro em getName() será apanhado por fromEntity e lançado como
        // DtoConversionException
        // que será apanhado pelo catch de Exception em getBookingByToken e lançado como
        // BookingServiceException
        BookingServiceException exception = assertThrows(
                BookingServiceException.class,
                () -> bookingService.getBookingByToken(token),
                "Deve lançar BookingServiceException quando ocorre erro inesperado na conversão");

        assertTrue(exception.getMessage().contains("Erro ao buscar reserva por token"));
        assertNotNull(exception.getCause());
        verify(bookingRepository, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar BookingServiceException quando ocorre DtoConversionException na conversão")
    void testGetBookingByToken_DtoConversionException() {
        // Arrange
        String token = mockBooking.getToken();
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));

        // Criar uma DtoConversionException com causa
        RuntimeException rootCause = new RuntimeException("Erro ao aceder propriedade do Booking");
        DtoConversionException dtoConversionException = new DtoConversionException(
                "Erro ao converter Booking para DTO", rootCause);

        // Usar MockedStatic para fazer mock do método estático fromEntity
        try (MockedStatic<BookingResponseDTO> mockedStatic = mockStatic(BookingResponseDTO.class)) {
            // Fazer com que fromEntity lance uma DtoConversionException
            mockedStatic.when(() -> BookingResponseDTO.fromEntity(mockBooking))
                    .thenThrow(dtoConversionException);

            // Act & Assert
            // A DtoConversionException será apanhada pelo catch de DtoConversionException
            // em convertBookingToDto e re-lançada com contexto adicional,
            // que será apanhada pelo catch de Exception em getBookingByToken e lançada como
            // BookingServiceException
            BookingServiceException exception = assertThrows(
                    BookingServiceException.class,
                    () -> bookingService.getBookingByToken(token),
                    "Deve lançar BookingServiceException quando ocorre DtoConversionException na conversão");

            assertTrue(exception.getMessage().contains("Erro ao buscar reserva por token"));
            assertNotNull(exception.getCause());
            // Verificar que a causa é uma DtoConversionException com mensagem modificada
            assertTrue(exception.getCause() instanceof DtoConversionException);
            assertTrue(exception.getCause().getMessage()
                    .contains("Erro ao converter reserva para DTO durante processamento"));
            // Verificar que a causa original foi preservada
            // A causa da nova DtoConversionException é a DtoConversionException original
            assertNotNull(exception.getCause().getCause());
            assertTrue(exception.getCause().getCause() instanceof DtoConversionException);
            assertEquals("Erro ao converter Booking para DTO", exception.getCause().getCause().getMessage());
            // Verificar que a causa raiz original foi preservada
            assertNotNull(exception.getCause().getCause().getCause());
            assertEquals(rootCause, exception.getCause().getCause().getCause());
            verify(bookingRepository, times(1)).findByToken(token);
        }
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar BookingServiceException quando ocorre erro genérico após conversão")
    void testGetBookingByToken_GenericExceptionAfterConversion() {
        // Arrange
        String token = mockBooking.getToken();
        when(bookingRepository.findByToken(token)).thenReturn(Optional.of(mockBooking));

        // Criar um mock do DTO que lance uma exceção genérica quando acedido
        BookingResponseDTO problematicDto = mock(BookingResponseDTO.class);
        when(problematicDto.getToken()).thenThrow(new RuntimeException("Erro inesperado ao aceder token"));

        // Usar MockedStatic para fazer mock do método estático fromEntity
        try (MockedStatic<BookingResponseDTO> mockedStatic = mockStatic(BookingResponseDTO.class)) {
            // Fazer com que fromEntity retorne o DTO problemático
            mockedStatic.when(() -> BookingResponseDTO.fromEntity(mockBooking)).thenReturn(problematicDto);

            // Act & Assert
            // O erro ao aceder getToken() será apanhado pelo catch de Exception genérica
            // em convertBookingToDto e lançado como DtoConversionException,
            // que será apanhado pelo catch de Exception em getBookingByToken e lançado como
            // BookingServiceException
            BookingServiceException exception = assertThrows(
                    BookingServiceException.class,
                    () -> bookingService.getBookingByToken(token),
                    "Deve lançar BookingServiceException quando ocorre erro genérico após conversão");

            assertTrue(exception.getMessage().contains("Erro ao buscar reserva por token"));
            assertNotNull(exception.getCause());
            // Verificar que a causa é uma DtoConversionException
            assertTrue(exception.getCause() instanceof DtoConversionException);
            assertTrue(exception.getCause().getMessage().contains("Erro inesperado ao converter reserva para DTO"));
            verify(bookingRepository, times(1)).findByToken(token);
        }
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

    @Test
    @DisplayName("getBookingByToken - Deve lançar BookingServiceException quando ocorre erro inesperado")
    void testGetBookingByToken_UnexpectedError() {
        // Arrange
        String token = "valid-token";
        // Simular um erro inesperado do repositório (ex: erro de base de dados)
        when(bookingRepository.findByToken(token))
                .thenThrow(new RuntimeException("Erro inesperado de base de dados"));

        // Act & Assert
        BookingServiceException exception = assertThrows(
                BookingServiceException.class,
                () -> bookingService.getBookingByToken(token),
                "Deve lançar BookingServiceException quando ocorre erro inesperado");

        assertTrue(exception.getMessage().contains("Erro ao buscar reserva por token"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Erro inesperado de base de dados", exception.getCause().getMessage());
        verify(bookingRepository, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("getBookingByToken - Deve lançar BookingServiceException com causa aninhada quando ocorre erro inesperado")
    void testGetBookingByToken_UnexpectedErrorWithNestedCause() {
        // Arrange
        String token = "valid-token";
        // Simular um erro inesperado com causa aninhada
        RuntimeException nestedCause = new RuntimeException("Causa raiz");
        RuntimeException rootCause = new RuntimeException("Erro de base de dados", nestedCause);
        when(bookingRepository.findByToken(token)).thenThrow(rootCause);

        // Act & Assert
        BookingServiceException exception = assertThrows(
                BookingServiceException.class,
                () -> bookingService.getBookingByToken(token),
                "Deve lançar BookingServiceException quando ocorre erro inesperado com causa aninhada");

        assertTrue(exception.getMessage().contains("Erro ao buscar reserva por token"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Erro de base de dados", exception.getCause().getMessage());
        assertNotNull(exception.getCause().getCause());
        assertEquals("Causa raiz", exception.getCause().getCause().getMessage());
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

    // ==================== TESTES DE BookingResponseDTO.fromEntity
    // ====================

    @Test
    @DisplayName("BookingResponseDTO.fromEntity - Deve lançar DtoConversionException quando ocorre erro genérico durante conversão")
    void testBookingResponseDTOFromEntity_GenericException() {
        // Arrange
        // Criar um Booking que cause erro durante a conversão
        // Usar um spy para fazer getId() lançar uma exceção genérica
        Booking problematicBooking = spy(mockBooking);
        when(problematicBooking.getId()).thenThrow(new RuntimeException("Erro ao obter ID do Booking"));

        // Act & Assert
        // O erro em getId() será apanhado pelo catch de Exception genérica
        // em fromEntity e lançado como DtoConversionException
        DtoConversionException exception = assertThrows(
                DtoConversionException.class,
                () -> BookingResponseDTO.fromEntity(problematicBooking),
                "Deve lançar DtoConversionException quando ocorre erro genérico durante conversão");

        assertTrue(exception.getMessage().contains("Erro ao converter Booking para DTO"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Erro ao obter ID do Booking", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("BookingResponseDTO.fromEntity - Deve lançar DtoConversionException quando HistoryMapper lança exceção")
    void testBookingResponseDTOFromEntity_HistoryMapperException() {
        // Arrange
        // Criar um Booking que cause erro no HistoryMapper
        // Usar um spy para fazer getHistory() lançar uma exceção genérica
        Booking problematicBooking = spy(mockBooking);
        when(problematicBooking.getHistory()).thenThrow(new RuntimeException("Erro ao obter histórico"));

        // Act & Assert
        // O erro em getHistory() será apanhado pelo catch de Exception genérica
        // em fromEntity e lançado como DtoConversionException
        DtoConversionException exception = assertThrows(
                DtoConversionException.class,
                () -> BookingResponseDTO.fromEntity(problematicBooking),
                "Deve lançar DtoConversionException quando HistoryMapper lança exceção");

        assertTrue(exception.getMessage().contains("Erro ao converter Booking para DTO"));
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Erro ao obter histórico", exception.getCause().getMessage());
    }

    // ==================== TESTES DE BookingServiceException ====================

    @Test
    @DisplayName("BookingServiceException - Construtor com mensagem deve criar exceção corretamente")
    void testBookingServiceException_WithMessage() {
        // Arrange & Act
        String message = "Erro no serviço de reservas";
        BookingServiceException exception = new BookingServiceException(message);

        // Assert
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("BookingServiceException - Construtor apenas com causa deve criar exceção corretamente")
    void testBookingServiceException_WithCauseOnly() {
        // Arrange
        Throwable cause = new RuntimeException("Causa raiz");

        // Act
        BookingServiceException exception = new BookingServiceException(cause);

        // Assert
        assertNotNull(exception);
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        assertEquals("Causa raiz", exception.getCause().getMessage());
    }

    // ==================== TESTES DE DtoConversionException ====================

    @Test
    @DisplayName("DtoConversionException - Construtor com mensagem deve criar exceção corretamente")
    void testDtoConversionException_WithMessage() {
        // Arrange & Act
        String message = "Erro ao converter Booking para DTO";
        DtoConversionException exception = new DtoConversionException(message);

        // Assert
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("DtoConversionException - Construtor apenas com causa deve criar exceção corretamente")
    void testDtoConversionException_WithCauseOnly() {
        // Arrange
        Throwable cause = new RuntimeException("Causa raiz");

        // Act
        DtoConversionException exception = new DtoConversionException(cause);

        // Assert
        assertNotNull(exception);
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        assertEquals("Causa raiz", exception.getCause().getMessage());
    }
}
