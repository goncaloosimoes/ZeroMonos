package tqs.zeromonos.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

@ControllerAdvice
@ResponseBody
public class RestExceptionHandler {

    // Excluir endpoints do SpringDoc do tratamento de exceções
    private boolean isSpringDocPath(WebRequest req) {
        String description = req.getDescription(false);
        // Extrair apenas o caminho da URI (remover "uri=" prefix se existir)
        String path = description.replace("uri=", "");
        return path.contains("/v3/api-docs") || path.contains("/swagger-ui");
    }

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    // Estrutura genérica de um erro na api
    public static class ApiError {
        private final OffsetDateTime timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;

        public ApiError(HttpStatus status, String message, String path) {
            this.timestamp = OffsetDateTime.now();
            this.status = status.value();
            this.error = status.getReasonPhrase();
            this.message = message;
            this.path = path;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }
    }

    // ---------- Specific Exception Handlers ----------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, WebRequest req) {
        log.warn("Invalid request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, WebRequest req) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleMissingStaticResource(NoResourceFoundException ex, WebRequest req) {
        log.debug("Static resource missing: {}", ex.getResourcePath());
        return buildResponse(HttpStatus.NOT_FOUND, "Requested resource not found", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            WebRequest req) {
        String requestDescription = req.getDescription(false);
        log.warn("Method not supported: {} for {}", ex.getMethod(), requestDescription);
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint", req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex, WebRequest req) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(Exception ex, WebRequest req) {
        // Ignora erros do SpringDoc OpenAPI para não interferir com a geração de
        // documentação
        if (isSpringDocPath(req)) {
            log.debug("Erro do SpringDoc - re-lançando: {}", ex.getMessage());
            // Re-lança a exceção para que o SpringDoc a trate
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            // Envolve exceções checked numa exceção customizada específica
            throw new SpringDocException("Erro do SpringDoc ao processar requisição", ex);
        }
        log.error("Exceção não tratada: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req);
    }

    // ---------- Helper ----------

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, WebRequest req) {
        String path = req.getDescription(false).replace("uri=", "");
        return ResponseEntity.status(status)
                .body(new ApiError(status, message, path));
    }
}
