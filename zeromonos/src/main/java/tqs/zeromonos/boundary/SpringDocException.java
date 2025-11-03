package tqs.zeromonos.boundary;

/**
 * Exceção específica para erros relacionados com o SpringDoc OpenAPI.
 * Usada para re-lançar exceções do SpringDoc sem interferir com a geração de
 * documentação.
 */
public class SpringDocException extends RuntimeException {

    public SpringDocException(String message) {
        super(message);
    }

    public SpringDocException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpringDocException(Throwable cause) {
        super(cause);
    }
}
