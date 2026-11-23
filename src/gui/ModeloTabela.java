package gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Modelo de tabela genérico, somente leitura: recebe os nomes das colunas e uma
 * função que transforma cada objeto (Cliente, Animal, ...) em uma linha de valores.
 */
public class ModeloTabela<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final String[] colunas;
    private final transient Function<T, Object[]> extrator;
    private transient List<T> itens = new ArrayList<>();
    private Class<?>[] classes;

    public ModeloTabela(String[] colunas, Function<T, Object[]> extrator) {
        this.colunas = colunas;
        this.extrator = extrator;
        this.classes = new Class<?>[colunas.length];
        java.util.Arrays.fill(classes, String.class);
    }

    /** Substitui todas as linhas e redesenha a tabela. */
    public void setItens(List<T> novos) {
        this.itens = new ArrayList<>(novos);
        // A classe da coluna (ex.: Integer para o ID) permite ordenar numericamente.
        java.util.Arrays.fill(classes, String.class);
        if (!itens.isEmpty()) {
            Object[] primeira = extrator.apply(itens.get(0));
            for (int i = 0; i < primeira.length; i++) {
                if (primeira[i] != null) {
                    classes[i] = primeira[i].getClass();
                }
            }
        }
        fireTableDataChanged();
    }

    /** Objeto da linha (índice do MODELO, não da tela). */
    public T getItem(int linhaDoModelo) {
        return itens.get(linhaDoModelo);
    }

    @Override
    public int getRowCount() {
        return itens.size();
    }

    @Override
    public int getColumnCount() {
        return colunas.length;
    }

    @Override
    public String getColumnName(int coluna) {
        return colunas[coluna];
    }

    @Override
    public Class<?> getColumnClass(int coluna) {
        return classes[coluna];
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
        return extrator.apply(itens.get(linha))[coluna];
    }
}
