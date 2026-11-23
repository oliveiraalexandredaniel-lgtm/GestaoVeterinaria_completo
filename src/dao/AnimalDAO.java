package dao;

import factory.ConnectionFactory;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import modelo.Animal;
import modelo.Cliente;
import modelo.Raca;
import util.Validacao;
import util.ValidadorCPF;

/**
 * Acesso a dados e regras de negócio de Animal.
 *
 * Regras: nome obrigatório; sexo M/F (ou não informado); o animal deve estar
 * vinculado a um cliente ATIVO e a uma raça cadastrada e ATIVA (verificado ao
 * cadastrar, alterar e reativar); ID nunca é alterado; exclusão lógica.
 */
public class AnimalDAO {

    private static final String SELECT_BASE =
            "SELECT a.ID_Animal, a.Nome, a.Data_Nascimento, a.Sexo, a.Cor, a.Observacoes, "
            + "a.ID_Cliente, a.ID_Raca, a.Status, "
            + "c.Nome AS nome_cliente, c.CPF AS cpf_cliente, c.Telefone AS telefone_cliente, "
            + "r.Nome_Raca, r.Tipo_Animal "
            + "FROM animal a "
            + "JOIN cliente c ON c.ID_Cliente = a.ID_Cliente "
            + "JOIN raca r ON r.ID_Raca = a.ID_Raca";

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final RacaDAO racaDAO = new RacaDAO();

    /** Cadastra um animal (sempre ativo) e preenche o ID gerado no objeto. */
    public void inserir(Animal a) throws DAOException {
        validar(a);
        verificarClienteERaca(a);

        String sql = "INSERT INTO animal (Nome, Data_Nascimento, Sexo, Cor, Observacoes, ID_Cliente, ID_Raca, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherCampos(ps, a);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setId(rs.getInt(1));
                }
            }
            a.setStatus(true);
        } catch (SQLException e) {
            throw new DAOException("Erro ao cadastrar o animal: " + e.getMessage(), e);
        }
    }

    /** Altera os dados de um animal ativo. O ID e o status não são alterados. */
    public void alterar(Animal a) throws DAOException {
        Animal atual = buscarPorId(a.getId());
        if (atual == null) {
            throw new RegraNegocioException("Animal não encontrado (ID " + a.getId() + ").");
        }
        if (!atual.isStatus()) {
            throw new RegraNegocioException("Animal inativo não pode ser alterado. Reative-o primeiro.");
        }
        validar(a);
        verificarClienteERaca(a);

        String sql = "UPDATE animal SET Nome = ?, Data_Nascimento = ?, Sexo = ?, Cor = ?, Observacoes = ?, "
                + "ID_Cliente = ?, ID_Raca = ? WHERE ID_Animal = ?";
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencherCampos(ps, a);
            ps.setInt(8, a.getId());
            ps.executeUpdate();
            a.setStatus(true);
        } catch (SQLException e) {
            throw new DAOException("Erro ao alterar o animal: " + e.getMessage(), e);
        }
    }

    /** Exclusão lógica (UPDATE Status = FALSE). */
    public void desativar(int id) throws DAOException {
        Animal atual = buscarPorId(id);
        if (atual == null) {
            throw new RegraNegocioException("Animal não encontrado (ID " + id + ").");
        }
        if (!atual.isStatus()) {
            throw new RegraNegocioException("O animal já está inativo.");
        }
        atualizarStatus(id, false);
    }

    /** Habilita novamente um animal, desde que seu cliente e sua raça estejam ativos. */
    public void reativar(int id) throws DAOException {
        Animal atual = buscarPorId(id);
        if (atual == null) {
            throw new RegraNegocioException("Animal não encontrado (ID " + id + ").");
        }
        if (atual.isStatus()) {
            throw new RegraNegocioException("O animal já está ativo.");
        }
        verificarClienteERaca(atual);
        atualizarStatus(id, true);
    }

    /** Busca por ID, qualquer status. Retorna null se não existir. */
    public Animal buscarPorId(int id) throws DAOException {
        List<Animal> lista = consultar(SELECT_BASE + " WHERE a.ID_Animal = ?", ps -> ps.setInt(1, id));
        return lista.isEmpty() ? null : lista.get(0);
    }

    /** Lista os animais ativos (true) ou inativos (false), ordenados por nome. */
    public List<Animal> listar(boolean ativos) throws DAOException {
        return pesquisar(null, ativos);
    }

    /** Pesquisa por parte do nome do animal, da raça ou do nome do cliente. */
    public List<Animal> pesquisar(String termo, boolean ativos) throws DAOException {
        String t = termo == null ? "" : termo.trim();
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE a.Status = ?");
        if (!t.isEmpty()) {
            sql.append(" AND (a.Nome LIKE ? OR r.Nome_Raca LIKE ? OR c.Nome LIKE ?)");
        }
        sql.append(" ORDER BY a.Nome");
        return consultar(sql.toString(), ps -> {
            int i = 1;
            ps.setBoolean(i++, ativos);
            if (!t.isEmpty()) {
                String padrao = Validacao.padraoLike(t);
                ps.setString(i++, padrao);
                ps.setString(i++, padrao);
                ps.setString(i++, padrao);
            }
        });
    }

    /**
     * Consulta de animais por cliente: filtra pelo nome do cliente e/ou pelo CPF
     * (com ou sem máscara). Campos vazios são ignorados. Por padrão traz só animais
     * ativos; com incluirInativos = true traz também os inativos.
     */
    public List<Animal> consultarPorCliente(String nomeCliente, String cpf, boolean incluirInativos)
            throws DAOException {
        String nome = Validacao.limparOuNull(nomeCliente);
        String digitos = ValidadorCPF.somenteDigitos(cpf);
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1 = 1");
        if (!incluirInativos) {
            sql.append(" AND a.Status = TRUE");
        }
        if (nome != null) {
            sql.append(" AND c.Nome LIKE ?");
        }
        if (!digitos.isEmpty()) {
            sql.append(" AND REPLACE(REPLACE(c.CPF, '.', ''), '-', '') LIKE ?");
        }
        sql.append(" ORDER BY c.Nome, a.Nome");
        return consultar(sql.toString(), ps -> {
            int i = 1;
            if (nome != null) {
                ps.setString(i++, Validacao.padraoLike(nome));
            }
            if (!digitos.isEmpty()) {
                ps.setString(i++, Validacao.padraoLike(digitos));
            }
        });
    }

    /** Animais ativos de um cliente (usado no relatório geral). */
    public List<Animal> listarPorIdCliente(int idCliente) throws DAOException {
        return consultar(SELECT_BASE + " WHERE a.ID_Cliente = ? AND a.Status = TRUE ORDER BY a.Nome",
                ps -> ps.setInt(1, idCliente));
    }

    /** Animais ativos que fazem aniversário no mês informado (1 a 12). */
    public List<Animal> listarAniversariantesDoMes(int mes) throws DAOException {
        if (mes < 1 || mes > 12) {
            throw new RegraNegocioException("Mês inválido. Informe um valor de 1 a 12.");
        }
        return consultar(SELECT_BASE + " WHERE a.Status = TRUE AND MONTH(a.Data_Nascimento) = ? "
                + "ORDER BY DAY(a.Data_Nascimento), a.Nome", ps -> ps.setInt(1, mes));
    }

    /** Quantidade de animais ativos (dashboard). */
    public int contarAtivos() throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM animal WHERE Status = TRUE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DAOException("Erro ao contar animais: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ privados

    /** Valida e normaliza os campos do animal (trim, sexo em maiúscula). */
    private void validar(Animal a) throws RegraNegocioException {
        if (a == null) {
            throw new RegraNegocioException("Animal não informado.");
        }
        a.setNome(Validacao.limparOuNull(a.getNome()));
        a.setCor(Validacao.limparOuNull(a.getCor()));
        a.setObservacoes(Validacao.limparOuNull(a.getObservacoes()));

        if (a.getNome() == null) {
            throw new RegraNegocioException("O nome do animal é obrigatório.");
        }
        if (Validacao.excede(a.getNome(), 100)) {
            throw new RegraNegocioException("O nome do animal pode ter no máximo 100 caracteres.");
        }
        if (Validacao.excede(a.getCor(), 50)) {
            throw new RegraNegocioException("A cor pode ter no máximo 50 caracteres.");
        }
        if (Validacao.excede(a.getObservacoes(), 65000)) {
            throw new RegraNegocioException("As observações são muito longas.");
        }
        if (a.getSexo() != null) {
            char sexo = Character.toUpperCase(a.getSexo());
            if (sexo != 'M' && sexo != 'F') {
                throw new RegraNegocioException("Sexo inválido. Use M (macho) ou F (fêmea).");
            }
            a.setSexo(sexo);
        }
        if (a.getDataNascimento() != null && a.getDataNascimento().isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de nascimento não pode ser futura.");
        }
        if (a.getIdCliente() <= 0) {
            throw new RegraNegocioException("O animal deve estar vinculado a um cliente.");
        }
        if (a.getIdRaca() <= 0) {
            throw new RegraNegocioException("O animal deve possuir uma raça cadastrada.");
        }
    }

    /** O cliente e a raça precisam existir e estar ativos. */
    private void verificarClienteERaca(Animal a) throws DAOException {
        Cliente cliente = clienteDAO.buscarPorId(a.getIdCliente());
        if (cliente == null) {
            throw new RegraNegocioException("O cliente informado não existe (ID " + a.getIdCliente() + ").");
        }
        if (!cliente.isStatus()) {
            throw new RegraNegocioException("O cliente '" + cliente.getNome() + "' está inativo.");
        }
        Raca raca = racaDAO.buscarPorId(a.getIdRaca());
        if (raca == null) {
            throw new RegraNegocioException("A raça informada não existe (ID " + a.getIdRaca() + ").");
        }
        if (!raca.isStatus()) {
            throw new RegraNegocioException("A raça '" + raca.getNome() + "' está inativa.");
        }
    }

    private void atualizarStatus(int id, boolean ativo) throws DAOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE animal SET Status = ? WHERE ID_Animal = ?")) {
            ps.setBoolean(1, ativo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Erro ao alterar o status do animal: " + e.getMessage(), e);
        }
    }

    /** Preenche os 7 primeiros parâmetros (Nome ... ID_Raca), comuns ao INSERT e ao UPDATE. */
    private void preencherCampos(PreparedStatement ps, Animal a) throws SQLException {
        ps.setString(1, a.getNome());
        if (a.getDataNascimento() == null) {
            ps.setNull(2, Types.DATE);
        } else {
            ps.setDate(2, Date.valueOf(a.getDataNascimento()));
        }
        if (a.getSexo() == null) {
            ps.setNull(3, Types.CHAR);
        } else {
            ps.setString(3, String.valueOf(a.getSexo()));
        }
        ps.setString(4, a.getCor());
        ps.setString(5, a.getObservacoes());
        ps.setInt(6, a.getIdCliente());
        ps.setInt(7, a.getIdRaca());
    }

    @FunctionalInterface
    private interface Parametros {
        void definir(PreparedStatement ps) throws SQLException;
    }

    private List<Animal> consultar(String sql, Parametros parametros) throws DAOException {
        List<Animal> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            parametros.definir(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar animais: " + e.getMessage(), e);
        }
        return lista;
    }

    private Animal mapear(ResultSet rs) throws SQLException {
        Animal a = new Animal();
        a.setId(rs.getInt("ID_Animal"));
        a.setNome(rs.getString("Nome"));
        Date nascimento = rs.getDate("Data_Nascimento");
        a.setDataNascimento(nascimento == null ? null : nascimento.toLocalDate());
        String sexo = rs.getString("Sexo");
        a.setSexo(sexo == null || sexo.isEmpty() ? null : sexo.charAt(0));
        a.setCor(rs.getString("Cor"));
        a.setObservacoes(rs.getString("Observacoes"));
        a.setIdCliente(rs.getInt("ID_Cliente"));
        a.setIdRaca(rs.getInt("ID_Raca"));
        a.setStatus(rs.getBoolean("Status"));
        a.setNomeCliente(rs.getString("nome_cliente"));
        a.setCpfCliente(rs.getString("cpf_cliente"));
        a.setTelefoneCliente(rs.getString("telefone_cliente"));
        a.setNomeRaca(rs.getString("Nome_Raca"));
        a.setTipoAnimal(rs.getString("Tipo_Animal"));
        return a;
    }
}
