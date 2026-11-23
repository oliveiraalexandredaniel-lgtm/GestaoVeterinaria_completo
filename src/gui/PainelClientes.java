package gui;

import dao.ClienteDAO;
import dao.DAOException;
import modelo.Cliente;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.util.List;

/** Tela de clientes: listar/pesquisar por nome ou CPF, cadastrar, alterar, desativar e reativar. */
public class PainelClientes extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private static final String[] UFS = {"", "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"};

    private final ClienteDAO dao = new ClienteDAO();

    private final ModeloTabela<Cliente> modelo = new ModeloTabela<>(
            new String[]{"ID", "Nome", "CPF", "Nascimento", "Telefone", "Cidade/UF", "Última alteração"},
            c -> new Object[]{c.getId(), c.getNome(), c.getCpf(), Formatos.data(c.getDataNascimento()),
                    Formatos.vazioSeNulo(c.getTelefone()), cidadeUf(c), Formatos.dataHora(c.getDataUltimaAlteracao())});
    private final JTable tabela = Tema.tabela(modelo);
    private final BarraPesquisa barra = new BarraPesquisa("Pesquisar por nome ou CPF", this::pesquisar);
    private final JLabel lblContagem = Tema.subtitulo(" ");

    private final JLabel lblFormulario = Tema.secao("Novo cliente");
    private final JTextField txtId = Tema.campo();
    private final JTextField txtNome = Tema.campo();
    private final JTextField txtCpf = Tema.campo();
    private final JTextField txtNascimento = Tema.campo();
    private final JTextField txtTelefone = Tema.campo();
    private final JTextField txtEndereco = Tema.campo();
    private final JTextField txtBairro = Tema.campo();
    private final JTextField txtCidade = Tema.campo();
    private final JComboBox<String> cmbEstado = new JComboBox<>(UFS);
    private final JTextField txtCep = Tema.campo();
    private final BotoesCrud botoes;

    private Cliente selecionado;
    private boolean carregando;

    public PainelClientes() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Tema.FUNDO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        botoes = new BotoesCrud(false, this::novo, this::salvar, this::desativar, this::reativar);
        add(criarTopo(), BorderLayout.NORTH);
        add(Tema.rolagem(tabela), BorderLayout.CENTER);
        add(criarFormulario(), BorderLayout.SOUTH);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !carregando) {
                int linha = tabela.getSelectedRow();
                if (linha >= 0) {
                    carregarNoFormulario(modelo.getItem(tabela.convertRowIndexToModel(linha)));
                }
            }
        });
        Tema.larguras(tabela, 50, 220, 130, 100, 140, 190, 140);
        txtId.setEditable(false);
        txtId.setBackground(Tema.CAMPO_BLOQUEADO);
        txtId.setToolTipText("O ID é gerado pelo sistema e não pode ser alterado.");
        Tema.estilizarCampo(cmbEstado);
        novo();
    }

    @Override
    public void atualizar() {
        pesquisar();
    }

    /** Usado pelo atalho do painel inicial. */
    public void novoCadastro() {
        novo();
        txtNome.requestFocusInWindow();
    }

    // ------------------------------------------------------------------ montagem da tela

    private JPanel criarTopo() {
        JPanel topo = Tema.fundo();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel titulo = Tema.titulo("Clientes");
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = Tema.subtitulo("Cadastre, consulte, altere e desative clientes.");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        barra.setAlignmentX(LEFT_ALIGNMENT);
        lblContagem.setAlignmentX(LEFT_ALIGNMENT);
        topo.add(titulo);
        topo.add(sub);
        topo.add(javax.swing.Box.createVerticalStrut(14));
        topo.add(barra);
        topo.add(javax.swing.Box.createVerticalStrut(8));
        topo.add(lblContagem);
        return topo;
    }

    private JPanel criarFormulario() {
        JPanel card = Tema.cartao();
        card.setLayout(new BorderLayout(0, 12));
        card.add(lblFormulario, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        Tema.adicionarCampo(campos, "Nome *", txtNome, 0, 0, 2);
        Tema.adicionarCampo(campos, "CPF *", txtCpf, 2, 0, 1);
        Tema.adicionarCampo(campos, "Data de nascimento (dd/mm/aaaa)", txtNascimento, 0, 1, 1);
        Tema.adicionarCampo(campos, "Telefone", txtTelefone, 1, 1, 1);
        Tema.adicionarCampo(campos, "ID (somente leitura)", txtId, 2, 1, 1);
        Tema.adicionarCampo(campos, "Endereço", txtEndereco, 0, 2, 2);
        Tema.adicionarCampo(campos, "Bairro", txtBairro, 2, 2, 1);
        Tema.adicionarCampo(campos, "Cidade", txtCidade, 0, 3, 1);
        Tema.adicionarCampo(campos, "Estado (UF)", cmbEstado, 1, 3, 1);
        Tema.adicionarCampo(campos, "CEP", txtCep, 2, 3, 1);
        card.add(campos, BorderLayout.CENTER);
        card.add(botoes, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------ ações

    private void pesquisar() {
        try {
            boolean ativos = !barra.isMostrarInativos();
            List<Cliente> lista = dao.pesquisar(barra.getTexto(), ativos);
            carregando = true;
            try {
                modelo.setItens(lista);
            } finally {
                carregando = false;
            }
            novo();
            lblContagem.setText(lista.size() + (ativos ? " cliente(s) ativo(s)" : " cliente(s) inativo(s)"));
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void novo() {
        selecionado = null;
        tabela.clearSelection();
        txtId.setText("");
        txtNome.setText("");
        txtCpf.setText("");
        txtNascimento.setText("");
        txtTelefone.setText("");
        txtEndereco.setText("");
        txtBairro.setText("");
        txtCidade.setText("");
        cmbEstado.setSelectedIndex(0);
        txtCep.setText("");
        lblFormulario.setText(barra.isMostrarInativos() ? "Clientes inativos (selecione um para reativar)" : "Novo cliente");
        definirFormularioEditavel(!barra.isMostrarInativos());
        botoes.atualizar(false, barra.isMostrarInativos());
    }

    private void carregarNoFormulario(Cliente c) {
        selecionado = c;
        txtId.setText(String.valueOf(c.getId()));
        txtNome.setText(c.getNome());
        txtCpf.setText(c.getCpf());
        txtNascimento.setText(Formatos.data(c.getDataNascimento()));
        txtTelefone.setText(Formatos.vazioSeNulo(c.getTelefone()));
        txtEndereco.setText(Formatos.vazioSeNulo(c.getEndereco()));
        txtBairro.setText(Formatos.vazioSeNulo(c.getBairro()));
        txtCidade.setText(Formatos.vazioSeNulo(c.getCidade()));
        cmbEstado.setSelectedItem(c.getEstado() == null ? "" : c.getEstado());
        txtCep.setText(Formatos.vazioSeNulo(c.getCep()));
        String ultima = c.getDataUltimaAlteracao() == null ? "" : "  (última alteração: " + Formatos.dataHora(c.getDataUltimaAlteracao()) + ")";
        lblFormulario.setText((c.isStatus() ? "Editar cliente #" : "Cliente inativo #") + c.getId() + ultima);
        definirFormularioEditavel(c.isStatus());
        botoes.atualizar(true, !c.isStatus());
    }

    private void definirFormularioEditavel(boolean editavel) {
        for (JTextField f : new JTextField[]{txtNome, txtCpf, txtNascimento, txtTelefone, txtEndereco, txtBairro, txtCidade, txtCep}) {
            f.setEditable(editavel);
            f.setBackground(editavel ? java.awt.Color.WHITE : Tema.CAMPO_BLOQUEADO);
        }
        cmbEstado.setEnabled(editavel);
    }

    private void salvar() {
        Cliente c = new Cliente();
        try {
            c.setDataNascimento(Formatos.lerData(txtNascimento.getText()));
        } catch (IllegalArgumentException e) {
            Mensagens.aviso(this, e.getMessage());
            txtNascimento.requestFocusInWindow();
            return;
        }
        c.setNome(txtNome.getText());
        c.setCpf(txtCpf.getText());
        c.setTelefone(txtTelefone.getText());
        c.setEndereco(txtEndereco.getText());
        c.setBairro(txtBairro.getText());
        c.setCidade(txtCidade.getText());
        c.setEstado((String) cmbEstado.getSelectedItem());
        c.setCep(txtCep.getText());

        try {
            if (selecionado == null) {
                dao.inserir(c);
                Mensagens.sucesso(this, "Cliente cadastrado com sucesso!\n\nNome: " + c.getNome() + "\nCPF: " + c.getCpf() + "\nID: " + c.getId());
            } else {
                c.setId(selecionado.getId());
                dao.alterar(c);
                Mensagens.sucesso(this, "Cliente alterado com sucesso!\n\nNome: " + c.getNome() + "\nID: " + c.getId());
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
        boolean sim = Mensagens.confirmar(this, "Desativar cliente",
                "Deseja realmente desativar o cliente \"" + selecionado.getNome() + "\"?\n\n"
                        + "O cadastro deixará de aparecer nas consultas, mas continuará no banco de dados.");
        if (!sim) {
            return;
        }
        try {
            dao.desativar(selecionado.getId());
            Mensagens.sucesso(this, "Cliente desativado com sucesso!");
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void reativar() {
        if (selecionado == null) {
            return;
        }
        boolean sim = Mensagens.confirmar(this, "Reativar cliente",
                "Deseja reativar o cliente \"" + selecionado.getNome() + "\"?");
        if (!sim) {
            return;
        }
        try {
            dao.reativar(selecionado.getId());
            Mensagens.sucesso(this, "Cliente reativado com sucesso!");
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private static String cidadeUf(Cliente c) {
        String cidade = Formatos.vazioSeNulo(c.getCidade());
        String uf = Formatos.vazioSeNulo(c.getEstado());
        if (cidade.isEmpty()) {
            return uf;
        }
        return uf.isEmpty() ? cidade : cidade + "/" + uf;
    }
}
