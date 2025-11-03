package tqs.zeromonos.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tqs.zeromonos.data.Booking;
import tqs.zeromonos.data.BookingRepository;
import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.data.Municipality;
import tqs.zeromonos.data.MunicipalityRepository;
import tqs.zeromonos.data.StateChange;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;

@Service
public class BookingServiceImplementation implements BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImplementation.class);

    private BookingRepository bookingRepository;
    private MunicipalityRepository municipalityRepository;
    private int maxBookingsPerMunicipality;

    public BookingServiceImplementation(BookingRepository bookingRepository,
            MunicipalityRepository municipalityRepository) {
        this.bookingRepository = bookingRepository;
        this.municipalityRepository = municipalityRepository;
        this.maxBookingsPerMunicipality = 32;
    }

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        logger.info("Iniciando criação de reserva para município '{}'", request.getMunicipalityName());

        // Buscar município — lança exceção se não existir
        Municipality municipality = municipalityRepository.findByName(request.getMunicipalityName())
                .orElseThrow(() -> {
                    String msg = "Município '" + request.getMunicipalityName() + "' não encontrado";
                    logger.error(msg);
                    return new IllegalArgumentException(msg);
                });

        logger.debug("Município '{}' encontrado na base de dados", municipality.getName());

        // Validar data da reserva
        validateDateOrThrow(request.getRequestedDate());

        // Verificar limite máximo de agendamentos para o município
        int totalBookings = bookingRepository.countByMunicipality(municipality);
        if (totalBookings >= maxBookingsPerMunicipality) {
            String msg = String.format("Limite de %d agendamentos atingido para o município '%s'",
                    maxBookingsPerMunicipality, municipality.getName());
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }

        // Criar e persistir reserva
        Booking newBooking = new Booking(
                municipality,
                request.getDescription(),
                request.getRequestedDate(),
                request.getTimeSlot());

        // Criar StateChange inicial para RECEIVED
        StateChange initialStateChange = new StateChange(
                BookingStatus.RECEIVED,
                newBooking.getCreatedAt());
        newBooking.addStateChange(initialStateChange);

        bookingRepository.save(newBooking);

        logger.info("Reserva criada com sucesso para '{}', data {}",
                municipality.getName(), request.getRequestedDate());
        logger.debug("Reserva criada com token: {}", newBooking.getToken());
        logger.debug("Histórico inicial: {} entradas", newBooking.getHistory().size());

        // Converter para DTO com tratamento de erro
        try {
            return BookingResponseDTO.fromEntity(newBooking);
        } catch (Exception e) {
            logger.error("Erro ao converter nova reserva para DTO:", e);
            throw new IllegalStateException("Erro ao criar reserva: " + e.getMessage(), e);
        }
    }

    private void validateDateOrThrow(LocalDate requestedDate) {
        ZoneId zone = ZoneId.of("Europe/Lisbon");
        LocalDate today = LocalDate.now(zone);

        if (requestedDate.isBefore(today)) {
            throw new IllegalArgumentException("A data solicitada não pode ser no passado");
        }
        if (requestedDate.isEqual(today)) {
            throw new IllegalArgumentException("A data solicitada não pode ser no mesmo dia");
        }
        if (requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Não são feitas recolhas ao fim de semana");
        }
    }

    @Override
    public BookingResponseDTO getBookingByToken(String token) {
        logger.info("=== GET /api/bookings/{} ===", token);
        logger.info("Buscando reserva com token: {}", token);
        logger.info("Tamanho do token: {}", token != null ? token.length() : 0);

        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warn("⚠️ Token inválido ou vazio");
                throw new IllegalArgumentException("Token inválido ou vazio");
            }

            String cleanToken = token.trim();
            logger.debug("Token limpo: '{}'", cleanToken);

            Optional<Booking> bookingOpt = bookingRepository.findByToken(cleanToken);
            logger.debug("Resultado da busca no repositório: {}",
                    bookingOpt.isPresent() ? "encontrado" : "não encontrado");

            if (bookingOpt.isEmpty()) {
                logger.warn("⚠️ Agendamento não encontrado para token: {}", cleanToken);
                throw new NoSuchElementException("Agendamento não encontrado para o token fornecido");
            }

            Booking booking = bookingOpt.get();
            logger.info("✅ Reserva encontrada:");
            logger.info("  - ID: {}", booking.getId());
            logger.info("  - Token: {}", booking.getToken());
            logger.info("  - Município: {}",
                    booking.getMunicipality() != null ? booking.getMunicipality().getName() : "N/A");
            logger.info("  - Data: {}", booking.getRequestedDate());
            logger.info("  - Status: {}", booking.getStatus());
            logger.info("  - TimeSlot: {}", booking.getTimeSlot());
            logger.info("  - History size: {}", booking.getHistory() != null ? booking.getHistory().size() : 0);

            // Converter para DTO com tratamento de erro
            try {
                logger.debug("Iniciando conversão para DTO...");
                BookingResponseDTO dto = BookingResponseDTO.fromEntity(booking);

                if (dto == null) {
                    logger.error("❌ Erro: DTO é null após conversão!");
                    throw new IllegalStateException("Erro ao converter reserva para DTO");
                }

                logger.info("✅ DTO criado com sucesso:");
                logger.info("  - Token: {}", dto.getToken());
                logger.info("  - Status: {}", dto.getStatus());
                logger.info("  - History: {}", dto.getHistory() != null ? dto.getHistory().size() : 0);

                return dto;
            } catch (Exception dtoError) {
                logger.error("❌ Erro ao converter Booking para DTO:", dtoError);
                logger.error("  - Mensagem: {}", dtoError.getMessage());
                logger.error("  - Causa: {}", dtoError.getCause());
                throw new IllegalStateException("Erro ao converter reserva para DTO: " + dtoError.getMessage(),
                        dtoError);
            }

        } catch (IllegalArgumentException | NoSuchElementException e) {
            // Re-throw exceções esperadas
            throw e;
        } catch (Exception e) {
            logger.error("❌ Erro inesperado ao buscar reserva por token:", e);
            logger.error("  - Mensagem: {}", e.getMessage());
            logger.error("  - Causa: {}", e.getCause());
            if (e.getCause() != null) {
                logger.error("  - Causa da causa: {}", e.getCause().getMessage());
            }
            throw new IllegalStateException("Erro ao buscar reserva: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelBooking(String token) {
        logger.info("Tentativa de cancelamento de reserva com token: {}", token);

        Booking booking = bookingRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.error("Agendamento não encontrado para token: {}", token);
                    return new NoSuchElementException("Agendamento não encontrado para o token fornecido");
                });

        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.RECEIVED || status == BookingStatus.ASSIGNED) {
            StateChange stateChange = new StateChange(BookingStatus.CANCELLED, java.time.OffsetDateTime.now());
            booking.addStateChange(stateChange);
            bookingRepository.save(booking);
            logger.info("Agendamento com token '{}' cancelado com sucesso", token);
        } else {
            logger.warn("Não é possível cancelar agendamento com token '{}' no estado {}", token, status);
            throw new IllegalStateException("O agendamento não pode ser cancelado no estado atual");
        }
    }

    @Override
    public List<String> getAvailableMunicipalities() {
        logger.info("=== GET /api/bookings/municipalities ===");
        logger.info("Buscando municípios disponíveis no repositório...");

        List<Municipality> municipalities = municipalityRepository.findAll();
        logger.info("Municípios encontrados na base de dados: {}", municipalities.size());

        if (municipalities.isEmpty()) {
            logger.warn("⚠️ Nenhum município disponível encontrado na base de dados!");
            logger.warn("⚠️ Verifique se MunicipalityImportService carregou os municípios corretamente.");
            return List.of(); // Retorna lista vazia
        }

        List<String> municipalityNames = municipalities.stream()
                .map(Municipality::getName)
                .collect(Collectors.toList());

        logger.info("Total de municípios retornados: {}", municipalityNames.size());
        logger.debug("Primeiros 5 municípios: {}", municipalityNames.stream().limit(5).collect(Collectors.toList()));

        return municipalityNames;
    }

    // Staff: lista bookings de um município
    @Override
    public List<BookingResponseDTO> listForStaff(String municipalityName) {
        logger.info("=== GET /api/staff/bookings ===");
        logger.info("Buscando reservas para município: {}", municipalityName);

        List<Booking> bookings;

        if (municipalityName == null || "all".equalsIgnoreCase(municipalityName) || municipalityName.isEmpty()) {
            logger.info("Listando TODAS as reservas (município: 'all')");
            bookings = bookingRepository.findAll();
            logger.info("Total de reservas encontradas: {}", bookings.size());
        } else {
            logger.info("Filtrando por município: '{}'", municipalityName);
            Optional<Municipality> municipalityOpt = municipalityRepository.findByName(municipalityName);

            if (municipalityOpt.isEmpty()) {
                logger.warn("⚠️ Município '{}' não encontrado na base de dados", municipalityName);
                throw new NoSuchElementException("Município não encontrado: " + municipalityName);
            }

            Municipality municipality = municipalityOpt.get();
            logger.info("Município encontrado: {}", municipality.getName());
            bookings = bookingRepository.findByMunicipality(municipality);
            logger.info("Total de reservas encontradas para '{}': {}", municipalityName, bookings.size());
        }

        List<BookingResponseDTO> result = bookings.stream()
                .map(booking -> {
                    logger.debug("Convertendo reserva: token={}, municipio={}, status={}",
                            booking.getToken(),
                            booking.getMunicipality() != null ? booking.getMunicipality().getName() : "N/A",
                            booking.getStatus());
                    return BookingResponseDTO.fromEntity(booking);
                })
                .toList();

        logger.info("Total de DTOs retornados: {}", result.size());
        return result;
    }

    // Staff: atualiza status de um booking
    @Override
    public BookingResponseDTO updateBookingStatusForStaff(String token, BookingStatus newStatus) {
        logger.info("Atualizando status da reserva com token: {} para {}", token, newStatus);

        var booking = bookingRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Agendamento não encontrado"));

        // Cria e adiciona mudança de estado
        OffsetDateTime ts = java.time.OffsetDateTime.now();
        var stateChange = new StateChange(newStatus, ts);
        booking.addStateChange(stateChange);

        bookingRepository.save(booking);
        logger.info("{} : Status da reserva com token '{}' atualizado para {}", ts, token, newStatus);

        return BookingResponseDTO.fromEntity(booking);
    }

}
