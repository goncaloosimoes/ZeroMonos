package tqs.zeromonos.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// JPA Repository for Municipalities
public interface MunicipalityRepository extends JpaRepository<Municipality, Long>{
    Optional<Municipality> findByName(String municipalityName);
}