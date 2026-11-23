package gui;

import dao.DAOException;
import dao.RacaDAO;
import modelo.Raca;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.List;

/** Tela de raças: listar/pesquisar, cadastrar (tipo Cachorro ou Gato), alterar, desativar e reativar. */
public class PainelRacas extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final RacaDAO dao = new RacaDAO();

    private final ModeloTabela<Raca> modelo = new ModeloTabela<>(
            new String[]{"ID", "Raça", "Tipo de animal", "Status"},
            r -> new Object[]{r.getId(), r.getNome(), r.getTipoAnimal(), Formatos.status(r.isStatus())});
    private final JTable tabela = Tema.tabela(modelo);
    private final BarraPesquisa barra = new BarraPesquisa("Pesquisar por nome da raça ou tipo", this::pesquisar);
    private final JLabel lblContagem = Tema.subtitulo(" ");

    private final JLabel lblFormulario = Tema.secao("Nova raça");
    private final JTextField txtId = Tema.campo();
    private final JTextField txtNome = Tema.campo();
    private final JComboBox<String> cmbTipo = new JComboBox<>(Raca.TIPOS);
    private final BotoesCrud botoes;

    private Raca selecionada;
    private boolean carregando;

    public PainelRacas() {
        setLayout(new BorderLayout(18, 14));
        setBackground(Tema.FUNDO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        botoes = new BotoesCrud(true, this::novo, this::salvar, this::desativar, this::reativar);
        add(criarTopo(), BorderLayout.NORTH);
        add(Tema.rolagem(tabela), BorderLayout.CENTER);
        add(criarFormulario(), BorderLayout.EAST);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !carregando) {
                int linha = tabela.getSelectedRow();
                if (linha >= 0) {
                    carregarNoFormulario(modelo.getItem(tabela.convertRowIndexToModel(linha)));
                }
            }
        });
        Tema.larguras(tabela, 50, 240, 150, 100);
        txtId.setEditable(false);
        txtId.setBackground(Tema.CAMPO_BLOQUEADO);
        txtId.setToolTipText("O ID é gerado pelo sistema e não pode ser alterado.");
        Tema.estilizarCampo(cmbTipo);
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

    private JPanel criarTopo() {
        JPanel topo = Tema.fundo();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel titulo = Tema.titulo("Raças");
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = Tema.subtitulo("Cadastre as raças de cachorros e gatos usadas no cadastro de animais.");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        barra.setAlignmentX(LEFT_ALIGNMENT);
        lblContagem.setAlignmentX(LEFT_ALIGNMENT);
        topo.add(titulo);
        topo.add(sub);
        topo.add(Box.createVerticalStrut(14));
        topo.add(barra);
        topo.add(Box.createVerticalStrut(8));
        topo.add(lblContagem);
        return topo;
    }

    private JPanel criarFormulario() {
        JPanel card = Tema.cartao();
        card.setLayout(new BorderLayout(0, 14));
        card.setPreferredSize(new Dimension(340, 0));
        card.add(lblFormulario, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        Tema.adicionarCampo(campos, "Nome da raça *", txtNome, 0, 0, 1);
        Tema.adicionarCampo(campos, "Tipo de animal *", cmbTipo, 0, 1, 1);
        Tema.adicionarCampo(campos, "ID (somente leitura)", txtId, 0, 2, 1);

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setOpaque(false);
        conteudo.add(campos, BorderLayout.NORTH);
        card.add(conteudo, BorderLayout.CENTER);
        card.add(botoes, BorderLayout.SOUTH);
        return card;
    }

    private void pesquisar() {
        try {
            boolean ativas = !barra.isMostrarInativos();
            List<Raca> lista = dao.pesquisar(barra.getTexto(), ativas);
            carregando = true;
            try {
                modelo.setItens(lista);
            } finally {
                carregando = false;
            }
            novo();
            lblContagem.setText(lista.size() + (ativas ? " raça(s) ativa(s)" : " raça(s) inativa(s)"));
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void novo() {
        selecionada = null;
        tabela.clearSelection();
        txtId.setText("");
        txtNome.setText("");
        cmbTipo.setSelectedIndex(0);
        lblFormulario.setText(barra.isMostrarInativos() ? "Raças inativas" : "Nova raça");
        definirFormularioEditavel(!barra.isMostrarInativos());
        botoes.atualizar(false, barra.isMostrarInativos());
    }

    private void carregarNoFormulario(Raca r) {
        selecionada = r;
        txtId.setText(String.valueOf(r.getId()));
        txtNome.setText(r.getNome());
        cmbTipo.setSelectedItem(r.getTipoAnimal());
        lblFormulario.setText((r.isStatus() ? "Editar raça #" : "Raça inativa #") + r.getId());
        definirFormularioEditavel(r.isStatus());
        botoes.atualizar(true, !r.isStatus());
    }

    private void definirFormularioEditavel(boolean editavel) {
        txtNome.setEditable(editavel);
        txtNome.setBackground(editavel ? Color.WHITE : Tema.CAMPO_BLOQUEADO);
        cmbTipo.setEnabled(editavel);
    }

    private void salvar() {
        Raca r = new Raca(txtNome.getText(), (String) cmbTipo.getSelectedItem());
        try {
            if (selecionada == null) {
                dao.inserir(r);
                Mensagens.sucesso(this, "Raça cadastrada com sucesso!\n\n" + r + "\nID: " + r.getId());
            } else {
                r.setId(selecionada.getId());
                dao.alterar(r);
                Mensagens.sucesso(this, "Raça alterada com sucesso!\n\n" + r + "\nID: " + r.getId());
            }
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void desativar() {
        if (selecionada == null) {
            return;
        }
        boolean sim = Mensagens.confirmar(this, "Desativar raça",
                "Deseja realmente desativar a raça \"" + selecionada + "\"?\n\n"
                        + "Ela deixará de aparecer nas consultas e no cadastro de animais.");
        if (!sim) {
            return;
        }
        try {
            dao.desativar(selecionada.getId());
            Mensagens.sucesso(this, "Raça desativada com sucesso!");
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }

    private void reativar() {
        if (selecionada == null) {
            return;
        }
        boolean sim = Mensagens.confirmar(this, "Reativar raça",
                "Deseja reativar a raça \"" + selecionada + "\"?");
        if (!sim) {
            return;
        }
        try {
            dao.reativar(selecionada.getId());
            Mensagens.sucesso(this, "Raça reativada com sucesso!");
            pesquisar();
        } catch (DAOException e) {
            Mensagens.tratar(this, e);
        }
    }
}
