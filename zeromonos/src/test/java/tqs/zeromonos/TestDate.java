package tqs.zeromonos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

@DisplayName("Testes de Validação de Datas")
public class TestDate {

    private LocalDate today;
    private ZoneId zone;

    @BeforeEach
    void setUp() {
        zone = ZoneId.of("Europe/Lisbon");
        today = LocalDate.now(zone);
    }

    @Test
    @DisplayName("Data não pode ser no passado")
    void testDateCannotBeInPast() {
        LocalDate yesterday = today.minusDays(1);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validateDate(yesterday),
                "Deve lançar exceção para data no passado");
        assertEquals("A data solicitada não pode ser no passado", exception.getMessage());
    }

    @Test
    @DisplayName("Data não pode ser hoje")
    void testDateCannotBeToday() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validateDate(today),
                "Deve lançar exceção para data hoje");
        assertEquals("A data solicitada não pode ser no mesmo dia", exception.getMessage());
    }

    @Test
    @DisplayName("Data não pode ser domingo")
    void testDateCannotBeSunday() {
        // Encontrar o próximo domingo
        LocalDate startDate = today;
        int daysUntilSunday = DayOfWeek.SUNDAY.getValue() - startDate.getDayOfWeek().getValue();
        if (daysUntilSunday <= 0) {
            daysUntilSunday += 7;
        }
        LocalDate sundayDate = startDate.plusDays(daysUntilSunday);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validateDate(sundayDate),
                "Deve lançar exceção para data no domingo");
        assertEquals("Não são feitas recolhas ao fim de semana", exception.getMessage());
    }

    @Test
    @DisplayName("Data válida pode ser amanhã se não for domingo")
    void testDateCanBeTomorrowIfNotSunday() {
        LocalDate tomorrow = today.plusDays(1);
        LocalDate testDate = tomorrow;

        if (testDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            // Se amanhã for domingo, testar segunda-feira
            testDate = testDate.plusDays(1);
        }

        LocalDate finalTestDate = testDate;
        assertDoesNotThrow(
                () -> validateDate(finalTestDate),
                "Não deve lançar exceção para data válida");
    }

    @Test
    @DisplayName("Data válida pode ser qualquer dia da semana exceto domingo")
    void testDateCanBeAnyWeekdayExceptSunday() {
        // Testar os próximos 30 dias
        LocalDate startDate = today.plusDays(2); // Começar 2 dias à frente para evitar conflitos

        for (int i = 0; i < 30; i++) {
            LocalDate currentDate = startDate.plusDays(i);

            if (currentDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                LocalDate finalDate = currentDate;
                assertDoesNotThrow(
                        () -> validateDate(finalDate),
                        "Data " + currentDate + " deveria ser válida");
            } else {
                LocalDate finalDate = currentDate;
                IllegalArgumentException exception = assertThrows(
                        IllegalArgumentException.class,
                        () -> validateDate(finalDate),
                        "Domingo " + currentDate + " deveria ser inválido");
                assertEquals("Não são feitas recolhas ao fim de semana", exception.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Data pode ser no futuro distante")
    void testDateCanBeInFarFuture() {
        LocalDate farFuture = today.plusDays(365);

        if (farFuture.getDayOfWeek() != DayOfWeek.SUNDAY) {
            assertDoesNotThrow(
                    () -> validateDate(farFuture),
                    "Não deve lançar exceção para data no futuro distante");
        }
    }

    /**
     * Método de validação de data que replica a lógica do
     * BookingServiceImplementation
     */
    private void validateDate(LocalDate requestedDate) {
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
}