package tqs.zeromonos.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import tqs.zeromonos.data.Booking;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.TimeSlot;
import tqs.zeromonos.utils.HistoryMapper;

public class BookingResponseDTO {
    private UUID id;
    private String token;
    private String municipalityName;
    private String description;
    private LocalDate requestedDate;
    private TimeSlot timeSlot;
    private BookingStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<String> history;

    public BookingResponseDTO() {

    }

    public static BookingResponseDTO fromEntity(Booking booking) {
        if (booking == null) {
            return null;
        }

        try {
            BookingResponseDTO bookingResponseDTO = new BookingResponseDTO();

            // ID
            bookingResponseDTO.setId(booking.getId());

            // Token
            bookingResponseDTO.setToken(booking.getToken());

            // Município - Evita NullPointerException se a municipalidade tenha sido
            // removida
            if (booking.getMunicipality() != null) {
                bookingResponseDTO.setMunicipalityName(booking.getMunicipality().getName());
            } else {
                bookingResponseDTO.setMunicipalityName(null);
            }

            // Descrição
            bookingResponseDTO.setDescription(booking.getDescription());

            // Data
            bookingResponseDTO.setRequestedDate(booking.getRequestedDate());

            // TimeSlot
            bookingResponseDTO.setTimeSlot(booking.getTimeSlot());

            // Status
            bookingResponseDTO.setStatus(booking.getStatus());

            // Timestamps
            bookingResponseDTO.setCreatedAt(booking.getCreatedAt());
            bookingResponseDTO.setUpdatedAt(booking.getUpdatedAt());

            // Mapeia o histórico de mudanças de estado para uma lista de strings legíveis
            bookingResponseDTO.setHistory(HistoryMapper.mapHistoryToStrings(booking.getHistory()));

            return bookingResponseDTO;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter Booking para DTO: " + e.getMessage(), e);
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMunicipalityName() {
        return municipalityName;
    }

    public void setMunicipalityName(String municipalityName) {
        this.municipalityName = municipalityName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }
}
