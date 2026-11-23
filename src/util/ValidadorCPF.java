package util;

/** Validação matemática (dígitos verificadores) e formatação de CPF. */
public final class ValidadorCPF {

    private ValidadorCPF() {
    }

    /** Remove tudo que não for dígito. Retorna "" se o texto for nulo. */
    public static String somenteDigitos(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    /**
     * Valida o CPF. Aceita com ou sem máscara (123.456.789-09 ou 12345678909).
     * Rejeita textos com letras, tamanho diferente de 11 dígitos, sequências
     * repetidas (111.111.111-11) e dígitos verificadores incorretos.
     */
    public static boolean isValido(String cpf) {
        if (cpf == null || !cpf.matches("[0-9.\\-\\s]+")) {
            return false;
        }
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return d.charAt(9) - '0' == calcularDigito(d, 9)
                && d.charAt(10) - '0' == calcularDigito(d, 10);
    }

    /** Formata para 000.000.000-00. Se não tiver 11 dígitos, devolve o texto original. */
    public static String formatar(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11) {
            return cpf;
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Calcula um dígito verificador usando os primeiros "quantidade" dígitos (9 ou 10). */
    private static int calcularDigito(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
