package util;

import java.time.LocalDate;
import java.time.Period;

/** Cálculo e formatação de idade usando LocalDate e Period. */
public final class CalculadoraIdade {

    public static final String NAO_INFORMADA = "Não informada";

    private CalculadoraIdade() {
    }

    /**
     * Calcula a idade em anos e meses.
     *
     * @return o Period, ou null se a data de nascimento for nula ou posterior à data de referência
     */
    public static Period calcular(LocalDate nascimento, LocalDate referencia) {
        if (nascimento == null || referencia == null || nascimento.isAfter(referencia)) {
            return null;
        }
        return Period.between(nascimento, referencia);
    }

    /** Idade em anos e meses (ex.: "3 anos e 2 meses") em relação a hoje. */
    public static String formatar(LocalDate nascimento) {
        return formatar(nascimento, LocalDate.now());
    }

    /** Idade em anos e meses em relação a uma data de referência (facilita testes). */
    public static String formatar(LocalDate nascimento, LocalDate referencia) {
        if (nascimento == null) {
            return NAO_INFORMADA;
        }
        Period p = calcular(nascimento, referencia);
        if (p == null) {
            return "Data inválida";
        }
        int anos = p.getYears();
        int meses = p.getMonths();
        if (anos == 0 && meses == 0) {
            return "Menos de 1 mês";
        }
        String textoAnos = anos + (anos == 1 ? " ano" : " anos");
        String textoMeses = meses + (meses == 1 ? " mês" : " meses");
        if (anos == 0) {
            return textoMeses;
        }
        if (meses == 0) {
            return textoAnos;
        }
        return textoAnos + " e " + textoMeses;
    }
}
