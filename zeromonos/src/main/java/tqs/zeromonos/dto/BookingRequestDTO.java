package tqs.zeromonos.dto;

import tqs.zeromonos.data.TimeSlot;
import java.time.LocalDate;

public class BookingRequestDTO {
    private LocalDate requestedDate;
    private TimeSlot timeSlot;
    private String description;
    private String municipalityName;

    /**
     * Construtor vazio necessário para deserialização JSON.
     * Frameworks como Jackson (usado pelo Spring Boot) requerem um construtor
     * padrão
     * sem argumentos para converter JSON em objetos Java.
     */
    public BookingRequestDTO() {
        // Construtor vazio intencionalmente - necessário para deserialização JSON
    }

    // Getters and Setters
    public String getMunicipalityName() {
        return municipalityName;
    }

    public void setMunicipalityName(String municipalityName) {
        this.municipalityName = municipalityName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
