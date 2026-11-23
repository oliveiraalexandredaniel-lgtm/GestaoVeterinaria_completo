package gui;

import dao.DAOException;
import dao.RegraNegocioException;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Caixas de diálogo padronizadas: sucesso, aviso, erro e confirmação. */
public final class Mensagens {

    private Mensagens() {
    }

    public static void sucesso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, formatar(mensagem), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void aviso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, formatar(mensagem), "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    public static void erro(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, formatar(mensagem), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /** Pergunta Sim/Não (o padrão é "Não"). Devolve true se o usuário escolheu "Sim". */
    public static boolean confirmar(Component pai, String titulo, String mensagem) {
        Object[] opcoes = {"Sim", "Não"};
        int r = JOptionPane.showOptionDialog(pai, formatar(mensagem), titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[1]);
        return r == 0;
    }

    /** Regra de negócio violada vira aviso; falha técnica vira erro. */
    public static void tratar(Component pai, DAOException e) {
        if (e instanceof RegraNegocioException) {
            aviso(pai, e.getMessage());
        } else {
            erro(pai, e.getMessage());
        }
    }

    /** Mensagens longas e sem quebra de linha são quebradas automaticamente (HTML com largura fixa). */
    private static Object formatar(String mensagem) {
        if (mensagem == null) {
            return "";
        }
        if (mensagem.length() <= 70 || mensagem.contains("\n")) {
            return mensagem;
        }
        String seguro = mensagem.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width: 340px'>" + seguro + "</body></html>";
    }
}
