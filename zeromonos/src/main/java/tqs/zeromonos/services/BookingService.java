package tqs.zeromonos.services;

import java.util.List;

import tqs.zeromonos.data.BookingStatus;
import tqs.zeromonos.dto.BookingRequestDTO;
import tqs.zeromonos.dto.BookingResponseDTO;

public interface BookingService {
    // Public methods
    BookingResponseDTO createBooking(BookingRequestDTO request);
    BookingResponseDTO getBookingByToken(String bookingToken);
    void cancelBooking(String bookingToken);
    List<String> getAvailableMunicipalities();

    // Staff-only methods (management)
    List<BookingResponseDTO> listForStaff(String municipalityCode);
    BookingResponseDTO updateBookingStatusForStaff(String token, BookingStatus newStatus);
}
