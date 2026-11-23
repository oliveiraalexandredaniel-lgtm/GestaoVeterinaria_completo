package gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * Botões de ação dos formulários (Novo, Cadastrar/Salvar, Desativar, Reativar),
 * com habilitação automática conforme haja registro selecionado e o modo (ativos/inativos).
 */
public class BotoesCrud extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JButton novo = Tema.botaoSecundario("Novo / Limpar");
    private final JButton salvar = Tema.botaoPrimario("Cadastrar");
    private final JButton desativar = Tema.botaoPerigo("Desativar");
    private final JButton reativar = Tema.botaoSecundario("Reativar");

    /**
     * @param emGrade true para distribuir os botões em 2x2 (painéis estreitos); false para uma linha à direita
     */
    public BotoesCrud(boolean emGrade, Runnable aoNovo, Runnable aoSalvar, Runnable aoDesativar, Runnable aoReativar) {
        super(emGrade ? new GridLayout(2, 2, 8, 8) : new FlowLayout(FlowLayout.RIGHT, 8, 0));
        setOpaque(false);
        novo.addActionListener(e -> aoNovo.run());
        salvar.addActionListener(e -> aoSalvar.run());
        desativar.addActionListener(e -> aoDesativar.run());
        reativar.addActionListener(e -> aoReativar.run());
        add(novo);
        add(salvar);
        add(desativar);
        add(reativar);
        atualizar(false, false);
    }

    /**
     * @param existeSelecao true se há um registro carregado no formulário (modo edição)
     * @param modoInativos  true se a tela está listando registros inativos
     */
    public void atualizar(boolean existeSelecao, boolean modoInativos) {
        salvar.setText(existeSelecao ? "Salvar alterações" : "Cadastrar");
        salvar.setEnabled(!modoInativos);
        desativar.setEnabled(existeSelecao && !modoInativos);
        reativar.setEnabled(existeSelecao && modoInativos);
        revalidate();
        repaint();
    }
}
