package factory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica de conexões JDBC com o MySQL.
 *
 * As credenciais NÃO ficam no código. Elas são lidas, nesta ordem de prioridade:
 *   1. variáveis de ambiente DB_URL, DB_USUARIO e DB_SENHA;
 *   2. arquivo "config.properties" na pasta de trabalho (raiz do projeto);
 *   3. arquivo "config.properties" na raiz do classpath.
 *
 * O driver JDBC (MySQL Connector/J) é localizado automaticamente pelo
 * DriverManager, bastando que o JAR esteja no classpath (pasta lib/).
 */
public final class ConnectionFactory {

    public static final String ARQUIVO_CONFIG = "config.properties";

    private static Properties configuracao;

    private ConnectionFactory() {
    }

    /** Abre uma nova conexão. Quem chama é responsável por fechá-la (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        Properties p = obterConfiguracao();
        String url = p.getProperty("db.url");
        String usuario = p.getProperty("db.usuario");
        String senha = p.getProperty("db.senha", "");
        if (url == null || url.isBlank() || usuario == null || usuario.isBlank()) {
            throw new SQLException("Configuração do banco incompleta. Defina db.url e db.usuario em "
                    + ARQUIVO_CONFIG + " (copie de config.properties.example).");
        }
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** Testa a conexão e devolve true se for possível conectar ao banco. */
    public static boolean testarConexao() {
        try (Connection c = getConnection()) {
            return c.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Descarta a configuração lida (útil se o arquivo foi alterado com o programa aberto). */
    public static synchronized void recarregarConfiguracao() {
        configuracao = null;
    }

    private static synchronized Properties obterConfiguracao() throws SQLException {
        if (configuracao != null) {
            return configuracao;
        }
        Properties p = new Properties();

        Path arquivo = Paths.get(ARQUIVO_CONFIG);
        try {
            if (Files.isReadable(arquivo)) {
                try (InputStream in = Files.newInputStream(arquivo)) {
                    p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                }
            } else {
                try (InputStream in = ConnectionFactory.class.getClassLoader().getResourceAsStream(ARQUIVO_CONFIG)) {
                    if (in != null) {
                        p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (IOException e) {
            throw new SQLException("Não foi possível ler o arquivo " + ARQUIVO_CONFIG + ": " + e.getMessage(), e);
        }

        sobrescreverComAmbiente(p, "db.url", "DB_URL");
        sobrescreverComAmbiente(p, "db.usuario", "DB_USUARIO");
        sobrescreverComAmbiente(p, "db.senha", "DB_SENHA");

        if (p.getProperty("db.url") == null) {
            throw new SQLException("Arquivo " + ARQUIVO_CONFIG + " não encontrado na pasta '"
                    + Paths.get("").toAbsolutePath() + "'. Copie config.properties.example para "
                    + ARQUIVO_CONFIG + " e informe usuário e senha do MySQL.");
        }
        configuracao = p;
        return p;
    }

    private static void sobrescreverComAmbiente(Properties p, String chave, String variavel) {
        String valor = System.getenv(variavel);
        if (valor != null) {
            p.setProperty(chave, valor);
        }
    }
}
