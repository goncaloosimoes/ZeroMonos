package tqs.zeromonos.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Utilidade para validação de datas conforme regras de negócio do ZeroMonos
 */
public class DateValidator {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Lisbon");

    /**
     * Construtor privado para prevenir instanciação desta classe utilitária.
     */
    private DateValidator() {
        throw new AssertionError("Esta classe não deve ser instanciada");
    }

    /**
     * Valida se uma data é válida para um agendamento de recolha
     * 
     * @param requestedDate data a validar
     * @throws IllegalArgumentException se a data violar alguma regra de negócio
     */
    public static void validateDate(LocalDate requestedDate) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);

        if (requestedDate.isBefore(today)) {
            throw new IllegalArgumentException("A data da recolha não pode ser no passado");
        }

        if (requestedDate.isEqual(today)) {
            throw new IllegalArgumentException("A data da recolha não pode ser no mesmo dia");
        }

        if (requestedDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Não são feitas recolhas ao fim de semana");
        }
    }

    /**
     * Verifica se uma data é válida para agendamento de recolha
     * 
     * @param requestedDate data a verificar
     * @return true se válida, false caso contrário
     */
    public static boolean isValidDate(LocalDate requestedDate) {
        try {
            validateDate(requestedDate);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
