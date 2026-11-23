package gui;

import dao.AnimalDAO;
import dao.ClienteDAO;
import dao.DAOException;
import modelo.Animal;
import modelo.Cliente;
import util.GeradorRelatorioTXT;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tela de relatórios: gera os arquivos .txt e informa onde foram salvos. */
public class PainelRelatorios extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AnimalDAO animalDAO = new AnimalDAO();

    private final JTextField txtPasta = Tema.campo();
    private final JComboBox<String> cmbMesAnimais = new JComboBox<>(opcoesDeMes());
    private final JComboBox<String> cmbMesClientes = new JComboBox<>(opcoesDeMes());
    private final JTextArea txtLog = new JTextArea(6, 40);

    public PainelRelatorios() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Tema.FUNDO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        txtPasta.setText(Paths.get("relatorios").toAbsolutePath().toString());
        int mesAtual = LocalDate.now().getMonthValue() - 1;
        cmbMesAnimais.setSelectedIndex(mesAtual);
        cmbMesClientes.setSelectedIndex(mesAtual);
        Tema.estilizarCampo(cmbMesAnimais);
        Tema.estilizarCampo(cmbMesClientes);

        JPanel topo = Tema.fundo();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel titulo = Tema.titulo("Relatórios");
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = Tema.subtitulo("Gere arquivos .txt (UTF-8) com os dados do banco. Os relatórios incluem apenas registros ativos.");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        topo.add(titulo);
        topo.add(sub);
        add(topo, BorderLayout.NORTH);

        JPanel centro = Tema.fundo();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(criarCartaoPasta());
        centro.add(Box.createVerticalStrut(14));
        centro.add(criarCartaoRelatorio("1. Todos os clientes e seus animais",
                "Lista cada cliente com CPF e seus animais (nome, raça e data de nascimento).",
                null, this::gerarGeral));
        centro.add(Box.createVerticalStrut(14));
        centro.add(criarCartaoRelatorio("2. Animais aniversariantes do mês",
                "Animais que fazem aniversário no mês escolhido, com cliente e telefone.",
                cmbMesAnimais, this::gerarAnimais));
        centro.add(Box.createVerticalStrut(14));
        centro.add(criarCartaoRelatorio("3. Clientes aniversariantes do mês",
                "Clientes que fazem aniversário no mês escolhido, com CPF e telefone.",
                cmbMesClientes, this::gerarClientes));
        add(centro, BorderLayout.CENTER);

        add(criarLog(), BorderLayout.SOUTH);
    }

    @Override
    public void atualizar() {
        // Os relatórios consultam o banco somente no momento da geração.
    }

    private JPanel criarCartaoPasta() {
        JPanel card = Tema.cartao();
        card.setLayout(new BorderLayout(10, 6));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 110));
        card.add(Tema.secao("Pasta de destino"), BorderLayout.NORTH);
        card.add(txtPasta, BorderLayout.CENTER);

        JButton btnEscolher = Tema.botaoSecundario("Escolher pasta...");
        btnEscolher.addActionListener(e -> escolherPasta());
        JButton btnAbrir = Tema.botaoSecundario("Abrir pasta");
        btnAbrir.addActionListener(e -> abrirPasta());
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(btnEscolher);
        acoes.add(btnAbrir);
        card.add(acoes, BorderLayout.EAST);
        return card;
    }

    private JPanel criarCartaoRelatorio(String titulo, String descricao, JComboBox<String> comboMes, Runnable acaoGerar) {
        JPanel card = Tema.cartao();
        card.setLayout(new GridBagLayout());
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 100));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        card.add(Tema.secao(titulo), g);
        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 0);
        card.add(Tema.subtitulo(descricao), g);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acoes.setOpaque(false);
        if (comboMes != null) {
            acoes.add(Tema.rotulo("Mês:"));
            acoes.add(comboMes);
        }
        JButton btn = Tema.botaoPrimario("Gerar relatório");
        btn.addActionListener(e -> acaoGerar.run());
        acoes.add(btn);

        g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = 0;
        g.gridheight = 2;
        g.anchor = GridBagConstraints.EAST;
        card.add(acoes, g);
        return card;
    }

    private JPanel criarLog() {
        JPanel painel = Tema.fundo();
        painel.setLayout(new BorderLayout(0, 6));
        painel.add(Tema.secao("Arquivos gerados"), BorderLayout.NORTH);
        txtLog.setEditable(false);
        txtLog.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        txtLog.setLineWrap(true);
        txtLog.setText("Nenhum relatório gerado nesta sessão.\n");
        JScrollPane rolagem = new JScrollPane(txtLog);
        rolagem.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1));
        rolagem.setPreferredSize(new java.awt.Dimension(100, 120));
        painel.add(rolagem, BorderLayout.CENTER);
        return painel;
    }

    // ------------------------------------------------------------------ ações

    private void escolherPasta() {
        JFileChooser chooser = new JFileChooser(txtPasta.getText());
        chooser.setDialogTitle("Escolha a pasta onde os relatórios serão salvos");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtPasta.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void abrirPasta() {
        Path pasta = Paths.get(txtPasta.getText().trim());
        try {
            Files.createDirectories(pasta);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(new File(pasta.toString()));
            } else {
                Mensagens.aviso(this, "Não é possível abrir a pasta automaticamente neste computador.\nCaminho: " + pasta.toAbsolutePath());
            }
        } catch (IOException | RuntimeException e) {
            Mensagens.erro(this, "Não foi possível abrir a pasta: " + e.getMessage());
        }
    }

    private void gerarGeral() {
        executar("Todos os clientes e seus animais", pasta -> {
            Map<Cliente, List<Animal>> mapa = new LinkedHashMap<>();
            for (Cliente c : clienteDAO.listar(true)) {
                mapa.put(c, animalDAO.listarPorIdCliente(c.getId()));
            }
            return GeradorRelatorioTXT.gerarRelatorioGeral(mapa, pasta);
        });
    }

    private void gerarAnimais() {
        int mes = cmbMesAnimais.getSelectedIndex() + 1;
        executar("Animais aniversariantes de " + GeradorRelatorioTXT.nomeMes(mes), pasta ->
                GeradorRelatorioTXT.gerarAniversariantesAnimais(mes, animalDAO.listarAniversariantesDoMes(mes), pasta));
    }

    private void gerarClientes() {
        int mes = cmbMesClientes.getSelectedIndex() + 1;
        executar("Clientes aniversariantes de " + GeradorRelatorioTXT.nomeMes(mes), pasta ->
                GeradorRelatorioTXT.gerarAniversariantesClientes(mes, clienteDAO.listarAniversariantesDoMes(mes), pasta));
    }

    @FunctionalInterface
    private interface Geracao {
        Path gerar(Path pasta) throws DAOException, IOException;
    }

    private void executar(String descricao, Geracao geracao) {
        String texto = txtPasta.getText().trim();
        if (texto.isEmpty()) {
            Mensagens.aviso(this, "Informe a pasta de destino dos relatórios.");
            txtPasta.requestFocusInWindow();
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            Path arquivo = geracao.gerar(Paths.get(texto));
            String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (txtLog.getText().startsWith("Nenhum relatório")) {
                txtLog.setText("");
            }
            txtLog.append("[" + hora + "] " + descricao + "\n    " + arquivo + "\n");
            Mensagens.sucesso(this, "Relatório gerado com sucesso!\n\n" + descricao + "\n\nArquivo salvo em:\n" + arquivo);
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        } catch (IOException e) {
            Mensagens.erro(this, "Não foi possível gravar o arquivo na pasta informada: " + e.getMessage());
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private static String[] opcoesDeMes() {
        String[] opcoes = new String[12];
        for (int i = 0; i < 12; i++) {
            opcoes[i] = String.format("%02d - %s", i + 1, GeradorRelatorioTXT.MESES[i]);
        }
        return opcoes;
    }
}
