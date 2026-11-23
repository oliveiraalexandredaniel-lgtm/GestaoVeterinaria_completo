package gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Conversões entre texto digitado/exibido na tela e os tipos do modelo. */
public final class Formatos {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {
    }

    /** Converte "dd/mm/aaaa" em LocalDate. Texto vazio vira null. Lança IllegalArgumentException se inválido. */
    public static LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: '" + texto.trim() + "'. Use o formato dd/mm/aaaa (ex.: 15/03/2020).");
        }
    }

    public static String data(LocalDate d) {
        return d == null ? "" : d.format(DATA);
    }

    public static String dataHora(LocalDateTime d) {
        return d == null ? "" : d.format(DATA_HORA);
    }

    public static String status(boolean ativo) {
        return ativo ? "Ativo" : "Inativo";
    }

    public static String sexo(Character sexo) {
        if (sexo == null) {
            return "-";
        }
        return sexo == 'M' ? "Macho" : "Fêmea";
    }

    public static String vazioSeNulo(String texto) {
        return texto == null ? "" : texto;
    }
}
