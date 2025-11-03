package tqs.zeromonos.isolation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tqs.zeromonos.boundary.StaffBookingController;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.TimeSlot;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;

@WebMvcTest(StaffBookingController.class)
@DisplayName("Testes Unitários de StaffBookingController com MockMvc")
class StaffBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private BookingResponseDTO booking1;
    private BookingResponseDTO booking2;
    private BookingResponseDTO booking3;
    private List<BookingResponseDTO> allBookings;
    private LocalDate validDate;

    @BeforeEach
    void setUp() {
        // Criar data válida
        validDate = LocalDate.now().plusDays(1);
        if (validDate.getDayOfWeek().getValue() == 7) { // Se for domingo
            validDate = validDate.plusDays(1);
        }

        // Criar DTOs de resposta
        booking1 = new BookingResponseDTO();
        booking1.setId(UUID.randomUUID());
        booking1.setToken("token-1");
        booking1.setMunicipalityName("Lisboa");
        booking1.setDescription("Sofá velho");
        booking1.setRequestedDate(validDate);
        booking1.setTimeSlot(TimeSlot.MORNING);
        booking1.setStatus(BookingStatus.RECEIVED);
        booking1.setCreatedAt(OffsetDateTime.now());
        booking1.setHistory(new ArrayList<>());

        booking2 = new BookingResponseDTO();
        booking2.setId(UUID.randomUUID());
        booking2.setToken("token-2");
        booking2.setMunicipalityName("Lisboa");
        booking2.setDescription("Colchão");
        booking2.setRequestedDate(validDate);
        booking2.setTimeSlot(TimeSlot.AFTERNOON);
        booking2.setStatus(BookingStatus.ASSIGNED);
        booking2.setCreatedAt(OffsetDateTime.now());
        booking2.setHistory(new ArrayList<>());

        booking3 = new BookingResponseDTO();
        booking3.setId(UUID.randomUUID());
        booking3.setToken("token-3");
        booking3.setMunicipalityName("Porto");
        booking3.setDescription("Mesa de jantar");
        booking3.setRequestedDate(validDate.plusDays(1));
        booking3.setTimeSlot(TimeSlot.EVENING);
        booking3.setStatus(BookingStatus.RECEIVED);
        booking3.setCreatedAt(OffsetDateTime.now());
        booking3.setHistory(new ArrayList<>());

        allBookings = List.of(booking1, booking2, booking3);
    }

    // ==================== TESTES DE GET /api/staff/bookings ====================

    @Test
    @DisplayName("GET /api/staff/bookings - Deve retornar todos os bookings (200 OK)")
    void testListBookings_All() throws Exception {
        // Arrange
        when(bookingService.listForStaff(null)).thenReturn(allBookings);

        // Act & Assert
        mockMvc.perform(get("/api/staff/bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].token").value("token-1"))
                .andExpect(jsonPath("$[0].municipalityName").value("Lisboa"))
                .andExpect(jsonPath("$[1].token").value("token-2"))
                .andExpect(jsonPath("$[2].token").value("token-3"));

        verify(bookingService, times(1)).listForStaff(null);
    }

    @Test
    @DisplayName("GET /api/staff/bookings - Deve retornar lista vazia quando não há bookings")
    void testListBookings_Empty() throws Exception {
        // Arrange
        when(bookingService.listForStaff(null)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/staff/bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookingService, times(1)).listForStaff(null);
    }

    @Test
    @DisplayName("GET /api/staff/bookings?municipality=all - Deve retornar todos os bookings")
    void testListBookings_WithAllParameter() throws Exception {
        // Arrange
        when(bookingService.listForStaff("all")).thenReturn(allBookings);

        // Act & Assert
        mockMvc.perform(get("/api/staff/bookings")
                .param("municipality", "all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        verify(bookingService, times(1)).listForStaff("all");
    }

    @Test
    @DisplayName("GET /api/staff/bookings?municipality=Lisboa - Deve filtrar por município")
    void testListBookings_ByMunicipality() throws Exception {
        // Arrange
        List<BookingResponseDTO> lisboaBookings = List.of(booking1, booking2);
        when(bookingService.listForStaff("Lisboa")).thenReturn(lisboaBookings);

        // Act & Assert
        mockMvc.perform(get("/api/staff/bookings")
                .param("municipality", "Lisboa")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].municipalityName").value("Lisboa"))
                .andExpect(jsonPath("$[1].municipalityName").value("Lisboa"));

        verify(bookingService, times(1)).listForStaff("Lisboa");
    }

    @Test
    @DisplayName("GET /api/staff/bookings?municipality=Porto - Deve retornar bookings do Porto")
    void testListBookings_ByPorto() throws Exception {
        // Arrange
        List<BookingResponseDTO> portoBookings = List.of(booking3);
        when(bookingService.listForStaff("Porto")).thenReturn(portoBookings);

        // Act & Assert
        mockMvc.perform(get("/api/staff/bookings")
                .param("municipality", "Porto")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].municipalityName").value("Porto"));

        verify(bookingService, times(1)).listForStaff("Porto");
    }

    @Test
    @DisplayName("GET /api/staff/bookings?municipality=Inexistente - Deve retornar 404 quando município não existe")
    void testListBookings_MunicipalityNotFound() throws Exception {
        // Arrange
        when(bookingService.listForStaff("Inexistente"))
                .thenThrow(new NoSuchElementException("Município não encontrado: Inexistente"));

        // Act & Assert
        mockMvc.perform(get("/api/staff/bookings")
                .param("municipality", "Inexistente")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).listForStaff("Inexistente");
    }

    // ==================== TESTES DE PATCH /api/staff/bookings/{token}/status
    // ====================

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=ASSIGNED - Deve atualizar status (200 OK)")
    void testUpdateStatus_Success() throws Exception {
        // Arrange
        String token = "token-1";
        BookingStatus newStatus = BookingStatus.ASSIGNED;

        BookingResponseDTO updatedBooking = new BookingResponseDTO();
        updatedBooking.setId(booking1.getId());
        updatedBooking.setToken(token);
        updatedBooking.setMunicipalityName("Lisboa");
        updatedBooking.setStatus(newStatus);

        when(bookingService.updateBookingStatusForStaff(token, newStatus)).thenReturn(updatedBooking);

        // Act & Assert
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "ASSIGNED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(bookingService, times(1)).updateBookingStatusForStaff(token, newStatus);
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=IN_PROGRESS - Deve atualizar para IN_PROGRESS")
    void testUpdateStatus_ToInProgress() throws Exception {
        // Arrange
        String token = "token-1";
        BookingStatus newStatus = BookingStatus.IN_PROGRESS;

        BookingResponseDTO updatedBooking = new BookingResponseDTO();
        updatedBooking.setToken(token);
        updatedBooking.setStatus(newStatus);

        when(bookingService.updateBookingStatusForStaff(token, newStatus)).thenReturn(updatedBooking);

        // Act & Assert
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "IN_PROGRESS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(bookingService, times(1)).updateBookingStatusForStaff(token, newStatus);
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=COMPLETED - Deve atualizar para COMPLETED")
    void testUpdateStatus_ToCompleted() throws Exception {
        // Arrange
        String token = "token-2";
        BookingStatus newStatus = BookingStatus.COMPLETED;

        BookingResponseDTO updatedBooking = new BookingResponseDTO();
        updatedBooking.setToken(token);
        updatedBooking.setStatus(newStatus);

        when(bookingService.updateBookingStatusForStaff(token, newStatus)).thenReturn(updatedBooking);

        // Act & Assert
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "COMPLETED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(bookingService, times(1)).updateBookingStatusForStaff(token, newStatus);
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=ASSIGNED&note=Nota - Deve aceitar parâmetro note opcional")
    void testUpdateStatus_WithOptionalNote() throws Exception {
        // Arrange
        String token = "token-1";
        BookingStatus newStatus = BookingStatus.ASSIGNED;
        String note = "Nota de teste";

        BookingResponseDTO updatedBooking = new BookingResponseDTO();
        updatedBooking.setToken(token);
        updatedBooking.setStatus(newStatus);

        when(bookingService.updateBookingStatusForStaff(token, newStatus)).thenReturn(updatedBooking);

        // Act & Assert
        // O parâmetro note é aceito mas não é usado pelo serviço (apenas está na
        // assinatura)
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "ASSIGNED")
                .param("note", note)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(bookingService, times(1)).updateBookingStatusForStaff(token, newStatus);
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status - Deve retornar 400 quando status está faltando")
    void testUpdateStatus_MissingStatusParameter() throws Exception {
        // Arrange
        String token = "token-1";

        // Act & Assert
        // Quando o parâmetro status está faltando, Spring retorna 400
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400, "Deve retornar erro (status >= 400), mas retornou: " + status);
                });

        verify(bookingService, never()).updateBookingStatusForStaff(anyString(), any(BookingStatus.class));
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=ASSIGNED - Deve retornar 404 quando booking não existe")
    void testUpdateStatus_BookingNotFound() throws Exception {
        // Arrange
        String token = "token-inexistente";
        BookingStatus newStatus = BookingStatus.ASSIGNED;

        when(bookingService.updateBookingStatusForStaff(token, newStatus))
                .thenThrow(new NoSuchElementException("Agendamento não encontrado"));

        // Act & Assert
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "ASSIGNED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).updateBookingStatusForStaff(token, newStatus);
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=INVALID - Deve retornar erro quando status é inválido")
    void testUpdateStatus_InvalidStatus() throws Exception {
        // Arrange
        String token = "token-1";

        // Act & Assert
        // Status inválido retorna erro (400) porque não pode ser convertido para
        // BookingStatus enum
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "INVALID_STATUS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400, "Deve retornar erro (status >= 400), mas retornou: " + status);
                });

        verify(bookingService, never()).updateBookingStatusForStaff(anyString(), any(BookingStatus.class));
    }

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status?status=CANCELLED - Deve atualizar para CANCELLED")
    void testUpdateStatus_ToCancelled() throws Exception {
        // Arrange
        String token = "token-1";
        BookingStatus newStatus = BookingStatus.CANCELLED;

        BookingResponseDTO updatedBooking = new BookingResponseDTO();
        updatedBooking.setToken(token);
        updatedBooking.setStatus(newStatus);

        when(bookingService.updateBookingStatusForStaff(token, newStatus)).thenReturn(updatedBooking);

        // Act & Assert
        mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                .param("status", "CANCELLED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(bookingService, times(1)).updateBookingStatusForStaff(token, newStatus);
    }

    // ==================== TESTES DE TODOS OS STATUS ====================

    @Test
    @DisplayName("PATCH /api/staff/bookings/{token}/status - Deve aceitar todos os status válidos")
    void testUpdateStatus_AllValidStatuses() throws Exception {
        // Arrange
        String token = "token-1";
        BookingStatus[] allStatuses = BookingStatus.values();

        for (BookingStatus status : allStatuses) {
            BookingResponseDTO updatedBooking = new BookingResponseDTO();
            updatedBooking.setToken(token);
            updatedBooking.setStatus(status);

            when(bookingService.updateBookingStatusForStaff(token, status)).thenReturn(updatedBooking);

            // Act & Assert
            mockMvc.perform(patch("/api/staff/bookings/{token}/status", token)
                    .param("status", status.name())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(status.name()));

            verify(bookingService, times(1)).updateBookingStatusForStaff(token, status);
            reset(bookingService); // Reset para próximo loop
        }
    }
}
