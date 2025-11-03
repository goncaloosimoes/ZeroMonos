package tqs.zeromonos.dto;

/**
 * Exceção específica para erros relacionados com a conversão entre entidades e
 * DTOs.
 * Usada quando há problemas ao converter objetos de domínio em objetos de
 * transferência de dados.
 */
public class DtoConversionException extends RuntimeException {

    public DtoConversionException(String message) {
        super(message);
    }

    public DtoConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DtoConversionException(Throwable cause) {
        super(cause);
    }
}
