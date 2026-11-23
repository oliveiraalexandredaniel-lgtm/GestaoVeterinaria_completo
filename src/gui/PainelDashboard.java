package gui;

import dao.AnimalDAO;
import dao.ClienteDAO;
import dao.DAOException;
import dao.RacaDAO;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Painel inicial: indicadores (consultados no banco) e atalhos para as principais telas. */
public class PainelDashboard extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AnimalDAO animalDAO = new AnimalDAO();
    private final RacaDAO racaDAO = new RacaDAO();

    private final JLabel lblClientes = numero();
    private final JLabel lblAnimais = numero();
    private final JLabel lblRacas = numero();
    private final JLabel lblAviso = Tema.subtitulo(" ");

    public PainelDashboard(Runnable novoCliente, Runnable novoAnimal, Runnable novaRaca, Runnable relatorios) {
        setLayout(new BorderLayout(0, 20));
        setBackground(Tema.FUNDO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel topo = Tema.fundo();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel titulo = Tema.titulo("Painel inicial");
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")));
        JLabel sub = Tema.subtitulo("Resumo da clínica em " + hoje + ".");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        lblAviso.setAlignmentX(LEFT_ALIGNMENT);
        lblAviso.setForeground(Tema.PERIGO);
        topo.add(titulo);
        topo.add(sub);
        topo.add(lblAviso);
        add(topo, BorderLayout.NORTH);

        JPanel centro = Tema.fundo();
        centro.setLayout(new BorderLayout(0, 20));

        JPanel indicadores = Tema.fundo();
        indicadores.setLayout(new GridLayout(1, 3, 18, 0));
        indicadores.add(criarIndicador("Clientes ativos", lblClientes, Tema.PRIMARIA));
        indicadores.add(criarIndicador("Animais ativos", lblAnimais, new Color(0xE08E0B)));
        indicadores.add(criarIndicador("Raças ativas", lblRacas, new Color(0x3B6FB6)));
        centro.add(indicadores, BorderLayout.NORTH);

        JPanel atalhos = Tema.cartao();
        atalhos.setLayout(new BorderLayout(0, 14));
        atalhos.add(Tema.secao("Atalhos"), BorderLayout.NORTH);
        JPanel botoes = new JPanel(new GridLayout(1, 4, 14, 0));
        botoes.setOpaque(false);
        botoes.add(atalho("Cadastrar cliente", novoCliente));
        botoes.add(atalho("Cadastrar animal", novoAnimal));
        botoes.add(atalho("Cadastrar raça", novaRaca));
        botoes.add(atalho("Gerar relatórios", relatorios));
        atalhos.add(botoes, BorderLayout.CENTER);
        JPanel envoltorio = Tema.fundo();
        envoltorio.setLayout(new BorderLayout());
        envoltorio.add(atalhos, BorderLayout.NORTH);
        centro.add(envoltorio, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    @Override
    public void atualizar() {
        try {
            lblClientes.setText(String.valueOf(clienteDAO.contarAtivos()));
            lblAnimais.setText(String.valueOf(animalDAO.contarAtivos()));
            lblRacas.setText(String.valueOf(racaDAO.contarAtivas()));
            lblAviso.setText(" ");
        } catch (DAOException e) {
            lblClientes.setText("-");
            lblAnimais.setText("-");
            lblRacas.setText("-");
            lblAviso.setText("Não foi possível consultar o banco: " + e.getMessage());
        }
    }

    private static JLabel numero() {
        JLabel l = new JLabel("-");
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        l.setForeground(Tema.TEXTO);
        return l;
    }

    private JPanel criarIndicador(String descricao, JLabel valor, Color cor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Tema.CARTAO);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDA, 1),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 6, 0, 0, cor),
                        BorderFactory.createEmptyBorder(18, 20, 18, 20))));
        JLabel texto = Tema.subtitulo(descricao);
        texto.setFont(Tema.FONTE_NEGRITO);
        card.add(valor, BorderLayout.CENTER);
        card.add(texto, BorderLayout.SOUTH);
        return card;
    }

    private JButton atalho(String texto, Runnable acao) {
        JButton b = Tema.botaoPrimario(texto);
        b.addActionListener(e -> acao.run());
        return b;
    }
}
