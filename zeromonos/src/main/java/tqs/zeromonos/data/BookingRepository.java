package tqs.zeromonos.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

// JPA Repository for Bookings
public interface BookingRepository extends JpaRepository<Booking, UUID>{
    Optional<Booking> findByToken(String token);

    // Pesquisa os bookings dado o município ou apenas o seu nome
    List<Booking> findByMunicipality(Municipality municipality);
    List<Booking> findByMunicipalityName(String municipalityName);

    // Retorna a lista de bookings para um município num determinado período de um dia em concreto
    List<Booking> findByMunicipalityAndRequestedDateAndTimeSlot(Municipality municipality, LocalDate requestedDate, TimeSlot timeSlot);
    // Conta o total de bookings para um município num determinado período de um dia em concreto
    long countByMunicipalityAndRequestedDateAndTimeSlot(Municipality municipality, LocalDate requestedDate, TimeSlot timeSlot);

    // Retorna a lista de bookings num município para um determinado dia
    List<Booking> findByRequestedDateAndMunicipality(LocalDate requestedDate, Municipality municipality);

    // Conta o total de bookings de um município (independente da data)
    int countByMunicipality(Municipality municipality);
}
