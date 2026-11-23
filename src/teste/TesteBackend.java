package teste;

import dao.AnimalDAO;
import dao.ClienteDAO;
import dao.DAOException;
import dao.RacaDAO;
import dao.RegraNegocioException;
import factory.ConnectionFactory;
import modelo.Animal;
import modelo.Cliente;
import modelo.Raca;
import util.CalculadoraIdade;
import util.GeradorRelatorioTXT;
import util.ValidadorCPF;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Teste automatizado do backend (sem JUnit): executa cadastros, consultas, alterações,
 * erros de validação, exclusão lógica e relatórios contra o MySQL configurado.
 *
 * PRÉ-CONDIÇÃO: banco recém-criado com Script.sql (os testes de aniversariantes usam
 * os dados iniciais). Os testes gravam registros de teste: ao terminar, execute o
 * Script.sql de novo para voltar ao estado inicial.
 *
 * Saída: uma linha OK/FALHOU por verificação, resumo no final e código de saída 1 se algo falhar.
 */
public class TesteBackend {

    private static int passou = 0;
    private static int falhou = 0;

    private static final ClienteDAO clienteDAO = new ClienteDAO();
    private static final RacaDAO racaDAO = new RacaDAO();
    private static final AnimalDAO animalDAO = new AnimalDAO();
    private static final Random aleatorio = new Random();
    private static final String SUFIXO = String.valueOf(System.currentTimeMillis() % 1_000_000);

    @FunctionalInterface
    private interface Acao {
        void executar() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        if (!ConnectionFactory.testarConexao()) {
            System.out.println("ERRO: não foi possível conectar ao banco. Verifique config.properties ou DB_URL/DB_USUARIO/DB_SENHA.");
            System.exit(2);
        }
        testarValidadorCPF();
        testarCalculadoraIdade();
        testarCliente();
        testarRaca();
        testarAnimal();
        testarAniversariantes();
        testarRelatorios();
        testarExclusaoLogicaECompletude();

        System.out.println();
        System.out.println("RESUMO: " + passou + " verificações OK, " + falhou + " falharam.");
        System.exit(falhou == 0 ? 0 : 1);
    }

    // ===================================================================== utilitários

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            passou++;
            System.out.println("  OK     " + descricao);
        } else {
            falhou++;
            System.out.println("  FALHOU " + descricao);
        }
    }

    /** Verifica que a ação lança RegraNegocioException (opcionalmente com um trecho na mensagem). */
    private static void esperarRegra(String descricao, String trechoMensagem, Acao acao) {
        try {
            acao.executar();
            falhou++;
            System.out.println("  FALHOU " + descricao + " (nenhuma exceção lançada)");
        } catch (RegraNegocioException e) {
            boolean ok = trechoMensagem == null || e.getMessage().toLowerCase().contains(trechoMensagem.toLowerCase());
            if (ok) {
                passou++;
                System.out.println("  OK     " + descricao + "  -> \"" + e.getMessage() + "\"");
            } else {
                falhou++;
                System.out.println("  FALHOU " + descricao + " (mensagem inesperada: " + e.getMessage() + ")");
            }
        } catch (Exception e) {
            falhou++;
            System.out.println("  FALHOU " + descricao + " (exceção inesperada: " + e + ")");
        }
    }

    private static void grupo(String titulo) {
        System.out.println();
        System.out.println("== " + titulo);
    }

    /** Gera um CPF matematicamente válido e ainda não usado (cálculo independente do ValidadorCPF). */
    private static String gerarCpfValido() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = aleatorio.nextInt(10);
            }
            d[9] = digito(d, 9);
            d[10] = digito(d, 10);
        } while (existeCpf(d));
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    private static boolean existeCpf(int[] d) {
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        String cpf = sb.toString();
        if (cpf.chars().distinct().count() == 1) {
            return true;
        }
        try {
            return clienteDAO.buscarPorCpf(cpf) != null;
        } catch (DAOException e) {
            return true;
        }
    }

    private static int digito(int[] d, int qtd) {
        int soma = 0;
        for (int i = 0; i < qtd; i++) {
            soma += d[i] * (qtd + 1 - i);
        }
        int r = (soma * 10) % 11;
        return r == 10 ? 0 : r;
    }

    private static Cliente novoCliente(String nome) throws DAOException {
        Cliente c = new Cliente(nome + " " + SUFIXO, gerarCpfValido());
        c.setTelefone("(12) 90000-0000");
        c.setCidade("Teste");
        c.setEstado("sp");
        clienteDAO.inserir(c);
        return c;
    }

    private static Raca novaRaca(String nome, String tipo) throws DAOException {
        Raca r = new Raca(nome + " " + SUFIXO, tipo);
        racaDAO.inserir(r);
        return r;
    }

    private static Animal novoAnimal(String nome, Cliente c, Raca r, LocalDate nascimento, Character sexo) throws DAOException {
        Animal a = new Animal();
        a.setNome(nome + " " + SUFIXO);
        a.setIdCliente(c.getId());
        a.setIdRaca(r.getId());
        a.setDataNascimento(nascimento);
        a.setSexo(sexo);
        animalDAO.inserir(a);
        return a;
    }

    private static boolean contemCliente(List<Cliente> lista, int id) {
        return lista.stream().anyMatch(c -> c.getId() == id);
    }

    private static boolean contemRaca(List<Raca> lista, int id) {
        return lista.stream().anyMatch(r -> r.getId() == id);
    }

    private static boolean contemAnimal(List<Animal> lista, int id) {
        return lista.stream().anyMatch(a -> a.getId() == id);
    }

    private static int contarLinhas(String tabela) throws Exception {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM " + tabela);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ===================================================================== grupos

    private static void testarValidadorCPF() {
        grupo("ValidadorCPF");
        verificar("CPF válido com máscara (123.456.789-09)", ValidadorCPF.isValido("123.456.789-09"));
        verificar("CPF válido sem máscara (12345678909)", ValidadorCPF.isValido("12345678909"));
        verificar("CPF válido 529.982.247-25", ValidadorCPF.isValido("529.982.247-25"));
        verificar("rejeita dígito verificador errado (123.456.789-00)", !ValidadorCPF.isValido("123.456.789-00"));
        verificar("rejeita sequência repetida (111.111.111-11)", !ValidadorCPF.isValido("111.111.111-11"));
        verificar("rejeita tamanho errado (123)", !ValidadorCPF.isValido("123"));
        verificar("rejeita texto vazio", !ValidadorCPF.isValido(""));
        verificar("rejeita null", !ValidadorCPF.isValido(null));
        verificar("rejeita letras (123.456.789-0a)", !ValidadorCPF.isValido("123.456.789-0a"));
        verificar("formatar 12345678909 -> 123.456.789-09", "123.456.789-09".equals(ValidadorCPF.formatar("12345678909")));
    }

    private static void testarCalculadoraIdade() {
        grupo("CalculadoraIdade (referência fixa: 19/11/2026)");
        LocalDate ref = LocalDate.of(2026, 11, 19);
        verificar("15/03/2020 -> 6 anos e 8 meses",
                "6 anos e 8 meses".equals(CalculadoraIdade.formatar(LocalDate.of(2020, 3, 15), ref)));
        verificar("19/11/2025 -> 1 ano", "1 ano".equals(CalculadoraIdade.formatar(LocalDate.of(2025, 11, 19), ref)));
        verificar("19/10/2026 -> 1 mês", "1 mês".equals(CalculadoraIdade.formatar(LocalDate.of(2026, 10, 19), ref)));
        verificar("19/06/2026 -> 5 meses", "5 meses".equals(CalculadoraIdade.formatar(LocalDate.of(2026, 6, 19), ref)));
        verificar("mesma data -> Menos de 1 mês", "Menos de 1 mês".equals(CalculadoraIdade.formatar(ref, ref)));
        verificar("data nula -> Não informada", "Não informada".equals(CalculadoraIdade.formatar(null, ref)));
        verificar("data futura -> Data inválida", "Data inválida".equals(CalculadoraIdade.formatar(LocalDate.of(2027, 1, 1), ref)));
        verificar("calcular(null) devolve null", CalculadoraIdade.calcular(null, ref) == null);
    }

    private static void testarCliente() throws Exception {
        grupo("Cliente: cadastro, validações, alteração, exclusão lógica");

        Cliente c = new Cliente("  Cliente Teste " + SUFIXO + "  ", ValidadorCPF.formatar(gerarCpfValido()).replace(".", "").replace("-", ""));
        c.setEstado("sp");
        c.setDataNascimento(LocalDate.of(1991, 5, 20));
        clienteDAO.inserir(c);
        verificar("inserir gera ID > 0", c.getId() > 0);
        verificar("status inicial é ativo", c.isStatus());
        verificar("data_ultima_alteracao inicia nula", c.getDataUltimaAlteracao() == null);

        Cliente lido = clienteDAO.buscarPorId(c.getId());
        verificar("nome gravado sem espaços nas pontas", lido.getNome().equals("Cliente Teste " + SUFIXO));
        verificar("CPF sem máscara é gravado formatado", lido.getCpf().matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}"));
        verificar("UF gravada em maiúsculas", "SP".equals(lido.getEstado()));
        verificar("data de nascimento gravada corretamente", LocalDate.of(1991, 5, 20).equals(lido.getDataNascimento()));
        verificar("buscarPorCpf encontra com máscara", clienteDAO.buscarPorCpf(lido.getCpf()) != null);
        verificar("buscarPorCpf encontra sem máscara", clienteDAO.buscarPorCpf(ValidadorCPF.somenteDigitos(lido.getCpf())) != null);

        esperarRegra("nome obrigatório", "nome", () -> clienteDAO.inserir(new Cliente("   ", gerarCpfValido())));
        esperarRegra("CPF obrigatório", "obrigatório", () -> clienteDAO.inserir(new Cliente("Sem CPF", null)));
        esperarRegra("CPF inválido (dígito errado)", "inválido", () -> clienteDAO.inserir(new Cliente("CPF Ruim", "123.456.789-00")));
        esperarRegra("CPF inválido (sequência repetida)", "inválido", () -> clienteDAO.inserir(new Cliente("CPF Ruim", "111.111.111-11")));
        esperarRegra("CPF duplicado (mesmo formato)", "já existe", () -> clienteDAO.inserir(new Cliente("Duplicado", lido.getCpf())));
        esperarRegra("CPF duplicado (sem máscara)", "já existe",
                () -> clienteDAO.inserir(new Cliente("Duplicado", ValidadorCPF.somenteDigitos(lido.getCpf()))));
        esperarRegra("UF inválida", "UF", () -> {
            Cliente x = new Cliente("UF Ruim", gerarCpfValido());
            x.setEstado("SAO");
            clienteDAO.inserir(x);
        });
        esperarRegra("data de nascimento futura", "futura", () -> {
            Cliente x = new Cliente("Futuro", gerarCpfValido());
            x.setDataNascimento(LocalDate.now().plusDays(1));
            clienteDAO.inserir(x);
        });
        esperarRegra("nome com mais de 100 caracteres", "100", () -> clienteDAO.inserir(new Cliente("N".repeat(101), gerarCpfValido())));

        // alteração
        int idOriginal = lido.getId();
        lido.setNome("Cliente Alterado " + SUFIXO);
        lido.setTelefone("(12) 98888-7777");
        clienteDAO.alterar(lido);
        Cliente depois = clienteDAO.buscarPorId(idOriginal);
        verificar("alterar mantém o mesmo ID", depois.getId() == idOriginal);
        verificar("alterar grava o novo nome", depois.getNome().equals("Cliente Alterado " + SUFIXO));
        verificar("alterar grava o novo telefone", "(12) 98888-7777".equals(depois.getTelefone()));
        verificar("alterar preenche data_ultima_alteracao", depois.getDataUltimaAlteracao() != null);

        Cliente outro = novoCliente("Outro Cliente");
        esperarRegra("alterar para CPF de outro cliente", "já existe", () -> {
            Cliente x = clienteDAO.buscarPorId(idOriginal);
            x.setCpf(outro.getCpf());
            clienteDAO.alterar(x);
        });
        esperarRegra("alterar com CPF inválido", "inválido", () -> {
            Cliente x = clienteDAO.buscarPorId(idOriginal);
            x.setCpf("123.456.789-99");
            clienteDAO.alterar(x);
        });
        esperarRegra("alterar sem nome", "nome", () -> {
            Cliente x = clienteDAO.buscarPorId(idOriginal);
            x.setNome("");
            clienteDAO.alterar(x);
        });
        verificar("alterar mantendo o próprio CPF é permitido", executa(() -> {
            Cliente x = clienteDAO.buscarPorId(idOriginal);
            clienteDAO.alterar(x);
        }));
        esperarRegra("alterar cliente inexistente", "não encontrado", () -> {
            Cliente x = new Cliente("Fantasma", gerarCpfValido());
            x.setId(99999999);
            clienteDAO.alterar(x);
        });

        // pesquisa
        verificar("pesquisar por parte do nome", contemCliente(clienteDAO.pesquisar("Alterado " + SUFIXO, true), idOriginal));
        verificar("pesquisar por nome ignora maiúsculas", contemCliente(clienteDAO.pesquisar("alterado " + SUFIXO.toLowerCase(), true), idOriginal));
        verificar("pesquisar por CPF formatado", contemCliente(clienteDAO.pesquisar(depois.getCpf(), true), idOriginal));
        verificar("pesquisar por CPF só com dígitos", contemCliente(clienteDAO.pesquisar(ValidadorCPF.somenteDigitos(depois.getCpf()), true), idOriginal));
        verificar("pesquisar por texto inexistente devolve lista vazia", clienteDAO.pesquisar("zzzxxxyyy" + SUFIXO, true).isEmpty());
        verificar("pesquisa com aspas/SQL injection não quebra nem devolve tudo", clienteDAO.pesquisar("' OR '1'='1", true).isEmpty());
        verificar("curinga % é tratado como texto literal", clienteDAO.pesquisar("%", true).isEmpty());
        verificar("listar(true) contém o cliente", contemCliente(clienteDAO.listar(true), idOriginal));

        // exclusão lógica
        clienteDAO.desativar(idOriginal);
        verificar("desativado não aparece em listar(true)", !contemCliente(clienteDAO.listar(true), idOriginal));
        verificar("desativado não aparece na pesquisa padrão", !contemCliente(clienteDAO.pesquisar("Alterado " + SUFIXO, true), idOriginal));
        verificar("desativado aparece em listar(false)", contemCliente(clienteDAO.listar(false), idOriginal));
        verificar("registro continua no banco com status FALSE", clienteDAO.buscarPorId(idOriginal) != null && !clienteDAO.buscarPorId(idOriginal).isStatus());
        esperarRegra("desativar de novo", "já está inativo", () -> clienteDAO.desativar(idOriginal));
        esperarRegra("alterar cliente inativo", "inativo", () -> clienteDAO.alterar(clienteDAO.buscarPorId(idOriginal)));
        esperarRegra("cadastrar CPF de cliente inativo orienta a reativar", "reative", () -> clienteDAO.inserir(new Cliente("Repetido", depois.getCpf())));
        clienteDAO.reativar(idOriginal);
        verificar("reativar volta a listar(true)", contemCliente(clienteDAO.listar(true), idOriginal));
        esperarRegra("reativar cliente já ativo", "já está ativo", () -> clienteDAO.reativar(idOriginal));
        esperarRegra("desativar cliente inexistente", "não encontrado", () -> clienteDAO.desativar(99999999));
    }

    private static void testarRaca() throws Exception {
        grupo("Raça: cadastro, duplicidade, alteração, exclusão lógica");

        Raca r = new Raca("  Poodle Teste " + SUFIXO + " ", "cachorro");
        racaDAO.inserir(r);
        verificar("inserir gera ID > 0", r.getId() > 0);
        verificar("tipo normalizado para 'Cachorro'", "Cachorro".equals(racaDAO.buscarPorId(r.getId()).getTipoAnimal()));
        verificar("status inicial ativo", racaDAO.buscarPorId(r.getId()).isStatus());

        esperarRegra("raça duplicada para o mesmo tipo", "já existe", () -> racaDAO.inserir(new Raca("Poodle Teste " + SUFIXO, "Cachorro")));
        esperarRegra("duplicada ignorando maiúsculas", "já existe", () -> racaDAO.inserir(new Raca("POODLE TESTE " + SUFIXO, "Cachorro")));
        esperarRegra("raça seed duplicada (Poodle/Cachorro)", "já existe", () -> racaDAO.inserir(new Raca("Poodle", "Cachorro")));
        Raca gato = new Raca("Poodle Teste " + SUFIXO, "Gato");
        racaDAO.inserir(gato);
        verificar("mesmo nome para outro tipo é permitido (Poodle/Gato)", gato.getId() > 0);
        esperarRegra("tipo inválido", "inválido", () -> racaDAO.inserir(new Raca("Raca X", "Pássaro")));
        esperarRegra("tipo obrigatório", "obrigatório", () -> racaDAO.inserir(new Raca("Raca X", "")));
        esperarRegra("nome obrigatório", "obrigatório", () -> racaDAO.inserir(new Raca(" ", "Gato")));

        int id = r.getId();
        Raca lida = racaDAO.buscarPorId(id);
        lida.setNome("Poodle Alterado " + SUFIXO);
        racaDAO.alterar(lida);
        verificar("alterar mantém o ID e grava o nome", racaDAO.buscarPorId(id).getNome().equals("Poodle Alterado " + SUFIXO));
        esperarRegra("alterar criando duplicidade", "já existe", () -> {
            Raca x = racaDAO.buscarPorId(id);
            x.setTipoAnimal("Gato");
            x.setNome("Poodle Teste " + SUFIXO);
            racaDAO.alterar(x);
        });
        verificar("alterar mantendo o próprio nome é permitido", executa(() -> racaDAO.alterar(racaDAO.buscarPorId(id))));

        verificar("pesquisar por nome", contemRaca(racaDAO.pesquisar("Alterado " + SUFIXO, true), id));
        verificar("pesquisar por tipo", contemRaca(racaDAO.pesquisar("Cachorro", true), id));
        verificar("listar(true) contém a raça", contemRaca(racaDAO.listar(true), id));

        racaDAO.desativar(id);
        verificar("desativada não aparece em listar(true)", !contemRaca(racaDAO.listar(true), id));
        verificar("desativada aparece em listar(false)", contemRaca(racaDAO.listar(false), id));
        esperarRegra("cadastrar raça igual a uma inativa orienta a reativar", "reative",
                () -> racaDAO.inserir(new Raca("Poodle Alterado " + SUFIXO, "Cachorro")));
        esperarRegra("alterar raça inativa", "inativa", () -> racaDAO.alterar(racaDAO.buscarPorId(id)));
        racaDAO.reativar(id);
        verificar("reativar volta a listar(true)", contemRaca(racaDAO.listar(true), id));
        esperarRegra("desativar raça inexistente", "não encontrada", () -> racaDAO.desativar(99999999));
    }

    private static void testarAnimal() throws Exception {
        grupo("Animal: cadastro, vínculos, consulta por cliente, alteração, exclusão lógica");

        Cliente dono = novoCliente("Dono Animal");
        Raca raca = novaRaca("Raca Animal", "Cachorro");
        Raca racaGato = novaRaca("Raca Gato", "Gato");

        Animal a = novoAnimal("Rex", dono, raca, LocalDate.of(2022, 1, 15), 'm');
        verificar("inserir gera ID > 0", a.getId() > 0);
        Animal lido = animalDAO.buscarPorId(a.getId());
        verificar("sexo 'm' gravado como 'M'", lido.getSexo() != null && lido.getSexo() == 'M');
        verificar("status inicial ativo", lido.isStatus());
        verificar("JOIN traz nome do cliente", lido.getNomeCliente().equals(dono.getNome()));
        verificar("JOIN traz raça e tipo", lido.getNomeRaca().equals(raca.getNome()) && "Cachorro".equals(lido.getTipoAnimal()));
        verificar("JOIN traz telefone do cliente", "(12) 90000-0000".equals(lido.getTelefoneCliente()));

        Animal semDados = novoAnimal("Sem Dados", dono, racaGato, null, null);
        Animal lidoSemDados = animalDAO.buscarPorId(semDados.getId());
        verificar("animal sem data de nascimento e sem sexo é aceito", lidoSemDados.getDataNascimento() == null && lidoSemDados.getSexo() == null);
        verificar("idade de animal sem data = 'Não informada'", "Não informada".equals(CalculadoraIdade.formatar(lidoSemDados.getDataNascimento())));

        esperarRegra("nome obrigatório", "nome", () -> {
            Animal x = new Animal();
            x.setNome(" ");
            x.setIdCliente(dono.getId());
            x.setIdRaca(raca.getId());
            animalDAO.inserir(x);
        });
        esperarRegra("sexo inválido", "sexo", () -> novoAnimal("Sexo Ruim", dono, raca, null, 'X'));
        esperarRegra("sem cliente (ID 0)", "cliente", () -> {
            Animal x = new Animal();
            x.setNome("Sem Dono");
            x.setIdRaca(raca.getId());
            animalDAO.inserir(x);
        });
        esperarRegra("sem raça (ID 0)", "raça", () -> {
            Animal x = new Animal();
            x.setNome("Sem Raca");
            x.setIdCliente(dono.getId());
            animalDAO.inserir(x);
        });
        esperarRegra("cliente inexistente", "não existe", () -> {
            Animal x = new Animal();
            x.setNome("Orfao");
            x.setIdCliente(99999999);
            x.setIdRaca(raca.getId());
            animalDAO.inserir(x);
        });
        esperarRegra("raça inexistente", "não existe", () -> {
            Animal x = new Animal();
            x.setNome("Sem Raca");
            x.setIdCliente(dono.getId());
            x.setIdRaca(99999999);
            animalDAO.inserir(x);
        });
        esperarRegra("data de nascimento futura", "futura", () -> novoAnimal("Futuro", dono, raca, LocalDate.now().plusDays(2), 'F'));

        Cliente clienteInativo = novoCliente("Cliente Inativo");
        clienteDAO.desativar(clienteInativo.getId());
        esperarRegra("cliente inativo não pode ter animal novo", "inativo", () -> novoAnimal("Pet", clienteInativo, raca, null, 'F'));
        Raca racaInativa = novaRaca("Raca Inativa", "Gato");
        racaDAO.desativar(racaInativa.getId());
        esperarRegra("raça inativa não pode ser usada", "inativa", () -> novoAnimal("Pet", dono, racaInativa, null, 'F'));

        // consulta por cliente
        List<Animal> porNome = animalDAO.consultarPorCliente(dono.getNome(), "", false);
        verificar("consulta por nome do cliente encontra os 2 animais", porNome.size() == 2 && contemAnimal(porNome, a.getId()) && contemAnimal(porNome, semDados.getId()));
        verificar("consulta por parte do nome do cliente", contemAnimal(animalDAO.consultarPorCliente("Dono Animal " + SUFIXO.substring(0, 2), null, false), a.getId())
                || animalDAO.consultarPorCliente("Dono Animal " + SUFIXO, null, false).size() == 2);
        verificar("consulta por CPF com máscara", animalDAO.consultarPorCliente(null, dono.getCpf(), false).size() == 2);
        verificar("consulta por CPF só dígitos", animalDAO.consultarPorCliente("", ValidadorCPF.somenteDigitos(dono.getCpf()), false).size() == 2);
        verificar("consulta por nome + CPF combinados", animalDAO.consultarPorCliente(dono.getNome(), dono.getCpf(), false).size() == 2);
        verificar("nome e CPF de pessoas diferentes não retornam nada",
                animalDAO.consultarPorCliente(dono.getNome(), clienteInativo.getCpf(), false).isEmpty());
        verificar("consulta por cliente inexistente devolve lista vazia", animalDAO.consultarPorCliente("zzz" + SUFIXO, "", false).isEmpty());
        verificar("idade calculada de Rex (nascido em 15/01/2022) tem anos", CalculadoraIdade.formatar(lido.getDataNascimento()).contains("anos"));

        // alteração
        int idAnimal = a.getId();
        Animal alt = animalDAO.buscarPorId(idAnimal);
        alt.setNome("Rex Alterado " + SUFIXO);
        alt.setCor("Preto");
        alt.setObservacoes("Observação com acentuação: coração, ação, pingüim");
        alt.setIdRaca(racaGato.getId());
        animalDAO.alterar(alt);
        Animal depois = animalDAO.buscarPorId(idAnimal);
        verificar("alterar mantém o ID", depois.getId() == idAnimal);
        verificar("alterar grava nome, cor e raça", depois.getNome().equals("Rex Alterado " + SUFIXO) && "Preto".equals(depois.getCor()) && depois.getIdRaca() == racaGato.getId());
        verificar("observações com acentos são gravadas corretamente (UTF-8)", "Observação com acentuação: coração, ação, pingüim".equals(depois.getObservacoes()));
        esperarRegra("alterar para raça inativa", "inativa", () -> {
            Animal x = animalDAO.buscarPorId(idAnimal);
            x.setIdRaca(racaInativa.getId());
            animalDAO.alterar(x);
        });
        esperarRegra("alterar para cliente inativo", "inativo", () -> {
            Animal x = animalDAO.buscarPorId(idAnimal);
            x.setIdCliente(clienteInativo.getId());
            animalDAO.alterar(x);
        });
        esperarRegra("alterar animal inexistente", "não encontrado", () -> {
            Animal x = new Animal();
            x.setId(99999999);
            x.setNome("Fantasma");
            x.setIdCliente(dono.getId());
            x.setIdRaca(raca.getId());
            animalDAO.alterar(x);
        });

        // pesquisa
        verificar("pesquisar por nome do animal", contemAnimal(animalDAO.pesquisar("Rex Alterado " + SUFIXO, true), idAnimal));
        verificar("pesquisar por nome do cliente", contemAnimal(animalDAO.pesquisar(dono.getNome(), true), idAnimal));
        verificar("pesquisar por raça", contemAnimal(animalDAO.pesquisar(racaGato.getNome(), true), idAnimal));
        verificar("listarPorIdCliente traz os 2 animais ativos", animalDAO.listarPorIdCliente(dono.getId()).size() == 2);

        // regras de bloqueio
        esperarRegra("desativar cliente com animais ativos", "animal", () -> clienteDAO.desativar(dono.getId()));
        esperarRegra("desativar raça com animais ativos", "animal", () -> racaDAO.desativar(racaGato.getId()));
        esperarRegra("mudar tipo de raça que tem animais", "tipo", () -> {
            Raca x = racaDAO.buscarPorId(racaGato.getId());
            x.setTipoAnimal("Cachorro");
            racaDAO.alterar(x);
        });

        // exclusão lógica
        animalDAO.desativar(idAnimal);
        verificar("animal desativado não aparece em listar(true)", !contemAnimal(animalDAO.listar(true), idAnimal));
        verificar("animal desativado não aparece na pesquisa padrão", !contemAnimal(animalDAO.pesquisar("Rex Alterado " + SUFIXO, true), idAnimal));
        verificar("animal desativado não aparece na consulta por cliente (padrão)", !contemAnimal(animalDAO.consultarPorCliente(dono.getNome(), "", false), idAnimal));
        verificar("consulta por cliente com incluirInativos mostra o desativado",
                contemAnimal(animalDAO.consultarPorCliente(dono.getNome(), "", true), idAnimal));
        verificar("animal desativado aparece em listar(false)", contemAnimal(animalDAO.listar(false), idAnimal));
        esperarRegra("desativar animal já inativo", "já está inativo", () -> animalDAO.desativar(idAnimal));
        esperarRegra("alterar animal inativo", "inativo", () -> animalDAO.alterar(animalDAO.buscarPorId(idAnimal)));
        animalDAO.reativar(idAnimal);
        verificar("reativar animal volta a listar(true)", contemAnimal(animalDAO.listar(true), idAnimal));

        // reativar animal cujo cliente foi desativado depois
        Cliente dono2 = novoCliente("Dono Dois");
        Animal b = novoAnimal("Pet Dois", dono2, raca, null, 'F');
        animalDAO.desativar(b.getId());
        clienteDAO.desativar(dono2.getId());
        esperarRegra("reativar animal de cliente inativo", "inativo", () -> animalDAO.reativar(b.getId()));
        esperarRegra("desativar animal inexistente", "não encontrado", () -> animalDAO.desativar(99999999));
    }

    private static void testarAniversariantes() throws Exception {
        grupo("Aniversariantes por mês");

        Cliente c = new Cliente("Aniversariante Dez " + SUFIXO, gerarCpfValido());
        c.setDataNascimento(LocalDate.of(1990, 12, 24));
        c.setTelefone("(12) 97777-0000");
        clienteDAO.inserir(c);
        Raca r = novaRaca("Raca Aniv", "Cachorro");
        Animal a = novoAnimal("Pet Dez", c, r, LocalDate.of(2021, 12, 5), 'F');

        List<Cliente> cDez = clienteDAO.listarAniversariantesDoMes(12);
        List<Animal> aDez = animalDAO.listarAniversariantesDoMes(12);
        verificar("cliente nascido em dezembro aparece no mês 12", contemCliente(cDez, c.getId()));
        verificar("animal nascido em dezembro aparece no mês 12", contemAnimal(aDez, a.getId()));
        verificar("mês 12 só traz clientes nascidos em dezembro", cDez.stream().allMatch(x -> x.getDataNascimento().getMonthValue() == 12));
        verificar("mês 12 só traz animais nascidos em dezembro", aDez.stream().allMatch(x -> x.getDataNascimento().getMonthValue() == 12));
        verificar("lista de animais do mês traz telefone do cliente", aDez.stream().filter(x -> x.getId() == a.getId()).allMatch(x -> "(12) 97777-0000".equals(x.getTelefoneCliente())));
        verificar("animal aparece em dezembro só uma vez", aDez.stream().filter(x -> x.getId() == a.getId()).count() == 1);

        List<Cliente> cMar = clienteDAO.listarAniversariantesDoMes(3);
        List<Animal> aMar = animalDAO.listarAniversariantesDoMes(3);
        verificar("dados iniciais: Ana Souza (12/03) aparece em março", cMar.stream().anyMatch(x -> x.getNome().equals("Ana Souza")));
        verificar("dados iniciais: Diego Ferreira (30/03) aparece em março", cMar.stream().anyMatch(x -> x.getNome().equals("Diego Ferreira")));
        verificar("dados iniciais: Thor (15/03) e Simba (30/03) aparecem em março",
                aMar.stream().anyMatch(x -> x.getNome().equals("Thor")) && aMar.stream().anyMatch(x -> x.getNome().equals("Simba")));
        verificar("março ordenado por dia (Ana 12 antes de Diego 30)",
                indice(cMar, "Ana Souza") >= 0 && indice(cMar, "Ana Souza") < indice(cMar, "Diego Ferreira"));
        verificar("animal sem data de nascimento (Max) não aparece em nenhum mês",
                java.util.stream.IntStream.rangeClosed(1, 12).boxed().noneMatch(m -> {
                    try {
                        return animalDAO.listarAniversariantesDoMes(m).stream().anyMatch(x -> x.getNome().equals("Max"));
                    } catch (DAOException e) {
                        return true;
                    }
                }));

        esperarRegra("mês 0 inválido (clientes)", "mês", () -> clienteDAO.listarAniversariantesDoMes(0));
        esperarRegra("mês 13 inválido (clientes)", "mês", () -> clienteDAO.listarAniversariantesDoMes(13));
        esperarRegra("mês 0 inválido (animais)", "mês", () -> animalDAO.listarAniversariantesDoMes(0));
        esperarRegra("mês 13 inválido (animais)", "mês", () -> animalDAO.listarAniversariantesDoMes(13));

        animalDAO.desativar(a.getId());
        verificar("animal desativado some dos aniversariantes", !contemAnimal(animalDAO.listarAniversariantesDoMes(12), a.getId()));
        clienteDAO.desativar(c.getId());
        verificar("cliente desativado some dos aniversariantes", !contemCliente(clienteDAO.listarAniversariantesDoMes(12), c.getId()));
    }

    private static int indice(List<Cliente> lista, String nome) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getNome().equals(nome)) {
                return i;
            }
        }
        return -1;
    }

    private static void testarRelatorios() throws Exception {
        grupo("Relatórios TXT (UTF-8)");

        Path pasta = Files.createTempDirectory("relatorios_teste");
        Cliente semAnimais = novoCliente("Cliente Sem Animais");

        Map<Cliente, List<Animal>> mapa = new LinkedHashMap<>();
        for (Cliente c : clienteDAO.listar(true)) {
            mapa.put(c, animalDAO.listarPorIdCliente(c.getId()));
        }
        Path geral = GeradorRelatorioTXT.gerarRelatorioGeral(mapa, pasta);
        verificar("arquivo do relatório geral foi criado", Files.exists(geral));
        verificar("caminho devolvido é absoluto", geral.isAbsolute());
        String t1 = Files.readString(geral, StandardCharsets.UTF_8);
        verificar("geral: tem título", t1.contains("TODOS OS CLIENTES E SEUS ANIMAIS"));
        verificar("geral: tem cliente, CPF, animal, raça e nascimento",
                t1.contains("Cliente: Ana Souza") && t1.contains("CPF: 123.456.789-09")
                        && t1.contains("Animal: Thor | Raça: Labrador (Cachorro) | Nascimento: 15/03/2020"));
        verificar("geral: acentos preservados em UTF-8 (Siamês)", t1.contains("Siamês"));
        verificar("geral: animal sem data mostra 'Não informada'", t1.contains("Animal: Max | Raça: Vira-lata (Cachorro) | Nascimento: Não informada"));
        verificar("geral: cliente sem animais é listado com aviso", t1.contains("Cliente: " + semAnimais.getNome()) && t1.contains("(sem animais cadastrados)"));
        verificar("geral: rodapé com totais", t1.contains("Total de clientes: " + mapa.size()));
        verificar("geral: não lista cliente inativo", !t1.contains("Cliente Inativo " + SUFIXO));
        byte[] bytes = Files.readAllBytes(geral);
        verificar("arquivo não começa com BOM", !(bytes.length > 2 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB));

        Path animaisMar = GeradorRelatorioTXT.gerarAniversariantesAnimais(3, animalDAO.listarAniversariantesDoMes(3), pasta);
        String t2 = Files.readString(animaisMar, StandardCharsets.UTF_8);
        verificar("animais: nome do arquivo inclui mês (03_marco)", animaisMar.getFileName().toString().equals("relatorio_animais_aniversariantes_03_marco.txt"));
        verificar("animais: título com o mês (MARÇO)", t2.contains("ANIVERSARIANTES DE MARÇO"));
        verificar("animais: traz animal, cliente, telefone e data",
                t2.contains("Animal: Thor") && t2.contains("Data de nascimento: 15/03/2020")
                        && t2.contains("Cliente: Ana Souza") && t2.contains("Telefone do cliente: (12) 99111-1111"));

        Path clientesMar = GeradorRelatorioTXT.gerarAniversariantesClientes(3, clienteDAO.listarAniversariantesDoMes(3), pasta);
        String t3 = Files.readString(clientesMar, StandardCharsets.UTF_8);
        verificar("clientes: traz nome, CPF, data e telefone",
                t3.contains("Nome: Ana Souza") && t3.contains("CPF: 123.456.789-09")
                        && t3.contains("Data de nascimento: 12/03/1985") && t3.contains("Telefone: (12) 99111-1111"));

        Path vazio = GeradorRelatorioTXT.gerarAniversariantesClientes(2, List.of(), pasta);
        verificar("mês sem aniversariantes gera arquivo com aviso", Files.readString(vazio, StandardCharsets.UTF_8).contains("Nenhum cliente faz aniversário neste mês."));

        Path subpasta = pasta.resolve("nova/subpasta");
        verificar("cria a pasta de destino se não existir", Files.exists(GeradorRelatorioTXT.gerarAniversariantesAnimais(1, List.of(), subpasta)));

        try {
            GeradorRelatorioTXT.nomeMes(13);
            verificar("nomeMes(13) deve lançar exceção", false);
        } catch (IllegalArgumentException e) {
            verificar("nomeMes(13) lança IllegalArgumentException", true);
        }
        verificar("nomeMes(1) = Janeiro e nomeMes(12) = Dezembro", "Janeiro".equals(GeradorRelatorioTXT.nomeMes(1)) && "Dezembro".equals(GeradorRelatorioTXT.nomeMes(12)));
        System.out.println("  (arquivos de teste gravados em " + pasta + ")");
    }

    private static void testarExclusaoLogicaECompletude() throws Exception {
        grupo("Exclusão lógica (sem DELETE) e contadores do dashboard");

        verificar("contarAtivos de clientes = tamanho de listar(true)", clienteDAO.contarAtivos() == clienteDAO.listar(true).size());
        verificar("contarAtivas de raças = tamanho de listar(true)", racaDAO.contarAtivas() == racaDAO.listar(true).size());
        verificar("contarAtivos de animais = tamanho de listar(true)", animalDAO.contarAtivos() == animalDAO.listar(true).size());
        verificar("clientes no banco = ativos + inativos (nenhum apagado fisicamente)",
                contarLinhas("cliente") == clienteDAO.listar(true).size() + clienteDAO.listar(false).size());
        verificar("raças no banco = ativas + inativas", contarLinhas("raca") == racaDAO.listar(true).size() + racaDAO.listar(false).size());
        verificar("animais no banco = ativos + inativos", contarLinhas("animal") == animalDAO.listar(true).size() + animalDAO.listar(false).size());

        Path src = Path.of("src");
        if (Files.isDirectory(src)) {
            String proibido = "delete" + " from";
            try (Stream<Path> arquivos = Files.walk(src)) {
                long ocorrencias = arquivos.filter(p -> p.toString().endsWith(".java")).filter(p -> {
                    try {
                        return Files.readString(p, StandardCharsets.UTF_8).toLowerCase().contains(proibido);
                    } catch (Exception e) {
                        return true;
                    }
                }).count();
                verificar("nenhum arquivo do código-fonte usa o comando de remoção física", ocorrencias == 0);
            }
        } else {
            System.out.println("  (pasta src/ não encontrada a partir de " + Path.of("").toAbsolutePath() + ": verificação de código pulada)");
        }
    }

    /** Executa a ação e devolve true se ela terminou sem exceção. */
    private static boolean executa(Acao acao) {
        try {
            acao.executar();
            return true;
        } catch (Exception e) {
            System.out.println("    (exceção: " + e + ")");
            return false;
        }
    }
}
