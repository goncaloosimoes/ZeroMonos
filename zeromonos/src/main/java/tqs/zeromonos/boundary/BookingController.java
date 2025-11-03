package tqs.zeromonos.boundary;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;

/**
 * Controller REST para endpoints públicos de agendamento de recolha de
 * resíduos.
 * 
 * A configuração CORS é feita globalmente através de CorsConfig para permitir
 * acesso controlado a partir de origens específicas configuradas.
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Civilian", description = "Endpoints públicos para cidadãos")
public class BookingController {
    private BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Criar novo agendamento", description = "Permite criar um novo agendamento de recolha de resíduos volumosos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Limite de agendamentos atingido para o município")
    })
    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        BookingResponseDTO bookingResponse = bookingService.createBooking(request);
        return ResponseEntity.ok(bookingResponse);
    }

    @Operation(summary = "Consultar agendamento por token", description = "Retorna os detalhes de um agendamento usando o token único")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    @GetMapping("/{token}")
    public ResponseEntity<BookingResponseDTO> getBookingByToken(
            @Parameter(description = "Token único do agendamento", required = true) @PathVariable String token) {
        BookingResponseDTO bookingResponse = bookingService.getBookingByToken(token);
        return ResponseEntity.ok(bookingResponse);
    }

    @Operation(summary = "Cancelar agendamento", description = "Cancela um agendamento que ainda não foi iniciado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Agendamento cancelado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Agendamento não pode ser cancelado no estado atual")
    })
    @PutMapping("/{token}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @Parameter(description = "Token único do agendamento a cancelar", required = true) @PathVariable String token) {
        bookingService.cancelBooking(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar municípios disponíveis", description = "Retorna a lista de todos os municípios onde é possível agendar recolhas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de municípios retornada com sucesso")
    })
    @GetMapping("/municipalities")
    public ResponseEntity<List<String>> getAvailableMunicipalities() {
        List<String> municipalities = bookingService.getAvailableMunicipalities();
        return ResponseEntity.ok(municipalities);
    }
}
