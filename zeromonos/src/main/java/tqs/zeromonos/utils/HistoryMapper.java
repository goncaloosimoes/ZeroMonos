package tqs.zeromonos.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tqs.zeromonos.data.StateChange;

/**
 * Utilidade para mapear histórico de mudanças de estado
 */
public class HistoryMapper {

    /**
     * Construtor privado para prevenir instanciação desta classe utilitária.
     */
    private HistoryMapper() {
        throw new AssertionError("Esta classe não deve ser instanciada");
    }

    /**
     * Converte lista de StateChange para lista de strings legíveis
     * 
     * @param history lista de mudanças de estado
     * @return lista de strings no formato "timestamp - status"
     */
    public static List<String> mapHistoryToStrings(List<StateChange> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return history.stream()
                    .filter(Objects::nonNull)
                    .map(HistoryMapper::formatStateChange)
                    .toList();
        } catch (Exception e) {
            // Em caso de erro, retorna lista vazia
            return new ArrayList<>();
        }
    }

    /**
     * Formata uma mudança de estado para string
     * 
     * @param stateChange mudança de estado
     * @return string formatada
     */
    private static String formatStateChange(StateChange stateChange) {
        try {
            String timestamp = stateChange.getTimestamp() != null
                    ? stateChange.getTimestamp().toString()
                    : "null timestamp";
            String status = stateChange.getStatus() != null
                    ? stateChange.getStatus().name()
                    : "null status";
            return timestamp + " - " + status;
        } catch (Exception e) {
            return "Erro ao processar histórico: " + e.getMessage();
        }
    }
}
