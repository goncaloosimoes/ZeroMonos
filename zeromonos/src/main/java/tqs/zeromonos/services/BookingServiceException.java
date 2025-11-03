package tqs.zeromonos.services;

/**
 * Exceção específica para erros relacionados com operações do serviço de
 * reservas.
 * Usada para representar erros inesperados durante operações de busca, criação,
 * atualização ou cancelamento de reservas.
 */
public class BookingServiceException extends RuntimeException {

    public BookingServiceException(String message) {
        super(message);
    }

    public BookingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public BookingServiceException(Throwable cause) {
        super(cause);
    }
}
