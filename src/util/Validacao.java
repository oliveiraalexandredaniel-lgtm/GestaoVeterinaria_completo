package util;

/** Pequenos auxiliares de texto usados pelos DAOs na validação e nas consultas LIKE. */
public final class Validacao {

    private Validacao() {
    }

    /** true se o texto for nulo ou só tiver espaços. */
    public static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Remove espaços das pontas; devolve null se o resultado for vazio. */
    public static String limparOuNull(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    /** true se o texto (não nulo) tiver mais caracteres que o máximo permitido. */
    public static boolean excede(String texto, int maximo) {
        return texto != null && texto.length() > maximo;
    }

    /** Monta o padrão "%termo%" escapando os curingas do LIKE (%, _ e \). */
    public static String padraoLike(String termo) {
        String t = termo == null ? "" : termo.trim();
        t = t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + t + "%";
    }
}
