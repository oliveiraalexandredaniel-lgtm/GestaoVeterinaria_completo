package dao;

import factory.ConnectionFactory;
import modelo.Raca;
import util.Validacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados e regras de negócio de Raça.
 *
 * Regras: nome e tipo (Cachorro ou Gato) obrigatórios; não permite o mesmo nome
 * duas vezes para o mesmo tipo (mas permite Poodle para Cachorro e para Gato);
 * ID nunca é alterado; exclusão lógica.
 */
public class RacaDAO {

    private static final String SELECT_BASE = "SELECT ID_Raca, Nome_Raca, Tipo_Animal, Status FROM raca";

    /** Cadastra uma raça (sempre ativa) e preenche o ID gerado no objeto. */
    public void inserir(Raca r) throws DAOException {
        validar(r);
        verificarDuplicidade(r.getNome(), r.getTipoAnimal(), 0);

        String sql = "INSERT INTO raca (Nome_Raca, Tipo_Animal, Status) VALUES (?, ?, TRUE)";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getNome());
            ps.setString(2, r.getTipoAnimal());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getInt(1));
                }
            }
            r.setStatus(true);
        } catch (SQLException e) {
            throw new DAOException("Erro ao cadastrar a raça: " + e.getMessage(), e);
        }
    }

    /** Altera nome e tipo de uma raça ativa. O ID e o status não são alterados. */
    public void alterar(Raca r) throws DAOException {
        Raca atual = buscarPorId(r.getId());
        if (atual == null) {
            throw new RegraNegocioException("Raça não encontrada (ID " + r.getId() + ").");
        }
        if (!atual.isStatus()) {
            throw new RegraNegocioException("Raça inativa não pode ser alterada. Reative-a primeiro.");
        }
        validar(r);
        if (!atual.getTipoAnimal().equals(r.getTipoAnimal()) && contarAnimais(r.getId(), false) > 0) {
            throw new RegraNegocioException("Não é possível mudar o tipo de uma raça que já possui animais vinculados.");
        }
        verificarDuplicidade(r.getNome(), r.getTipoAnimal(), r.getId());

        String sql = "UPDATE raca SET Nome_Raca = ?, Tipo_Animal = ? WHERE ID_Raca = ?";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getNome());
            ps.setString(2, r.getTipoAnimal());
            ps.setInt(3, r.getId());
            ps.executeUpdate();
            r.setStatus(true);
        } catch (SQLException e) {
            throw new DAOException("Erro ao alterar a raça: " + e.getMessage(), e);
        }
    }

    /** Exclusão lógica. Não permite desativar raça que ainda tenha animais ativos. */
    public void desativar(int id) throws DAOException {
        Raca atual = buscarPorId(id);
        if (atual == null) {
            throw new RegraNegocioException("Raça não encontrada (ID " + id + ").");
        }
        if (!atual.isStatus()) {
            throw new RegraNegocioException("A raça já está inativa.");
        }
        int animais = contarAnimais(id, true);
        if (animais > 0) {
            throw new RegraNegocioException("Não é possível desativar a raça: há " + animais
                    + " animal(is) ativo(s) vinculado(s) a ela.");
        }
        atualizarStatus(id, false);
    }

    /** Habilita novamente uma raça desativada. */
    public void reativar(int id) throws DAOException {
        Raca atual = buscarPorId(id);
        if (atual == null) {
            throw new RegraNegocioException("Raça não encontrada (ID " + id + ").");
        }
        if (atual.isStatus()) {
            throw new RegraNegocioException("A raça já está ativa.");
        }
        atualizarStatus(id, true);
    }

    /** Busca por ID, qualquer status. Retorna null se não existir. */
    public Raca buscarPorId(int id) throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + " WHERE ID_Raca = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar raça: " + e.getMessage(), e);
        }
    }

    /** Lista as raças ativas (true) ou inativas (false), ordenadas por tipo e nome. */
    public List<Raca> listar(boolean ativas) throws DAOException {
        return pesquisar(null, ativas);
    }

    /** Pesquisa por parte do nome da raça ou do tipo. Termo vazio equivale a listar tudo. */
    public List<Raca> pesquisar(String termo, boolean ativas) throws DAOException {
        String t = termo == null ? "" : termo.trim();
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE Status = ?");
        if (!t.isEmpty()) {
            sql.append(" AND (Nome_Raca LIKE ? OR Tipo_Animal LIKE ?)");
        }
        sql.append(" ORDER BY Tipo_Animal, Nome_Raca");

        List<Raca> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setBoolean(1, ativas);
            if (!t.isEmpty()) {
                ps.setString(2, Validacao.padraoLike(t));
                ps.setString(3, Validacao.padraoLike(t));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar raças: " + e.getMessage(), e);
        }
        return lista;
    }

    /** Quantidade de raças ativas (dashboard). */
    public int contarAtivas() throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM raca WHERE Status = TRUE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DAOException("Erro ao contar raças: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ privados

    /** Valida e normaliza nome e tipo (aceita "gato" ou "GATO" e grava "Gato"). */
    private void validar(Raca r) throws RegraNegocioException {
        if (r == null) {
            throw new RegraNegocioException("Raça não informada.");
        }
        r.setNome(Validacao.limparOuNull(r.getNome()));
        if (r.getNome() == null) {
            throw new RegraNegocioException("O nome da raça é obrigatório.");
        }
        if (Validacao.excede(r.getNome(), 80)) {
            throw new RegraNegocioException("O nome da raça pode ter no máximo 80 caracteres.");
        }
        String tipo = Validacao.limparOuNull(r.getTipoAnimal());
        if (tipo == null) {
            throw new RegraNegocioException("O tipo de animal é obrigatório (Cachorro ou Gato).");
        }
        String tipoNormalizado = null;
        for (String valido : Raca.TIPOS) {
            if (valido.equalsIgnoreCase(tipo)) {
                tipoNormalizado = valido;
            }
        }
        if (tipoNormalizado == null) {
            throw new RegraNegocioException("Tipo de animal inválido: '" + tipo + "'. Use Cachorro ou Gato.");
        }
        r.setTipoAnimal(tipoNormalizado);
    }

    /**
     * Impede nome repetido para o mesmo tipo. A comparação do MySQL (collation
     * utf8mb4_unicode_ci) ignora maiúsculas/minúsculas e acentos. Considera também raças inativas.
     */
    private void verificarDuplicidade(String nome, String tipo, int idIgnorado) throws DAOException {
        String sql = "SELECT Status FROM raca WHERE Nome_Raca = ? AND Tipo_Animal = ? AND ID_Raca <> ?";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, tipo);
            ps.setInt(3, idIgnorado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String msg = "Já existe a raça '" + nome + "' para o tipo " + tipo + ".";
                    if (!rs.getBoolean("Status")) {
                        msg += " Ela está inativa; reative-a em vez de cadastrar novamente.";
                    }
                    throw new RegraNegocioException(msg);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao verificar raça duplicada: " + e.getMessage(), e);
        }
    }

    /** Conta animais vinculados à raça (somente ativos ou de qualquer status). */
    private int contarAnimais(int idRaca, boolean somenteAtivos) throws DAOException {
        String sql = "SELECT COUNT(*) FROM animal WHERE ID_Raca = ?" + (somenteAtivos ? " AND Status = TRUE" : "");
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRaca);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao verificar animais da raça: " + e.getMessage(), e);
        }
    }

    private void atualizarStatus(int id, boolean ativa) throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE raca SET Status = ? WHERE ID_Raca = ?")) {
            ps.setBoolean(1, ativa);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Erro ao alterar o status da raça: " + e.getMessage(), e);
        }
    }

    private Raca mapear(ResultSet rs) throws SQLException {
        Raca r = new Raca();
        r.setId(rs.getInt("ID_Raca"));
        r.setNome(rs.getString("Nome_Raca"));
        r.setTipoAnimal(rs.getString("Tipo_Animal"));
        r.setStatus(rs.getBoolean("Status"));
        return r;
    }
}
