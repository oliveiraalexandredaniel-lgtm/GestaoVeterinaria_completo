package util;

import modelo.Animal;
import modelo.Cliente;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Gera os relatórios em arquivos .txt (UTF-8).
 *
 * A classe não acessa o banco: recebe os dados já consultados pelos DAOs e
 * grava o arquivo. Cada método devolve o caminho absoluto do arquivo criado.
 */
public final class GeradorRelatorioTXT {

    public static final String[] MESES = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINHA_DUPLA = "=".repeat(70);
    private static final String LINHA_SIMPLES = "-".repeat(70);

    private GeradorRelatorioTXT() {
    }

    /** Nome do mês (1 a 12). */
    public static String nomeMes(int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido: " + mes + ". Informe um valor de 1 a 12.");
        }
        return MESES[mes - 1];
    }

    /** Relatório 1: todos os clientes e seus animais (cliente sem animais aparece com aviso). */
    public static Path gerarRelatorioGeral(Map<Cliente, List<Animal>> clientesEAnimais, Path pasta)
            throws IOException {
        Path arquivo = pasta.resolve("relatorio_clientes_e_animais.txt");
        int totalAnimais = 0;
        try (BufferedWriter w = abrir(arquivo)) {
            cabecalho(w, "RELATÓRIO - TODOS OS CLIENTES E SEUS ANIMAIS");
            for (Map.Entry<Cliente, List<Animal>> e : clientesEAnimais.entrySet()) {
                Cliente c = e.getKey();
                linha(w, "Cliente: " + c.getNome());
                linha(w, "CPF: " + c.getCpf());
                if (e.getValue().isEmpty()) {
                    linha(w, "   (sem animais cadastrados)");
                }
                for (Animal a : e.getValue()) {
                    linha(w, "   - Animal: " + a.getNome()
                            + " | Raça: " + a.getNomeRaca() + " (" + a.getTipoAnimal() + ")"
                            + " | Nascimento: " + data(a.getDataNascimento()));
                    totalAnimais++;
                }
                linha(w, LINHA_SIMPLES);
            }
            linha(w, "Total de clientes: " + clientesEAnimais.size());
            linha(w, "Total de animais: " + totalAnimais);
        }
        return arquivo.toAbsolutePath();
    }

    /** Relatório 2: animais que fazem aniversário no mês informado (1 a 12). */
    public static Path gerarAniversariantesAnimais(int mes, List<Animal> animais, Path pasta)
            throws IOException {
        String nomeMes = nomeMes(mes);
        Path arquivo = pasta.resolve(String.format("relatorio_animais_aniversariantes_%02d_%s.txt",
                mes, semAcento(nomeMes).toLowerCase()));
        try (BufferedWriter w = abrir(arquivo)) {
            cabecalho(w, "RELATÓRIO - ANIMAIS ANIVERSARIANTES DE " + nomeMes.toUpperCase());
            if (animais.isEmpty()) {
                linha(w, "Nenhum animal faz aniversário neste mês.");
            }
            for (Animal a : animais) {
                linha(w, "Animal: " + a.getNome());
                linha(w, "Data de nascimento: " + data(a.getDataNascimento()));
                linha(w, "Cliente: " + a.getNomeCliente());
                linha(w, "Telefone do cliente: " + texto(a.getTelefoneCliente()));
                linha(w, LINHA_SIMPLES);
            }
            linha(w, "Total de aniversariantes: " + animais.size());
        }
        return arquivo.toAbsolutePath();
    }

    /** Relatório 3: clientes que fazem aniversário no mês informado (1 a 12). */
    public static Path gerarAniversariantesClientes(int mes, List<Cliente> clientes, Path pasta)
            throws IOException {
        String nomeMes = nomeMes(mes);
        Path arquivo = pasta.resolve(String.format("relatorio_clientes_aniversariantes_%02d_%s.txt",
                mes, semAcento(nomeMes).toLowerCase()));
        try (BufferedWriter w = abrir(arquivo)) {
            cabecalho(w, "RELATÓRIO - CLIENTES ANIVERSARIANTES DE " + nomeMes.toUpperCase());
            if (clientes.isEmpty()) {
                linha(w, "Nenhum cliente faz aniversário neste mês.");
            }
            for (Cliente c : clientes) {
                linha(w, "Nome: " + c.getNome());
                linha(w, "CPF: " + c.getCpf());
                linha(w, "Data de nascimento: " + data(c.getDataNascimento()));
                linha(w, "Telefone: " + texto(c.getTelefone()));
                linha(w, LINHA_SIMPLES);
            }
            linha(w, "Total de aniversariantes: " + clientes.size());
        }
        return arquivo.toAbsolutePath();
    }

    // ----------------------------------------------------------------- auxiliares

    private static BufferedWriter abrir(Path arquivo) throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
        return Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8);
    }

    private static void cabecalho(BufferedWriter w, String titulo) throws IOException {
        linha(w, LINHA_DUPLA);
        linha(w, titulo);
        linha(w, "Gerado em: " + LocalDateTime.now().format(DATA_HORA));
        linha(w, LINHA_DUPLA);
        linha(w, "");
    }

    private static void linha(BufferedWriter w, String texto) throws IOException {
        w.write(texto);
        w.newLine();
    }

    private static String data(LocalDate d) {
        return d == null ? "Não informada" : d.format(DATA);
    }

    private static String texto(String t) {
        return Validacao.vazio(t) ? "-" : t;
    }

    private static String semAcento(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
