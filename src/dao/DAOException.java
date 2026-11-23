package dao;

/**
 * Erro técnico na camada de dados (falha de conexão, SQL, etc.).
 * A mensagem já é amigável para ser exibida ao usuário.
 */
public class DAOException extends Exception {

    private static final long serialVersionUID = 1L;

    public DAOException(String mensagem) {
        super(mensagem);
    }

    public DAOException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
