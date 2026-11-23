package teste;

import gui.JanelaPrincipal;
import principal.Main;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ferramenta usada para gerar as capturas de tela do manual do usuário e para exercitar
 * a interface de ponta a ponta: abre a janela real, navega pelas telas, preenche formulários,
 * clica nos botões (inclusive os que abrem diálogos) e salva PNGs.
 *
 * Uso (precisa de ambiente gráfico; em Linux sem monitor use xvfb-run):
 *   java -cp "bin:lib/*" teste.CapturarTelas pasta_de_saida
 *
 * ATENÇÃO: cadastra registros reais no banco e gera relatórios na pasta relatorios/.
 * Execute o Script.sql depois para voltar ao estado inicial.
 */
public class CapturarTelas {

    private static Robot robot;
    private static JanelaPrincipal janela;
    private static File saida;

    public static void main(String[] args) throws Exception {
        saida = new File(args.length > 0 ? args[0] : "docs/telas");
        saida.mkdirs();
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        Main.configurarAparencia();
        robot = new Robot();

        SwingUtilities.invokeAndWait(() -> {
            janela = new JanelaPrincipal();
            janela.setLocation(20, 20);
            janela.setVisible(true);
        });
        esperar(1500);

        // 1. Painel inicial
        capturarJanela("01_painel_inicial");

        // 2. Clientes: listagem
        navegar("Clientes");
        capturarJanela("02_clientes_listagem");

        // 3. Clientes: formulário preenchido e cadastro com sucesso
        List<JTextField> campos = campos();
        preencher(campos.get(1), "Marina Costa");
        preencher(campos.get(2), "168.995.350-09");
        preencher(campos.get(3), "08/09/1992");
        preencher(campos.get(4), "(12) 99555-4321");
        preencher(campos.get(6), "Rua das Acácias, 77");
        preencher(campos.get(7), "Jardim das Colinas");
        preencher(campos.get(8), "São José dos Campos");
        preencher(campos.get(9), "12242-010");
        selecionarNoCombo("SP");
        capturarJanela("03_clientes_formulario_preenchido");
        clicarEcapturarDialogo("Cadastrar", "04_clientes_cadastro_sucesso");
        capturarJanela("05_clientes_apos_cadastro");

        // 4. Clientes: CPF inválido
        campos = campos();
        preencher(campos.get(1), "Cliente Teste");
        preencher(campos.get(2), "123.456.789-00");
        clicarEcapturarDialogo("Cadastrar", "06_clientes_cpf_invalido");

        // 5. Clientes: CPF duplicado
        preencher(campos.get(2), "123.456.789-09");
        clicarEcapturarDialogo("Cadastrar", "07_clientes_cpf_duplicado");

        // 6. Clientes: edição (seleciona a primeira linha) e confirmação de desativação
        navegar("Clientes");
        selecionarLinha(0, 0);
        capturarJanela("08_clientes_edicao");
        clicarEcapturarDialogo("Desativar", "09_clientes_confirmar_desativacao");

        // 7. Raças
        navegar("Raças");
        capturarJanela("10_racas_listagem");
        campos = campos();
        preencher(campos.get(1), "Border Collie");
        capturarJanela("11_racas_formulario");
        clicarEcapturarDialogo("Cadastrar", "12_racas_cadastro_sucesso");
        preencher(campos().get(1), "Poodle");
        clicarEcapturarDialogo("Cadastrar", "13_racas_duplicada");

        // 8. Animais: cadastro
        navegar("Animais");
        capturarJanela("14_animais_listagem");
        selecionarLinha(0, 0);
        capturarJanela("15_animais_edicao");

        campos = campos();
        clicarSemCapturar("Novo / Limpar");
        campos = campos();
        preencher(campos.get(1), "Pipoca");
        preencher(campos.get(2), "10/05/2024");
        capturarJanela("16_animais_formulario_novo");
        clicarEcapturarDialogo("Cadastrar", "17_animais_cadastro_sucesso");

        // 9. Animais: consulta por cliente
        clicarSemCapturar("Consultar por cliente");
        esperar(300);
        campos = campos();
        preencher(campos.get(0), "Ana");
        clicarSemCapturar("Consultar");
        esperar(500);
        capturarJanela("18_animais_consulta_por_nome");
        preencher(campos.get(0), "");
        preencher(campos.get(1), "111.444.777-35");
        clicarSemCapturar("Consultar");
        esperar(500);
        capturarJanela("19_animais_consulta_por_cpf");

        // 10. Relatórios
        navegar("Relatórios");
        capturarJanela("20_relatorios");
        List<JButton> gerar = botoes("Gerar relatório");
        clicarBotaoEcapturar(gerar.get(0), "21_relatorios_geral_sucesso");
        selecionarMes(0, 2);
        selecionarMes(1, 2);
        clicarBotaoEcapturar(gerar.get(1), "22_relatorios_animais_sucesso");
        clicarBotaoEcapturar(gerar.get(2), "23_relatorios_clientes_sucesso");
        capturarJanela("24_relatorios_arquivos_gerados");

        // 11. Mostrar inativos (desativa a cliente recém-cadastrada, que não tem animais, direto pelo DAO)
        dao.ClienteDAO clienteDAO = new dao.ClienteDAO();
        clienteDAO.desativar(clienteDAO.buscarPorCpf("168.995.350-09").getId());
        navegar("Clientes");
        clicarCheckbox("Mostrar inativos");
        esperar(500);
        selecionarLinha(0, 0);
        capturarJanela("25_clientes_inativos");

        System.out.println("Capturas salvas em " + saida.getAbsolutePath());
        SwingUtilities.invokeAndWait(() -> janela.dispose());
        System.exit(0);
    }

    // ------------------------------------------------------------------ ações

    private static void navegar(String textoDoMenu) throws Exception {
        clicarSemCapturar(textoDoMenu);
        esperar(600);
    }

    private static void capturarJanela(String nome) throws Exception {
        esperar(400);
        Rectangle r = new Rectangle(janela.getLocationOnScreen(), janela.getSize());
        salvar(robot.createScreenCapture(r), nome);
    }

    /** Clica no botão (abre um diálogo modal), captura o diálogo e o fecha. */
    private static void clicarEcapturarDialogo(String textoBotao, String nome) throws Exception {
        clicarBotaoEcapturar(botoes(textoBotao).get(0), nome);
    }

    private static void clicarBotaoEcapturar(JButton botao, String nome) throws Exception {
        SwingUtilities.invokeLater(botao::doClick);
        esperar(900);
        JDialog dialogo = dialogoVisivel();
        if (dialogo != null) {
            Rectangle r = new Rectangle(dialogo.getLocationOnScreen(), dialogo.getSize());
            salvar(robot.createScreenCapture(r), nome);
            SwingUtilities.invokeAndWait(dialogo::dispose);
        } else {
            System.out.println("AVISO: nenhum diálogo apareceu para " + nome);
        }
        esperar(700);
    }

    private static void clicarSemCapturar(String texto) throws Exception {
        List<JButton> lista = botoes(texto);
        if (lista.isEmpty()) {
            throw new IllegalStateException("Botão não encontrado: " + texto);
        }
        SwingUtilities.invokeAndWait(lista.get(0)::doClick);
        esperar(400);
    }

    private static void clicarCheckbox(String texto) throws Exception {
        for (Component c : todos(janela, javax.swing.JCheckBox.class)) {
            javax.swing.JCheckBox cb = (javax.swing.JCheckBox) c;
            if (cb.isShowing() && texto.equals(cb.getText())) {
                SwingUtilities.invokeAndWait(cb::doClick);
                return;
            }
        }
        throw new IllegalStateException("Checkbox não encontrado: " + texto);
    }

    private static void preencher(JTextField campo, String texto) throws Exception {
        SwingUtilities.invokeAndWait(() -> campo.setText(texto));
    }

    private static void selecionarNoCombo(String valor) throws Exception {
        for (JComboBox<?> combo : combosVisiveis()) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (valor.equals(String.valueOf(combo.getItemAt(i)))) {
                    int indice = i;
                    SwingUtilities.invokeAndWait(() -> combo.setSelectedIndex(indice));
                    return;
                }
            }
        }
        throw new IllegalStateException("Item de combo não encontrado: " + valor);
    }

    private static void selecionarMes(int qualCombo, int indiceMes) throws Exception {
        List<JComboBox<?>> meses = new ArrayList<>();
        for (JComboBox<?> combo : combosVisiveis()) {
            if (combo.getItemCount() == 12) {
                meses.add(combo);
            }
        }
        SwingUtilities.invokeAndWait(() -> meses.get(qualCombo).setSelectedIndex(indiceMes));
    }

    private static void selecionarLinha(int qualTabela, int linha) throws Exception {
        List<JTable> tabelas = new ArrayList<>();
        for (Component c : todos(janela, JTable.class)) {
            if (c.isShowing()) {
                tabelas.add((JTable) c);
            }
        }
        JTable t = tabelas.get(qualTabela);
        SwingUtilities.invokeAndWait(() -> t.setRowSelectionInterval(linha, linha));
        esperar(400);
    }

    // ------------------------------------------------------------------ busca de componentes

    /** Campos de texto visíveis, na ordem em que foram adicionados à tela. */
    private static List<JTextField> campos() {
        List<JTextField> lista = new ArrayList<>();
        for (Component c : todos(janela, JTextField.class)) {
            if (c.isShowing()) {
                lista.add((JTextField) c);
            }
        }
        return lista;
    }

    private static List<JComboBox<?>> combosVisiveis() {
        List<JComboBox<?>> lista = new ArrayList<>();
        for (Component c : todos(janela, JComboBox.class)) {
            if (c.isShowing()) {
                lista.add((JComboBox<?>) c);
            }
        }
        return lista;
    }

    private static List<JButton> botoes(String texto) {
        List<JButton> lista = new ArrayList<>();
        for (Component c : todos(janela, JButton.class)) {
            JButton b = (JButton) c;
            if (b.isShowing() && texto.equals(b.getText())) {
                lista.add(b);
            }
        }
        return lista;
    }

    private static List<Component> todos(Container raiz, Class<?> tipo) {
        List<Component> lista = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c)) {
                lista.add(c);
            }
            if (c instanceof Container) {
                lista.addAll(todos((Container) c, tipo));
            }
        }
        return lista;
    }

    private static JDialog dialogoVisivel() {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && w.isShowing()) {
                return (JDialog) w;
            }
        }
        return null;
    }

    private static void salvar(BufferedImage imagem, String nome) throws Exception {
        ImageIO.write(imagem, "png", new File(saida, nome + ".png"));
        System.out.println("  capturada: " + nome + ".png");
    }

    private static void esperar(int ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
