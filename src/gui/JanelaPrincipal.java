package gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/** Janela principal: menu lateral à esquerda e a tela escolhida à direita (CardLayout). */
public class JanelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String DASHBOARD = "dashboard";
    public static final String CLIENTES = "clientes";
    public static final String ANIMAIS = "animais";
    public static final String RACAS = "racas";
    public static final String RELATORIOS = "relatorios";

    private final CardLayout cartas = new CardLayout();
    private final JPanel area = new JPanel(cartas);
    private final Map<String, Atualizavel> telas = new LinkedHashMap<>();
    private final Map<String, Tema.BotaoMenu> botoesMenu = new LinkedHashMap<>();

    private final PainelClientes painelClientes = new PainelClientes();
    private final PainelAnimais painelAnimais = new PainelAnimais();
    private final PainelRacas painelRacas = new PainelRacas();

    public JanelaPrincipal() {
        super("VetGestão - Sistema de Gestão Veterinária");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1240, 800);
        setLocationRelativeTo(null);

        PainelDashboard painelDashboard = new PainelDashboard(
                () -> {
                    navegarPara(CLIENTES);
                    painelClientes.novoCadastro();
                },
                () -> {
                    navegarPara(ANIMAIS);
                    painelAnimais.novoCadastro();
                },
                () -> {
                    navegarPara(RACAS);
                    painelRacas.novoCadastro();
                },
                () -> navegarPara(RELATORIOS));
        PainelRelatorios painelRelatorios = new PainelRelatorios();

        registrar(DASHBOARD, "Painel inicial", painelDashboard);
        registrar(CLIENTES, "Clientes", painelClientes);
        registrar(ANIMAIS, "Animais", painelAnimais);
        registrar(RACAS, "Raças", painelRacas);
        registrar(RELATORIOS, "Relatórios", painelRelatorios);

        setLayout(new BorderLayout());
        add(criarMenu(), BorderLayout.WEST);
        add(area, BorderLayout.CENTER);

        navegarPara(DASHBOARD);
    }

    /** Exibe a tela indicada e recarrega seus dados do banco. */
    public void navegarPara(String chave) {
        cartas.show(area, chave);
        telas.get(chave).atualizar();
        botoesMenu.forEach((k, b) -> b.setSelecionado(k.equals(chave)));
    }

    private void registrar(String chave, String titulo, javax.swing.JPanel painel) {
        area.add(painel, chave);
        telas.put(chave, (Atualizavel) painel);
        Tema.BotaoMenu botao = new Tema.BotaoMenu(titulo);
        botao.addActionListener(e -> navegarPara(chave));
        botoesMenu.put(chave, botao);
    }

    private JPanel criarMenu() {
        JPanel menu = new JPanel();
        menu.setBackground(Tema.MENU_FUNDO);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setPreferredSize(new Dimension(230, 0));

        JLabel marca = new JLabel("VetGestão");
        marca.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        marca.setForeground(Color.WHITE);
        marca.setAlignmentX(LEFT_ALIGNMENT);
        JLabel legenda = new JLabel("Gestão veterinária");
        legenda.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        legenda.setForeground(new Color(0x9FB8BE));
        legenda.setAlignmentX(LEFT_ALIGNMENT);

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(28, 26, 28, 10));
        cabecalho.setAlignmentX(LEFT_ALIGNMENT);
        cabecalho.add(marca);
        cabecalho.add(legenda);
        menu.add(cabecalho);

        for (Tema.BotaoMenu botao : botoesMenu.values()) {
            menu.add(botao);
        }
        menu.add(Box.createVerticalGlue());

        Tema.BotaoMenu sair = new Tema.BotaoMenu("Sair");
        sair.addActionListener(e -> {
            if (Mensagens.confirmar(this, "Sair", "Deseja realmente fechar o sistema?")) {
                dispose();
                System.exit(0);
            }
        });
        menu.add(sair);
        menu.add(Box.createVerticalStrut(18));
        return menu;
    }
}
