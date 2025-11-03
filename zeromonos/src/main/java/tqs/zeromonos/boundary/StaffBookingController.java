package tqs.zeromonos.boundary;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.dto.BookingResponseDTO;
import tqs.zeromonos.services.BookingService;

@RestController
@RequestMapping("/api/staff/bookings")
@Tag(name = "Staff", description = "Endpoints administrativos para gestão de agendamentos")
public class StaffBookingController {
    private final BookingService bookingService;

    public StaffBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Listar agendamentos", description = "Retorna todos os agendamentos, opcionalmente filtrados por município")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Município não encontrado")
    })
    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> listBookings(
            @Parameter(description = "Nome do município para filtrar (opcional)", required = false) 
            @RequestParam(value = "municipality", required = false) String municipalityName) {
        List<BookingResponseDTO> responseList = bookingService.listForStaff(municipalityName);
        return ResponseEntity.ok(responseList);
    }

    @Operation(summary = "Atualizar estado do agendamento", description = "Permite atualizar o estado de um agendamento (ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Status inválido ou parâmetros em falta"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    @PatchMapping("/{token}/status")
    public ResponseEntity<BookingResponseDTO> updateStatus(
        @Parameter(description = "Token único do agendamento", required = true) @PathVariable String token,
        @Parameter(description = "Novo estado (ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED)", required = true) 
        @RequestParam(value = "status") BookingStatus status,
        @Parameter(description = "Nota opcional sobre a mudança de estado", required = false) 
        @RequestParam(value = "note", required = false) String note
    ) {
        BookingResponseDTO bookingResponse = bookingService.updateBookingStatusForStaff(token, status);
        return ResponseEntity.ok(bookingResponse);
    }

}
