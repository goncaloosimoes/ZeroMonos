package tqs.zeromonos.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import tqs.zeromonos.boundary.RestExceptionHandler;
import tqs.zeromonos.boundary.RestExceptionHandler.ApiError;
import tqs.zeromonos.boundary.SpringDocException;

import java.util.NoSuchElementException;

/**
 * Testes unitários do RestExceptionHandler para verificar que cada tipo de
 * exceção retorna o status HTTP correto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Isolamento - RestExceptionHandler")
class RestExceptionHandlerTest {

    private RestExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new RestExceptionHandler();
        lenient().when(webRequest.getDescription(false)).thenReturn("uri=/api/bookings/test");
    }

    @Test
    @DisplayName("handleBadRequest - Deve retornar 400 BAD_REQUEST para IllegalArgumentException")
    void testHandleBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid request data");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleBadRequest(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request data", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("handleNotFound - Deve retornar 404 NOT_FOUND para NoSuchElementException")
    void testHandleNotFound() {
        // Arrange
        NoSuchElementException ex = new NoSuchElementException("Resource not found");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleNotFound(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Resource not found", response.getBody().getMessage());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
    }

    @Test
    @DisplayName("handleMissingStaticResource - Deve retornar 404 NOT_FOUND para NoResourceFoundException")
    void testHandleMissingStaticResource() {
        // Arrange
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getResourcePath()).thenReturn("/missing.html");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleMissingStaticResource(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Requested resource not found", response.getBody().getMessage());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
    }

    @Test
    @DisplayName("handleMethodNotSupported - Deve retornar 405 METHOD_NOT_ALLOWED para HttpRequestMethodNotSupportedException")
    void testHandleMethodNotSupported() {
        // Arrange
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleMethodNotSupported(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("HTTP method not supported for this endpoint", response.getBody().getMessage());
        assertEquals(405, response.getBody().getStatus());
        assertEquals("Method Not Allowed", response.getBody().getError());
    }

    @Test
    @DisplayName("handleConflict - Deve retornar 409 CONFLICT para IllegalStateException")
    void testHandleConflict() {
        // Arrange
        IllegalStateException ex = new IllegalStateException("Booking cannot be cancelled in current state");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleConflict(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Booking cannot be cancelled in current state", response.getBody().getMessage());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
    }

    @Test
    @DisplayName("handleUnexpectedError - Deve retornar 500 INTERNAL_SERVER_ERROR para Exception genérica")
    void testHandleUnexpectedError() {
        // Arrange
        RuntimeException ex = new RuntimeException("Unexpected error occurred");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleUnexpectedError(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
    }

    @Test
    @DisplayName("ApiError - Deve construir objeto com todos os campos")
    void testApiErrorConstruction() {
        // Arrange & Act
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "Test message", "/api/test");

        // Assert
        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("Test message", error.getMessage());
        assertEquals("/api/test", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    @DisplayName("buildResponse - Deve extrair path corretamente do WebRequest")
    void testBuildResponse_PathExtraction() {
        // Arrange
        when(webRequest.getDescription(false)).thenReturn("uri=/api/bookings/123");

        IllegalArgumentException ex = new IllegalArgumentException("Test");

        // Act
        ResponseEntity<ApiError> response = exceptionHandler.handleBadRequest(ex, webRequest);

        // Assert
        assertEquals("/api/bookings/123", response.getBody().getPath());
    }

    @Test
    @DisplayName("Prioridade de handlers - Handlers específicos devem ter precedência sobre genérico")
    void testHandlerPriority() {
        // Arrange
        IllegalArgumentException specificEx = new IllegalArgumentException("Specific error");
        RuntimeException genericEx = new RuntimeException("Generic error");

        // Act
        ResponseEntity<ApiError> specificResponse = exceptionHandler.handleBadRequest(specificEx, webRequest);
        ResponseEntity<ApiError> genericResponse = exceptionHandler.handleUnexpectedError(genericEx, webRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, specificResponse.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, genericResponse.getStatusCode());
        assertEquals("Specific error", specificResponse.getBody().getMessage());
        assertEquals("An unexpected error occurred", genericResponse.getBody().getMessage());
    }

    // ==================== TESTES DE SpringDocException ====================

    @Test
    @DisplayName("SpringDocException - Construtor com mensagem deve criar exceção corretamente")
    void testSpringDocException_WithMessage() {
        // Arrange & Act
        String message = "Erro do SpringDoc";
        SpringDocException exception = new SpringDocException(message);

        // Assert
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("SpringDocException - Construtor com mensagem e causa deve criar exceção corretamente")
    void testSpringDocException_WithMessageAndCause() {
        // Arrange
        String message = "Erro do SpringDoc";
        Throwable cause = new RuntimeException("Causa raiz");

        // Act
        SpringDocException exception = new SpringDocException(message, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        assertEquals("Causa raiz", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("SpringDocException - Construtor apenas com causa deve criar exceção corretamente")
    void testSpringDocException_WithCauseOnly() {
        // Arrange
        Throwable cause = new RuntimeException("Causa raiz");

        // Act
        SpringDocException exception = new SpringDocException(cause);

        // Assert
        assertNotNull(exception);
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals(cause, exception.getCause());
        assertEquals("Causa raiz", exception.getCause().getMessage());
    }
}
