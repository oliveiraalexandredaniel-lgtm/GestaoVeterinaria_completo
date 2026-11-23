package gui;

import dao.AnimalDAO;
import dao.ClienteDAO;
import dao.DAOException;
import dao.RacaDAO;
import modelo.Animal;
import modelo.Cliente;
import modelo.Raca;
import util.CalculadoraIdade;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Tela de animais, com duas abas:
 *  - Cadastro: listar/pesquisar, cadastrar, alterar, desativar e reativar animais;
 *  - Consultar por cliente: busca por nome ou CPF do cliente, exibindo raça, idade, sexo e status.
 */
public class PainelAnimais extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private static final String[] OPCOES_SEXO = {"Não informado", "Macho", "Fêmea"};

    private final AnimalDAO animalDAO = new AnimalDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final RacaDAO racaDAO = new RacaDAO();

    // ---- aba de cadastro
    private final ModeloTabela<Animal> modelo = new ModeloTabela<>(
            new String[]{"ID", "Animal", "Raça", "Cliente", "Nascimento", "Idade", "Sexo", "Cor", "Status"},
            a -> new Object[]{a.getId(), a.getNome(), a.getNomeRaca() + " (" + a.getTipoAnimal() + ")", a.getNomeCliente(),
                    Formatos.data(a.getDataNascimento()), CalculadoraIdade.formatar(a.getDataNascimento()),
                    Formatos.sexo(a.getSexo()), Formatos.vazioSeNulo(a.getCor()), Formatos.status(a.isStatus())});
    private final JTable tabela = Tema.tabela(modelo);
    private final BarraPesquisa barra = new BarraPesquisa("Pesquisar por nome do animal, raça ou cliente", this::pesquisar);
    private final JLabel lblContagem = Tema.subtitulo(" ");

    private final JLabel lblFormulario = Tema.secao("Novo animal");
    private final JTextField txtId = Tema.campo();
    private final JTextField txtNome = Tema.campo();
    private final JTextField txtNascimento = Tema.campo();
    private final JComboBox<String> cmbSexo = new JComboBox<>(OPCOES_SEXO);
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>();
    private final JComboBox<Raca> cmbRaca = new JComboBox<>();
    private final JTextField txtCor = Tema.campo();
    private final JTextArea txtObservacoes = new JTextArea(3, 20);
    private final BotoesCrud botoes;

    private Animal selecionado;
    private boolean carregando;

    // ---- aba de consulta por cliente
    private final ModeloTabela<Animal> modeloConsulta = new ModeloTabela<>(
            new String[]{"Cliente", "CPF", "Animal", "Raça", "Idade", "Sexo", "Status"},
            a -> new Object[]{a.getNomeCliente(), a.getCpfCliente(), a.getNome(),
                    a.getNomeRaca() + " (" + a.getTipoAnimal() + ")", CalculadoraIdade.formatar(a.getDataNascimento()),
                    Formatos.sexo(a.getSexo()), Formatos.status(a.isStatus())});
    private final JTable tabelaConsulta = Tema.tabela(modeloConsulta);
    private final JTextField txtConsultaNome = Tema.campo();
    private final JTextField txtConsultaCpf = Tema.campo();
    private final JCheckBox chkConsultaInativos = new JCheckBox("Incluir animais inativos");
    private final JLabel lblConsulta = Tema.subtitulo("Informe o nome e/ou o CPF do cliente e clique em Consultar.");

    // ---- abas
    private static final String ABA_CADASTRO = "cadastro";
    private static final String ABA_CONSULTA = "consulta";
    private final CardLayout cartasAba = new CardLayout();
    private final JPanel areaAbas = new JPanel(cartasAba);
    private final Tema.BotaoAba abaCadastro = new Tema.BotaoAba("Cadastro e listagem");
    private final Tema.BotaoAba abaConsulta = new Tema.BotaoAba("Consultar por cliente");

    public PainelAnimais() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Tema.FUNDO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        botoes = new BotoesCrud(false, this::novo, this::salvar, this::desativar, this::reativar);

        JPanel cabecalho = Tema.fundo();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        JLabel titulo = Tema.titulo("Animais");
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = Tema.subtitulo("Cadastre animais vinculados a um cliente e a uma raça, ou consulte os animais de um cliente.");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        cabecalho.add(titulo);
        cabecalho.add(sub);
        cabecalho.add(Box.createVerticalStrut(10));
        add(cabecalho, BorderLayout.NORTH);

        // Abas (botões com faixa colorida) + CardLayout com o conteúdo de cada aba
        abaCadastro.addActionListener(e -> mostrarAba(ABA_CADASTRO));
        abaConsulta.addActionListener(e -> mostrarAba(ABA_CONSULTA));
        JPanel barraAbas = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barraAbas.setOpaque(false);
        barraAbas.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA));
        barraAbas.add(abaCadastro);
        barraAbas.add(abaConsulta);
        areaAbas.setOpaque(false);
        areaAbas.add(criarAbaCadastro(), ABA_CADASTRO);
        areaAbas.add(criarAbaConsulta(), ABA_CONSULTA);

        JPanel corpo = Tema.fundo();
        corpo.setLayout(new BorderLayout());
        corpo.add(barraAbas, BorderLayout.NORTH);
        corpo.add(areaAbas, BorderLayout.CENTER);
        add(corpo, BorderLayout.CENTER);
        mostrarAba(ABA_CADASTRO);
        Tema.larguras(tabela, 50, 110, 190, 150, 110, 160, 80, 90, 70);
        Tema.larguras(tabelaConsulta, 190, 130, 130, 190, 130, 80, 70);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !carregando) {
                int linha = tabela.getSelectedRow();
                if (linha >= 0) {
                    carregarNoFormulario(modelo.getItem(tabela.convertRowIndexToModel(linha)));
                }
            }
        });
        txtId.setEditable(false);
        txtId.setBackground(Tema.CAMPO_BLOQUEADO);
        txtId.setToolTipText("O ID é gerado pelo sistema e não pode ser alterado.");
        Tema.estilizarCampo(cmbSexo);
        Tema.estilizarCampo(cmbCliente);
        Tema.estilizarCampo(cmbRaca);
        Tema.estilizarCampo(txtObservacoes);
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        novo();
    }

    @Override
    public void atualizar() {
        carregarCombos();
        pesquisar();
    }

    /** Usado pelo atalho do painel inicial. */
    public void novoCadastro() {
        mostrarAba(ABA_CADASTRO);
        novo();
        txtNome.requestFocusInWindow();
    }

    private void mostrarAba(String chave) {
        cartasAba.show(areaAbas, chave);
        abaCadastro.setSelecionado(ABA_CADASTRO.equals(chave));
        abaConsulta.setSelecionado(ABA_CONSULTA.equals(chave));
    }

    // ------------------------------------------------------------------ montagem da tela

    private JPanel criarAbaCadastro() {
        JPanel aba = Tema.fundo();
        aba.setLayout(new BorderLayout(0, 12));
        aba.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel topo = Tema.fundo();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        barra.setAlignmentX(LEFT_ALIGNMENT);
        lblContagem.setAlignmentX(LEFT_ALIGNMENT);
        topo.add(barra);
        topo.add(Box.createVerticalStrut(8));
        topo.add(lblContagem);

        aba.add(topo, BorderLayout.NORTH);
        aba.add(Tema.rolagem(tabela), BorderLayout.CENTER);
        aba.add(criarFormulario(), BorderLayout.SOUTH);
        return aba;
    }

    private JPanel criarFormulario() {
        JPanel card = Tema.cartao();
        card.setLayout(new BorderLayout(0, 10));
        card.add(lblFormulario, BorderLayout.NORTH);

        JScrollPane rolagemObs = new JScrollPane(txtObservacoes);
        rolagemObs.setBorder(Tema.bordaCampo());
        rolagemObs.setPreferredSize(new Dimension(100, 52));

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        Tema.adicionarCampo(campos, "Nome do animal *", txtNome, 0, 0, 1);
        Tema.adicionarCampo(campos, "Nascimento (dd/mm/aaaa)", txtNascimento, 1, 0, 1);
        Tema.adicionarCampo(campos, "Sexo", cmbSexo, 2, 0, 1);
        Tema.adicionarCampo(campos, "ID (somente leitura)", txtId, 3, 0, 1);
        Tema.adicionarCampo(campos, "Cliente (dono) *", cmbCliente, 0, 1, 2);
        Tema.adicionarCampo(campos, "Raça *", cmbRaca, 2, 1, 2);
        Tema.adicionarCampo(campos, "Cor", txtCor, 0, 2, 1);
        Tema.adicionarCampo(campos, "Observações", rolagemObs, 1, 2, 3);
        card.add(campos, BorderLayout.CENTER);
        card.add(botoes, BorderLayout.SOUTH);
        return card;
    }

    private JPanel criarAbaConsulta() {
        JPanel aba = Tema.fundo();
        aba.setLayout(new BorderLayout(0, 12));
        aba.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel filtros = Tema.cartao();
        filtros.setLayout(new BorderLayout(0, 10));
        filtros.add(Tema.secao("Consultar animais por cliente"), BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        Tema.adicionarCampo(campos, "Nome do cliente", txtConsultaNome, 0, 0, 2);
        Tema.adicionarCampo(campos, "CPF do cliente", txtConsultaCpf, 2, 0, 1);
        txtConsultaNome.addActionListener(e -> consultarPorCliente());
        txtConsultaCpf.addActionListener(e -> consultarPorCliente());

        chkConsultaInativos.setOpaque(false);
        chkConsultaInativos.setFont(Tema.FONTE);
        chkConsultaInativos.setForeground(Tema.TEXTO);
        chkConsultaInativos.setFocusPainted(false);

        JButton btnConsultar = Tema.botaoPrimario("Consultar");
        btnConsultar.addActionListener(e -> consultarPorCliente());
        JButton btnLimpar = Tema.botaoSecundario("Limpar");
        btnLimpar.addActionListener(e -> {
            txtConsultaNome.setText("");
            txtConsultaCpf.setText("");
            chkConsultaInativos.setSelected(false);
            modeloConsulta.setItens(List.of());
            lblConsulta.setText("Informe o nome e/ou o CPF do cliente e clique em Consultar.");
        });
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        acoes.add(chkConsultaInativos);
        acoes.add(btnLimpar);
        acoes.add(btnConsultar);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(campos, BorderLayout.CENTER);
        centro.add(acoes, BorderLayout.SOUTH);
        filtros.add(centro, BorderLayout.CENTER);

        JPanel resultado = Tema.fundo();
        resultado.setLayout(new BorderLayout(0, 8));
        resultado.add(lblConsulta, BorderLayout.NORTH);
        resultado.add(Tema.rolagem(tabelaConsulta), BorderLayout.CENTER);

        aba.add(filtros, BorderLayout.NORTH);
        aba.add(resultado, BorderLayout.CENTER);
        return aba;
    }

    // ------------------------------------------------------------------ dados

    /** Recarrega os combos de cliente e raça (somente ativos), mantendo a seleção atual quando possível. */
    private void carregarCombos() {
        try {
            int idCliente = cmbCliente.getSelectedItem() == null ? 0 : ((Cliente) cmbCliente.getSelectedItem()).getId();
            int idRaca = cmbRaca.getSelectedItem() == null ? 0 : ((Raca) cmbRaca.getSelectedItem()).getId();
            cmbCliente.removeAllItems();
            for (Cliente c : clienteDAO.listar(true)) {
                cmbCliente.addItem(c);
            }
            cmbRaca.removeAllItems();
            for (Raca r : racaDAO.listar(true)) {
                cmbRaca.addItem(r);
            }
            selecionarCliente(idCliente, null);
            selecionarRaca(idRaca, null);
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    /** Seleciona o cliente do combo pelo ID; se não estiver na lista (inativo), insere-o para exibição. */
    private void selecionarCliente(int id, Animal origem) {
        for (int i = 0; i < cmbCliente.getItemCount(); i++) {
            if (cmbCliente.getItemAt(i).getId() == id) {
                cmbCliente.setSelectedIndex(i);
                return;
            }
        }
        if (origem != null) {
            Cliente c = new Cliente(origem.getNomeCliente(), origem.getCpfCliente());
            c.setId(origem.getIdCliente());
            cmbCliente.addItem(c);
            cmbCliente.setSelectedItem(c);
        } else if (cmbCliente.getItemCount() > 0) {
            cmbCliente.setSelectedIndex(0);
        }
    }

    private void selecionarRaca(int id, Animal origem) {
        for (int i = 0; i < cmbRaca.getItemCount(); i++) {
            if (cmbRaca.getItemAt(i).getId() == id) {
                cmbRaca.setSelectedIndex(i);
                return;
            }
        }
        if (origem != null) {
            Raca r = new Raca(origem.getNomeRaca(), origem.getTipoAnimal());
            r.setId(origem.getIdRaca());
            cmbRaca.addItem(r);
            cmbRaca.setSelectedItem(r);
        } else if (cmbRaca.getItemCount() > 0) {
            cmbRaca.setSelectedIndex(0);
        }
    }

    private void pesquisar() {
        try {
            boolean ativos = !barra.isMostrarInativos();
            List<Animal> lista = animalDAO.pesquisar(barra.getTexto(), ativos);
            carregando = true;
            try {
                modelo.setItens(lista);
            } finally {
                carregando = false;
            }
            novo();
            lblContagem.setText(lista.size() + (ativos ? " animal(is) ativo(s)" : " animal(is) inativo(s)"));
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void consultarPorCliente() {
        try {
            List<Animal> lista = animalDAO.consultarPorCliente(txtConsultaNome.getText(), txtConsultaCpf.getText(),
                    chkConsultaInativos.isSelected());
            modeloConsulta.setItens(lista);
            lblConsulta.setText(lista.isEmpty()
                    ? "Nenhum animal encontrado para o filtro informado."
                    : lista.size() + " animal(is) encontrado(s).");
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    // ------------------------------------------------------------------ formulário

    private void novo() {
        selecionado = null;
        tabela.clearSelection();
        txtId.setText("");
        txtNome.setText("");
        txtNascimento.setText("");
        cmbSexo.setSelectedIndex(0);
        txtCor.setText("");
        txtObservacoes.setText("");
        if (cmbCliente.getItemCount() > 0) {
            cmbCliente.setSelectedIndex(0);
        }
        if (cmbRaca.getItemCount() > 0) {
            cmbRaca.setSelectedIndex(0);
        }
        lblFormulario.setText(barra.isMostrarInativos() ? "Animais inativos (selecione um para reativar)" : "Novo animal");
        definirFormularioEditavel(!barra.isMostrarInativos());
        botoes.atualizar(false, barra.isMostrarInativos());
    }

    private void carregarNoFormulario(Animal a) {
        selecionado = a;
        txtId.setText(String.valueOf(a.getId()));
        txtNome.setText(a.getNome());
        txtNascimento.setText(Formatos.data(a.getDataNascimento()));
        cmbSexo.setSelectedIndex(a.getSexo() == null ? 0 : (a.getSexo() == 'M' ? 1 : 2));
        txtCor.setText(Formatos.vazioSeNulo(a.getCor()));
        txtObservacoes.setText(Formatos.vazioSeNulo(a.getObservacoes()));
        txtObservacoes.setCaretPosition(0);
        selecionarCliente(a.getIdCliente(), a);
        selecionarRaca(a.getIdRaca(), a);
        lblFormulario.setText((a.isStatus() ? "Editar animal #" : "Animal inativo #") + a.getId());
        definirFormularioEditavel(a.isStatus());
        botoes.atualizar(true, !a.isStatus());
    }

    private void definirFormularioEditavel(boolean editavel) {
        for (JTextField f : new JTextField[]{txtNome, txtNascimento, txtCor}) {
            f.setEditable(editavel);
            f.setBackground(editavel ? Color.WHITE : Tema.CAMPO_BLOQUEADO);
        }
        txtObservacoes.setEditable(editavel);
        txtObservacoes.setBackground(editavel ? Color.WHITE : Tema.CAMPO_BLOQUEADO);
        cmbSexo.setEnabled(editavel);
        cmbCliente.setEnabled(editavel);
        cmbRaca.setEnabled(editavel);
    }

    // ------------------------------------------------------------------ ações

    private void salvar() {
        Animal a = new Animal();
        try {
            LocalDate nascimento = Formatos.lerData(txtNascimento.getText());
            a.setDataNascimento(nascimento);
        } catch (IllegalArgumentException e) {
            Mensagens.aviso(this, e.getMessage());
            txtNascimento.requestFocusInWindow();
            return;
        }
        a.setNome(txtNome.getText());
        int indiceSexo = cmbSexo.getSelectedIndex();
        a.setSexo(indiceSexo == 1 ? Character.valueOf('M') : indiceSexo == 2 ? Character.valueOf('F') : null);
        a.setCor(txtCor.getText());
        a.setObservacoes(txtObservacoes.getText());
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        Raca raca = (Raca) cmbRaca.getSelectedItem();
        a.setIdCliente(cliente == null ? 0 : cliente.getId());
        a.setIdRaca(raca == null ? 0 : raca.getId());

        try {
            if (selecionado == null) {
                animalDAO.inserir(a);
                Mensagens.sucesso(this, "Animal cadastrado com sucesso!\n\nNome: " + a.getNome() + "\nID: " + a.getId());
            } else {
                a.setId(selecionado.getId());
                animalDAO.alterar(a);
                Mensagens.sucesso(this, "Animal alterado com sucesso!\n\nNome: " + a.getNome() + "\nID: " + a.getId());
            }
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void desativar() {
        if (selecionado == null) {
            return;
        }
        boolean sim = Mensagens.confirmar(this, "Desativar animal",
                "Deseja realmente desativar o animal \"" + selecionado.getNome() + "\"?\n\n"
                        + "O cadastro deixará de aparecer nas consultas, mas continuará no banco de dados.");
        if (!sim) {
            return;
        }
        try {
            animalDAO.desativar(selecionado.getId());
            Mensagens.sucesso(this, "Animal desativado com sucesso!");
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void reativar() {
        if (selecionado == null) {
            return;
        }
        boolean sim = Mensagens.confirmar(this, "Reativar animal",
                "Deseja reativar o animal \"" + selecionado.getNome() + "\"?");
        if (!sim) {
            return;
        }
        try {
            animalDAO.reativar(selecionado.getId());
            Mensagens.sucesso(this, "Animal reativado com sucesso!");
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }
}
