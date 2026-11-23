package gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Barra de pesquisa reutilizada nas telas de clientes, raças e animais:
 * campo de texto, botões Pesquisar/Limpar e a opção "Mostrar inativos".
 */
public class BarraPesquisa extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField campo = Tema.campo();
    private final JCheckBox chkInativos = new JCheckBox("Mostrar inativos");

    /**
     * @param dica        texto de ajuda exibido no campo (visível quando o Look and Feel é o FlatLaf)
     * @param aoPesquisar ação executada ao pesquisar, limpar ou alternar "Mostrar inativos"
     */
    public BarraPesquisa(String dica, Runnable aoPesquisar) {
        super(new BorderLayout(10, 0));
        setOpaque(false);

        campo.putClientProperty("JTextField.placeholderText", dica);
        campo.setToolTipText(dica);
        campo.addActionListener(e -> aoPesquisar.run());

        JButton btnPesquisar = Tema.botaoPrimario("Pesquisar");
        btnPesquisar.addActionListener(e -> aoPesquisar.run());

        JButton btnLimpar = Tema.botaoSecundario("Limpar");
        btnLimpar.addActionListener(e -> {
            campo.setText("");
            aoPesquisar.run();
        });

        chkInativos.setOpaque(false);
        chkInativos.setFont(Tema.FONTE);
        chkInativos.setForeground(Tema.TEXTO);
        chkInativos.setFocusPainted(false);
        chkInativos.addActionListener(e -> aoPesquisar.run());

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        direita.setOpaque(false);
        direita.add(btnPesquisar);
        direita.add(btnLimpar);
        direita.add(Box.createHorizontalStrut(6));
        direita.add(chkInativos);

        add(campo, BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    public String getTexto() {
        return campo.getText().trim();
    }

    public boolean isMostrarInativos() {
        return chkInativos.isSelected();
    }
}
