package principal;

import factory.ConnectionFactory;
import gui.JanelaPrincipal;
import gui.Tema;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/** Ponto de entrada do sistema. */
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        configurarAparencia();

        // Verifica a conexão antes de abrir a janela, para dar uma mensagem clara em caso de erro.
        try (Connection c = ConnectionFactory.getConnection()) {
            c.isValid(5);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível conectar ao banco de dados.\n\n"
                            + e.getMessage() + "\n\n"
                            + "Verifique se:\n"
                            + " - o MySQL está em execução e o Script.sql foi executado;\n"
                            + " - o arquivo config.properties existe na pasta do projeto (copie de config.properties.example);\n"
                            + " - o driver JDBC (mysql-connector-j) está na pasta lib/ e no classpath.",
                    "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> new JanelaPrincipal().setVisible(true));
    }

    /**
     * Usa o FlatLaf se o JAR estiver em lib/ (aparência moderna); caso contrário usa o
     * Nimbus, que já vem com o Java. O FlatLaf é carregado por nome, então o projeto
     * compila e roda normalmente sem ele.
     */
    public static void configurarAparencia() {
        String[] opcoes = {"com.formdev.flatlaf.FlatLightLaf", "javax.swing.plaf.nimbus.NimbusLookAndFeel"};
        boolean aplicado = false;
        for (String laf : opcoes) {
            try {
                UIManager.setLookAndFeel(laf);
                aplicado = true;
                break;
            } catch (Exception | LinkageError e) {
                // tenta a próxima opção
            }
        }
        if (!aplicado) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                // mantém o padrão do Java
            }
        }
        Tema.instalarFontePadrao();
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.yesButtonText", "Sim");
        UIManager.put("OptionPane.noButtonText", "Não");
    }
}
