package tqs.zeromonos.data;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_state_changes")
public class StateChange {
    @Id
    @GeneratedValue
    private UUID id;

    // Um booking pode ter várias mudanças de estado, mas cada mudança de estado estará sempre associada a um único booking
    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    public StateChange() {
    }

    public StateChange(BookingStatus status, OffsetDateTime timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public UUID getUuid() {
        return id;
    }
    // Não fazemos setter do id porque é auto-gerado

    public Booking getBooking() {
        return booking;
    }
    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public BookingStatus getStatus() {
        return status;
    }
    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
