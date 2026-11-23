package dao;

import factory.ConnectionFactory;
import modelo.Cliente;
import util.Validacao;
import util.ValidadorCPF;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados e regras de negócio de Cliente.
 *
 * Regras: nome e CPF obrigatórios; CPF validado matematicamente e único (inclusive
 * ao alterar); status inicial ativo; ID nunca é alterado; data_ultima_alteracao é
 * atualizada a cada alteração; exclusão lógica (UPDATE status = FALSE).
 */
public class ClienteDAO {

    private static final String SELECT_BASE =
            "SELECT ID_Cliente, Nome, CPF, Data_Nascimento, Telefone, Endereco, Bairro, Cidade, "
            + "Estado, CEP, status, data_ultima_alteracao FROM cliente";

    private static final String CPF_SOMENTE_DIGITOS = "REPLACE(REPLACE(CPF, '.', ''), '-', '')";

    /** Cadastra um cliente (sempre ativo) e preenche o ID gerado no objeto. */
    public void inserir(Cliente c) throws DAOException {
        validar(c);
        verificarCpfDuplicado(c.getCpf(), 0);

        String sql = "INSERT INTO cliente (Nome, CPF, Data_Nascimento, Telefone, Endereco, Bairro, "
                + "Cidade, Estado, CEP, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherCampos(ps, c);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                }
            }
            c.setStatus(true);
            c.setDataUltimaAlteracao(null);
        } catch (SQLException e) {
            throw new DAOException("Erro ao cadastrar o cliente: " + e.getMessage(), e);
        }
    }

    /** Altera os dados de um cliente ativo. O ID e o status não são alterados. */
    public void alterar(Cliente c) throws DAOException {
        Cliente atual = buscarPorId(c.getId());
        if (atual == null) {
            throw new RegraNegocioException("Cliente não encontrado (ID " + c.getId() + ").");
        }
        if (!atual.isStatus()) {
            throw new RegraNegocioException("Cliente inativo não pode ser alterado. Reative-o primeiro.");
        }
        validar(c);
        verificarCpfDuplicado(c.getCpf(), c.getId());

        LocalDateTime agora = LocalDateTime.now().withNano(0);
        String sql = "UPDATE cliente SET Nome = ?, CPF = ?, Data_Nascimento = ?, Telefone = ?, Endereco = ?, "
                + "Bairro = ?, Cidade = ?, Estado = ?, CEP = ?, data_ultima_alteracao = ? WHERE ID_Cliente = ?";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencherCampos(ps, c);
            ps.setTimestamp(10, Timestamp.valueOf(agora));
            ps.setInt(11, c.getId());
            ps.executeUpdate();
            c.setStatus(true);
            c.setDataUltimaAlteracao(agora);
        } catch (SQLException e) {
            throw new DAOException("Erro ao alterar o cliente: " + e.getMessage(), e);
        }
    }

    /** Exclusão lógica. Não permite desativar cliente que ainda tenha animais ativos. */
    public void desativar(int id) throws DAOException {
        Cliente atual = buscarPorId(id);
        if (atual == null) {
            throw new RegraNegocioException("Cliente não encontrado (ID " + id + ").");
        }
        if (!atual.isStatus()) {
            throw new RegraNegocioException("O cliente já está inativo.");
        }
        int animais = contarAnimaisAtivos(id);
        if (animais > 0) {
            throw new RegraNegocioException("Não é possível desativar o cliente: ele possui " + animais
                    + " animal(is) ativo(s). Desative os animais primeiro.");
        }
        atualizarStatus(id, false);
    }

    /** Habilita novamente um cliente desativado. */
    public void reativar(int id) throws DAOException {
        Cliente atual = buscarPorId(id);
        if (atual == null) {
            throw new RegraNegocioException("Cliente não encontrado (ID " + id + ").");
        }
        if (atual.isStatus()) {
            throw new RegraNegocioException("O cliente já está ativo.");
        }
        atualizarStatus(id, true);
    }

    /** Busca por ID, qualquer status. Retorna null se não existir. */
    public Cliente buscarPorId(int id) throws DAOException {
        return buscarUm(SELECT_BASE + " WHERE ID_Cliente = ?", ps -> ps.setInt(1, id));
    }

    /** Busca por CPF (com ou sem máscara), qualquer status. Retorna null se não existir. */
    public Cliente buscarPorCpf(String cpf) throws DAOException {
        String digitos = ValidadorCPF.somenteDigitos(cpf);
        return buscarUm(SELECT_BASE + " WHERE " + CPF_SOMENTE_DIGITOS + " = ?", ps -> ps.setString(1, digitos));
    }

    /** Lista todos os clientes ativos (ativos = true) ou inativos (ativos = false), ordenados por nome. */
    public List<Cliente> listar(boolean ativos) throws DAOException {
        return pesquisar(null, ativos);
    }

    /** Pesquisa por parte do nome ou do CPF. Termo vazio equivale a listar tudo. */
    public List<Cliente> pesquisar(String termo, boolean ativos) throws DAOException {
        String t = termo == null ? "" : termo.trim();
        // Só compara com o CPF sem máscara se o termo "parece um CPF" (apenas dígitos, pontos e traço).
        String digitos = t.matches("[0-9.\\-\\s]+") ? ValidadorCPF.somenteDigitos(t) : "";
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE status = ?");
        if (!t.isEmpty()) {
            sql.append(" AND (Nome LIKE ? OR CPF LIKE ?");
            if (!digitos.isEmpty()) {
                sql.append(" OR ").append(CPF_SOMENTE_DIGITOS).append(" LIKE ?");
            }
            sql.append(")");
        }
        sql.append(" ORDER BY Nome");

        List<Cliente> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setBoolean(i++, ativos);
            if (!t.isEmpty()) {
                ps.setString(i++, Validacao.padraoLike(t));
                ps.setString(i++, Validacao.padraoLike(t));
                if (!digitos.isEmpty()) {
                    ps.setString(i++, Validacao.padraoLike(digitos));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar clientes: " + e.getMessage(), e);
        }
        return lista;
    }

    /** Clientes ativos que fazem aniversário no mês informado (1 a 12). */
    public List<Cliente> listarAniversariantesDoMes(int mes) throws DAOException {
        if (mes < 1 || mes > 12) {
            throw new RegraNegocioException("Mês inválido. Informe um valor de 1 a 12.");
        }
        String sql = SELECT_BASE + " WHERE status = TRUE AND MONTH(Data_Nascimento) = ? "
                + "ORDER BY DAY(Data_Nascimento), Nome";
        List<Cliente> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar aniversariantes: " + e.getMessage(), e);
        }
        return lista;
    }

    /** Quantidade de clientes ativos (dashboard). */
    public int contarAtivos() throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM cliente WHERE status = TRUE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DAOException("Erro ao contar clientes: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ privados

    /** Valida e normaliza os campos do cliente (trim, CPF formatado, UF em maiúsculas). */
    private void validar(Cliente c) throws RegraNegocioException {
        if (c == null) {
            throw new RegraNegocioException("Cliente não informado.");
        }
        c.setNome(Validacao.limparOuNull(c.getNome()));
        c.setCpf(Validacao.limparOuNull(c.getCpf()));
        c.setTelefone(Validacao.limparOuNull(c.getTelefone()));
        c.setEndereco(Validacao.limparOuNull(c.getEndereco()));
        c.setBairro(Validacao.limparOuNull(c.getBairro()));
        c.setCidade(Validacao.limparOuNull(c.getCidade()));
        c.setEstado(Validacao.limparOuNull(c.getEstado()));
        c.setCep(Validacao.limparOuNull(c.getCep()));

        if (c.getNome() == null) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (c.getCpf() == null) {
            throw new RegraNegocioException("O CPF do cliente é obrigatório.");
        }
        if (!ValidadorCPF.isValido(c.getCpf())) {
            throw new RegraNegocioException("CPF inválido: " + c.getCpf());
        }
        c.setCpf(ValidadorCPF.formatar(c.getCpf()));

        if (Validacao.excede(c.getNome(), 100)) {
            throw new RegraNegocioException("O nome pode ter no máximo 100 caracteres.");
        }
        if (Validacao.excede(c.getTelefone(), 20)) {
            throw new RegraNegocioException("O telefone pode ter no máximo 20 caracteres.");
        }
        if (Validacao.excede(c.getEndereco(), 150)) {
            throw new RegraNegocioException("O endereço pode ter no máximo 150 caracteres.");
        }
        if (Validacao.excede(c.getBairro(), 80)) {
            throw new RegraNegocioException("O bairro pode ter no máximo 80 caracteres.");
        }
        if (Validacao.excede(c.getCidade(), 80)) {
            throw new RegraNegocioException("A cidade pode ter no máximo 80 caracteres.");
        }
        if (Validacao.excede(c.getCep(), 10)) {
            throw new RegraNegocioException("O CEP pode ter no máximo 10 caracteres.");
        }
        if (c.getEstado() != null) {
            String uf = c.getEstado().toUpperCase();
            if (!uf.matches("[A-Z]{2}")) {
                throw new RegraNegocioException("O estado deve ser a sigla da UF com 2 letras (ex.: SP).");
            }
            c.setEstado(uf);
        }
        if (c.getDataNascimento() != null && c.getDataNascimento().isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de nascimento não pode ser futura.");
        }
    }

    /** Impede CPF repetido (considerando também clientes inativos, pois o CPF é único no banco). */
    private void verificarCpfDuplicado(String cpf, int idIgnorado) throws DAOException {
        String sql = "SELECT Nome, status FROM cliente WHERE " + CPF_SOMENTE_DIGITOS + " = ? AND ID_Cliente <> ?";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ValidadorCPF.somenteDigitos(cpf));
            ps.setInt(2, idIgnorado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String msg = "Já existe um cliente cadastrado com este CPF: " + rs.getString("Nome") + ".";
                    if (!rs.getBoolean("status")) {
                        msg += " Esse cadastro está inativo; reative-o em vez de cadastrar novamente.";
                    }
                    throw new RegraNegocioException(msg);
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao verificar CPF duplicado: " + e.getMessage(), e);
        }
    }

    private int contarAnimaisAtivos(int idCliente) throws DAOException {
        String sql = "SELECT COUNT(*) FROM animal WHERE ID_Cliente = ? AND Status = TRUE";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao verificar animais do cliente: " + e.getMessage(), e);
        }
    }

    private void atualizarStatus(int id, boolean ativo) throws DAOException {
        String sql = "UPDATE cliente SET status = ?, data_ultima_alteracao = ? WHERE ID_Cliente = ?";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, ativo);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Erro ao alterar o status do cliente: " + e.getMessage(), e);
        }
    }

    /** Preenche os 9 primeiros parâmetros (Nome ... CEP), comuns ao INSERT e ao UPDATE. */
    private void preencherCampos(PreparedStatement ps, Cliente c) throws SQLException {
        ps.setString(1, c.getNome());
        ps.setString(2, c.getCpf());
        if (c.getDataNascimento() == null) {
            ps.setNull(3, Types.DATE);
        } else {
            ps.setDate(3, Date.valueOf(c.getDataNascimento()));
        }
        ps.setString(4, c.getTelefone());
        ps.setString(5, c.getEndereco());
        ps.setString(6, c.getBairro());
        ps.setString(7, c.getCidade());
        ps.setString(8, c.getEstado());
        ps.setString(9, c.getCep());
    }

    @FunctionalInterface
    private interface Parametros {
        void definir(PreparedStatement ps) throws SQLException;
    }

    private Cliente buscarUm(String sql, Parametros parametros) throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            parametros.definir(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar cliente: " + e.getMessage(), e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("ID_Cliente"));
        c.setNome(rs.getString("Nome"));
        c.setCpf(rs.getString("CPF"));
        Date nascimento = rs.getDate("Data_Nascimento");
        c.setDataNascimento(nascimento == null ? null : nascimento.toLocalDate());
        c.setTelefone(rs.getString("Telefone"));
        c.setEndereco(rs.getString("Endereco"));
        c.setBairro(rs.getString("Bairro"));
        c.setCidade(rs.getString("Cidade"));
        c.setEstado(rs.getString("Estado"));
        c.setCep(rs.getString("CEP"));
        c.setStatus(rs.getBoolean("status"));
        Timestamp alteracao = rs.getTimestamp("data_ultima_alteracao");
        c.setDataUltimaAlteracao(alteracao == null ? null : alteracao.toLocalDateTime());
        return c;
    }
}
