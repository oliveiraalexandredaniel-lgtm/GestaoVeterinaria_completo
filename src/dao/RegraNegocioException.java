package dao;

/**
 * Violação de regra de negócio (CPF inválido, duplicidade, campo obrigatório, etc.).
 * A interface a exibe como aviso, e não como erro do sistema.
 */
public class RegraNegocioException extends DAOException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
