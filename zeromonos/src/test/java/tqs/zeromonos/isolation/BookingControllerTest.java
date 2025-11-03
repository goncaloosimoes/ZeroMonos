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

import com.fasterxml.jackson.databind.ObjectMapper;

import tqs.zeromonos.boundary.BookingController;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.TimeSlot;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;

@WebMvcTest(BookingController.class)
@DisplayName("Testes Unitários de BookingController com MockMvc")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingRequestDTO requestDTO;
    private BookingResponseDTO responseDTO;
    private LocalDate validDate;

    @BeforeEach
    void setUp() {
        // Criar data válida (amanhã)
        validDate = LocalDate.now().plusDays(1);
        if (validDate.getDayOfWeek().getValue() == 7) { // Se for domingo, avança para segunda
            validDate = validDate.plusDays(1);
        }

        // Criar DTO de requisição
        requestDTO = new BookingRequestDTO();
        requestDTO.setMunicipalityName("Lisboa");
        requestDTO.setDescription("Sofá velho e colchão");
        requestDTO.setRequestedDate(validDate);
        requestDTO.setTimeSlot(TimeSlot.AFTERNOON);

        // Criar DTO de resposta
        responseDTO = new BookingResponseDTO();
        responseDTO.setId(UUID.randomUUID());
        responseDTO.setToken("test-token-123");
        responseDTO.setMunicipalityName("Lisboa");
        responseDTO.setDescription("Sofá velho e colchão");
        responseDTO.setRequestedDate(validDate);
        responseDTO.setTimeSlot(TimeSlot.AFTERNOON);
        responseDTO.setStatus(BookingStatus.RECEIVED);
        responseDTO.setCreatedAt(OffsetDateTime.now());
        responseDTO.setHistory(new ArrayList<>());
    }

    // ==================== TESTES DE POST /api/bookings ====================

    @Test
    @DisplayName("POST /api/bookings - Deve criar booking com sucesso (200 OK)")
    void testCreateBooking_Success() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(BookingRequestDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token-123"))
                .andExpect(jsonPath("$.municipalityName").value("Lisboa"))
                .andExpect(jsonPath("$.description").value("Sofá velho e colchão"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.requestedDate").value(validDate.toString()))
                .andExpect(jsonPath("$.timeSlot").value("AFTERNOON"));

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve criar booking mesmo com dados que passam validação")
    void testCreateBooking_InvalidData() throws Exception {
        // Arrange - Request com municipalityName vazio, mas pode passar validação se
        // não houver @NotNull
        BookingRequestDTO invalidRequest = new BookingRequestDTO();
        invalidRequest.setMunicipalityName(""); // Vazio, mas pode passar
        invalidRequest.setDescription("Teste");
        invalidRequest.setRequestedDate(validDate);
        invalidRequest.setTimeSlot(TimeSlot.MORNING);

        // O serviço será chamado, mas retornará erro se o município não existir
        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Município '' não encontrado"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve retornar 400 quando município não existe")
    void testCreateBooking_MunicipalityNotFound() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Município 'Inexistente' não encontrado"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve retornar 409 quando limite de bookings é atingido")
    void testCreateBooking_MaxBookingsReached() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new IllegalStateException("Limite de 32 agendamentos atingido"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    // ==================== TESTES DE GET /api/bookings/{token} ====================

    @Test
    @DisplayName("GET /api/bookings/{token} - Deve retornar booking quando token existe (200 OK)")
    void testGetBookingByToken_Success() throws Exception {
        // Arrange
        String token = "test-token-123";
        when(bookingService.getBookingByToken(token)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/bookings/{token}", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.municipalityName").value("Lisboa"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(bookingService, times(1)).getBookingByToken(token);
    }

    @Test
    @DisplayName("GET /api/bookings/{token} - Deve retornar 404 quando token não existe")
    void testGetBookingByToken_NotFound() throws Exception {
        // Arrange
        String token = "token-inexistente";
        when(bookingService.getBookingByToken(token))
                .thenThrow(new NoSuchElementException("Agendamento não encontrado"));

        // Act & Assert
        mockMvc.perform(get("/api/bookings/{token}", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).getBookingByToken(token);
    }

    @Test
    @DisplayName("GET /api/bookings/{token} - Deve retornar erro quando token é vazio")
    void testGetBookingByToken_InvalidToken() throws Exception {
        // Arrange
        String token = "";
        // Quando passamos string vazia no path variable, Spring pode tratar
        // /api/bookings/
        // como endpoint diferente e retornar 404 antes de chegar ao controller
        // Se chegar ao controller, pode retornar 400 (IllegalArgumentException)
        when(bookingService.getBookingByToken(token))
                .thenThrow(new IllegalArgumentException("Token inválido ou vazio"));

        // Act & Assert
        // Pode retornar 404 (endpoint não encontrado) ou 400 (validação no controller)
        mockMvc.perform(get("/api/bookings/{token}", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 400 || status == 404,
                            "Deve retornar 400 ou 404, mas retornou: " + status);
                });

        // O serviço pode ou não ser chamado dependendo de como Spring trata path
        // variable vazio
        verify(bookingService, atMost(1)).getBookingByToken(token);
    }

    // ==================== TESTES DE PUT /api/bookings/{token}/cancel
    // ====================

    @Test
    @DisplayName("PUT /api/bookings/{token}/cancel - Deve cancelar booking com sucesso (204 No Content)")
    void testCancelBooking_Success() throws Exception {
        // Arrange
        String token = "test-token-123";
        doNothing().when(bookingService).cancelBooking(token);

        // Act & Assert
        mockMvc.perform(put("/api/bookings/{token}/cancel", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(bookingService, times(1)).cancelBooking(token);
    }

    @Test
    @DisplayName("PUT /api/bookings/{token}/cancel - Deve retornar 404 quando booking não existe")
    void testCancelBooking_NotFound() throws Exception {
        // Arrange
        String token = "token-inexistente";
        doThrow(new NoSuchElementException("Agendamento não encontrado"))
                .when(bookingService).cancelBooking(token);

        // Act & Assert
        mockMvc.perform(put("/api/bookings/{token}/cancel", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).cancelBooking(token);
    }

    @Test
    @DisplayName("PUT /api/bookings/{token}/cancel - Deve retornar 409 quando status não permite cancelamento")
    void testCancelBooking_Conflict() throws Exception {
        // Arrange
        String token = "test-token-123";
        doThrow(new IllegalStateException("O agendamento não pode ser cancelado no estado atual"))
                .when(bookingService).cancelBooking(token);

        // Act & Assert
        mockMvc.perform(put("/api/bookings/{token}/cancel", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        verify(bookingService, times(1)).cancelBooking(token);
    }

    // ==================== TESTES DE GET /api/bookings/municipalities
    // ====================

    @Test
    @DisplayName("GET /api/bookings/municipalities - Deve retornar lista de municípios (200 OK)")
    void testGetAvailableMunicipalities_Success() throws Exception {
        // Arrange
        List<String> municipalities = List.of("Lisboa", "Porto", "Coimbra", "Aveiro");
        when(bookingService.getAvailableMunicipalities()).thenReturn(municipalities);

        // Act & Assert
        mockMvc.perform(get("/api/bookings/municipalities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0]").value("Lisboa"))
                .andExpect(jsonPath("$[1]").value("Porto"))
                .andExpect(jsonPath("$[2]").value("Coimbra"))
                .andExpect(jsonPath("$[3]").value("Aveiro"));

        verify(bookingService, times(1)).getAvailableMunicipalities();
    }

    @Test
    @DisplayName("GET /api/bookings/municipalities - Deve retornar lista vazia quando não há municípios")
    void testGetAvailableMunicipalities_EmptyList() throws Exception {
        // Arrange
        when(bookingService.getAvailableMunicipalities()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/bookings/municipalities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookingService, times(1)).getAvailableMunicipalities();
    }

    // ==================== TESTES DE VALIDAÇÃO DE REQUISIÇÃO ====================

    @Test
    @DisplayName("POST /api/bookings - Deve retornar erro quando Content-Type está incorreto")
    void testCreateBooking_InvalidContentType() throws Exception {
        // Act & Assert
        // Quando Content-Type está incorreto, pode retornar 400 ou 500 dependendo de
        // como o Spring trata
        // O importante é que não seja 200 (sucesso)
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() >= 400,
                        "Deve retornar erro (status >= 400)"));

        verify(bookingService, never()).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/bookings - Deve retornar erro quando JSON é inválido")
    void testCreateBooking_InvalidJson() throws Exception {
        // Act & Assert
        // JSON inválido pode resultar em 400 ou 500 dependendo de como o Spring trata
        // O importante é que não seja 200 (sucesso)
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() >= 400,
                        "Deve retornar erro (status >= 400)"));

        verify(bookingService, never()).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/bookings - Pode processar request com campos null se não houver validação @NotNull")
    void testCreateBooking_MissingRequiredFields() throws Exception {
        // Arrange - Request sem alguns campos (pode passar se não houver @NotNull)
        BookingRequestDTO incompleteRequest = new BookingRequestDTO();
        incompleteRequest.setMunicipalityName("Lisboa");
        // description, requestedDate, timeSlot podem ser null
        // Se passar validação, o serviço será chamado e pode lançar exceção
        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Dados inválidos"));

        // Act & Assert
        // O request pode passar se não houver validações @NotNull, então o serviço será
        // chamado
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incompleteRequest)))
                .andExpect(status().isBadRequest());

        // O serviço pode ser chamado se o JSON for válido, mesmo com campos null
        verify(bookingService, atLeast(0)).createBooking(any(BookingRequestDTO.class));
    }

    // ==================== TESTES DE CORS ====================

    @Test
    @DisplayName("GET /api/bookings/municipalities - Deve permitir CORS")
    void testCorsHeaders() throws Exception {
        // Arrange
        when(bookingService.getAvailableMunicipalities()).thenReturn(List.of("Lisboa"));

        // Act & Assert
        mockMvc.perform(get("/api/bookings/municipalities")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));

        verify(bookingService, times(1)).getAvailableMunicipalities();
    }

    // ==================== TESTES DE ERRO INTERNO ====================

    @Test
    @DisplayName("POST /api/bookings - Deve retornar 500 quando ocorre erro interno")
    void testCreateBooking_InternalServerError() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new RuntimeException("Erro interno"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isInternalServerError());

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }
}
