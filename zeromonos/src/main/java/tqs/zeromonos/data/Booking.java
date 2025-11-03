package tqs.zeromonos.data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String token; // Este token é usado para consultar ou mudar o estado da reserva

    // Cada reserva está obrigatoriamoente associada a um município, mas um
    // município pode ter várias reservas
    @ManyToOne(optional = false)
    @JoinColumn(name = "municipality_id")
    private Municipality municipality;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate requestedDate; // Data para a qual a reserva é solicitada

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlot timeSlot; // Período do dia para a reserva

    @Enumerated(EnumType.STRING)
    private BookingStatus status; // Estado atual da reserva

    @Column(nullable = false)
    private OffsetDateTime createdAt; // Timestamp de quando o pedido foi criado
    private OffsetDateTime updatedAt; // Timestamp de quando o pedido foi atualizado pela última vez

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StateChange> history = new ArrayList<>(); // Histórico de mudanças de estado

    // Construtor padrão necessário para JPA/Hibernate
    public Booking() {
        // Construtor vazio para JPA
    }

    // Construtor usado na criação de uma nova reserva: gera um token único,
    // inicializa o estado para RECEIVED
    public Booking(Municipality municipality, String description, LocalDate requestedDate, TimeSlot timeSlot) {
        this.token = UUID.randomUUID().toString();
        this.municipality = municipality;
        this.description = description;
        this.requestedDate = requestedDate;
        this.timeSlot = timeSlot;
        this.status = BookingStatus.RECEIVED;
        this.createdAt = OffsetDateTime.now();
    }

    // Regista uma alteração de estado e atualiza o timestamp e status atuais
    public void addStateChange(StateChange stateChange) {
        stateChange.setBooking(this);
        history.add(stateChange);
        this.updatedAt = stateChange.getTimestamp();
        this.status = stateChange.getStatus();
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Municipality getMunicipality() {
        return municipality;
    }

    public void setMunicipality(Municipality municipality) {
        this.municipality = municipality;
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

    public List<StateChange> getHistory() {
        return history;
    }
}
