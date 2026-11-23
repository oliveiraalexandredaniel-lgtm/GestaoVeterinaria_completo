package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 * Cores, fontes e fábricas de componentes com a aparência do sistema.
 * Concentrar o estilo aqui mantém as telas simples e consistentes.
 */
public final class Tema {

    public static final Color PRIMARIA = new Color(0x0E7C7B);
    public static final Color PRIMARIA_HOVER = new Color(0x0B6362);
    public static final Color MENU_FUNDO = new Color(0x12343B);
    public static final Color MENU_HOVER = new Color(0x1B4A54);
    public static final Color MENU_SELECIONADO = new Color(0x1F5C68);
    public static final Color DESTAQUE = new Color(0xF2B134);
    public static final Color FUNDO = new Color(0xF3F6F7);
    public static final Color CARTAO = Color.WHITE;
    public static final Color BORDA = new Color(0xD3DDE1);
    public static final Color TEXTO = new Color(0x22313A);
    public static final Color TEXTO_SUAVE = new Color(0x667882);
    public static final Color PERIGO = new Color(0xC0392B);
    public static final Color PERIGO_HOVER = new Color(0xA03024);
    public static final Color CAMPO_BLOQUEADO = new Color(0xEBF0F2);
    private static final Color CABECALHO_TABELA = new Color(0xE6EEF0);
    private static final Color LINHA_ZEBRA = new Color(0xF7FAFB);
    private static final Color SELECAO = new Color(0xCDE7E6);
    private static final Color BOTAO_DESABILITADO = new Color(0xE3E8EA);

    public static final Font FONTE = new Font(Font.SANS_SERIF, Font.PLAIN, 30);
    public static final Font FONTE_NEGRITO = new Font(Font.SANS_SERIF, Font.BOLD, 30);

    private Tema() {
    }

    /** Aplica a fonte padrão a todos os componentes Swing (deve ser chamado antes de criar telas). */
    public static void instalarFontePadrao() {
        FontUIResource fonte = new FontUIResource(FONTE);
        UIManager.put("defaultFont", fonte);
        Enumeration<Object> chaves = UIManager.getDefaults().keys();
        while (chaves.hasMoreElements()) {
            Object chave = chaves.nextElement();
            if (chave.toString().endsWith(".font")) {
                UIManager.put(chave, fonte);
            }
        }
    }

    // ------------------------------------------------------------------ textos

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(FONTE);
        l.setForeground(TEXTO_SUAVE);
        return l;
    }

    public static JLabel secao(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        l.setForeground(TEXTO);
        return l;
    }

    public static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        l.setForeground(TEXTO_SUAVE);
        return l;
    }

    // ------------------------------------------------------------------ campos e painéis

    public static JTextField campo() {
        JTextField c = new JTextField(8);
        estilizarCampo(c);
        return c;
    }

    public static void estilizarCampo(JComponent c) {
        c.setFont(FONTE);
        c.setBackground(Color.WHITE);
        c.setForeground(TEXTO);
        c.setBorder(bordaCampo());
    }

    public static Border bordaCampo() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    /** Painel branco com borda e margem interna, usado como "cartão". */
    public static JPanel cartao() {
        JPanel p = new JPanel();
        p.setBackground(CARTAO);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return p;
    }

    /** Painel transparente (cor de fundo da aplicação). */
    public static JPanel fundo() {
        JPanel p = new JPanel();
        p.setBackground(FUNDO);
        return p;
    }

    /**
     * Adiciona "rótulo + componente" em um painel GridBagLayout. Cada campo ocupa duas
     * linhas da grade (rótulo em cima, campo embaixo).
     */
    public static void adicionarCampo(JPanel painel, String rotulo, JComponent componente,
                                      int coluna, int linha, int largura) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = coluna;
        g.gridy = linha * 2;
        g.gridwidth = largura;
        g.weightx = largura;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(linha == 0 ? 0 : 12, coluna == 0 ? 0 : 14, 3, 0);
        painel.add(rotulo(rotulo), g);
        g.gridy = linha * 2 + 1;
        g.insets = new Insets(0, coluna == 0 ? 0 : 14, 0, 0);
        painel.add(componente, g);
    }

    // ------------------------------------------------------------------ tabela

    /** Cria uma JTable somente leitura, com linhas zebradas, cabeçalho estilizado e ordenação por coluna. */
    public static JTable tabela(TableModel modelo) {
        JTable t = new JTable(modelo);
        t.setFont(FONTE);
        t.setForeground(TEXTO);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.setSelectionBackground(SELECAO);
        t.setSelectionForeground(TEXTO);

        DefaultTableCellRenderer celula = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tabela, Object valor,
                    boolean selecionada, boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(tabela, valor, selecionada, false, linha, coluna);
                setHorizontalAlignment(LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setForeground(TEXTO);
                setBackground(selecionada ? SELECAO : (linha % 2 == 0 ? Color.WHITE : LINHA_ZEBRA));
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, celula);
        t.setDefaultRenderer(Number.class, celula);
        t.setDefaultRenderer(Integer.class, celula);

        DefaultTableCellRenderer cabecalho = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tabela, Object valor,
                    boolean selecionada, boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(tabela, valor, false, false, linha, coluna);
                setFont(FONTE_NEGRITO);
                setForeground(TEXTO);
                setBackground(CABECALHO_TABELA);
                setHorizontalAlignment(LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
                return this;
            }
        };
        t.getTableHeader().setDefaultRenderer(cabecalho);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 32));
        return t;
    }

    /** Define a largura preferida de cada coluna (a primeira, normalmente o ID, fica estreita). */
    public static void larguras(JTable tabela, int... larguras) {
        for (int i = 0; i < larguras.length && i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
    }

    /** Coloca a tabela em um JScrollPane com borda leve. */
    public static JScrollPane rolagem(JComponent conteudo) {
        JScrollPane s = new JScrollPane(conteudo);
        s.setBorder(BorderFactory.createLineBorder(BORDA, 1));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    // ------------------------------------------------------------------ botões

    public static JButton botaoPrimario(String texto) {
        return new BotaoTema(texto, PRIMARIA, PRIMARIA_HOVER, Color.WHITE, null);
    }

    public static JButton botaoSecundario(String texto) {
        return new BotaoTema(texto, Color.WHITE, new Color(0xEEF3F4), TEXTO, BORDA);
    }

    public static JButton botaoPerigo(String texto) {
        return new BotaoTema(texto, PERIGO, PERIGO_HOVER, Color.WHITE, null);
    }

    /**
     * Botão desenhado manualmente (cantos arredondados, cor de hover, estado desabilitado),
     * para ter o mesmo visual em qualquer Look and Feel (FlatLaf, Nimbus ou Metal).
     */
    private static final class BotaoTema extends JButton {
        private static final long serialVersionUID = 1L;

        private final Color fundo;
        private final Color fundoHover;
        private final Color borda;

        BotaoTema(String texto, Color fundo, Color fundoHover, Color corTexto, Color borda) {
            super(texto);
            this.fundo = fundo;
            this.fundoHover = fundoHover;
            this.borda = borda;
            setForeground(corTexto);
            setFont(FONTE_NEGRITO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(Math.max(96, fm.stringWidth(getText()) + 36), 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean ativo = isEnabled();
            Color cor = !ativo ? BOTAO_DESABILITADO
                    : getModel().isPressed() ? fundoHover.darker()
                    : getModel().isRollover() ? fundoHover : fundo;
            g2.setColor(cor);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            if (borda != null) {
                g2.setColor(ativo ? borda : BOTAO_DESABILITADO);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(getText())) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(ativo ? getForeground() : new Color(0x9AA8AF));
            g2.drawString(getText(), x, y);
            if (isFocusOwner() && ativo) {
                g2.setColor(new Color(0, 0, 0, 70));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
            }
            g2.dispose();
        }
    }

    /** Aba em forma de botão: texto com faixa colorida embaixo quando selecionada. */
    public static final class BotaoAba extends JButton {
        private static final long serialVersionUID = 1L;

        private boolean selecionado;

        public BotaoAba(String texto) {
            super(texto);
            setFont(FONTE_NEGRITO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void setSelecionado(boolean selecionado) {
            this.selecionado = selecionado;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth(getText()) + 44, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (selecionado) {
                g2.setColor(PRIMARIA);
                g2.fillRect(0, h - 3, w, 3);
            } else if (getModel().isRollover()) {
                g2.setColor(BORDA);
                g2.fillRect(0, h - 3, w, 3);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(selecionado ? PRIMARIA : TEXTO_SUAVE);
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - 3 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Botão do menu lateral: fundo escuro, texto à esquerda e faixa de destaque quando selecionado. */
    public static final class BotaoMenu extends JButton {
        private static final long serialVersionUID = 1L;

        private boolean selecionado;

        public BotaoMenu(String texto) {
            super(texto);
            setForeground(Color.WHITE);
            setFont(FONTE_NEGRITO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setHorizontalAlignment(LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        public void setSelecionado(boolean selecionado) {
            this.selecionado = selecionado;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(220, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (selecionado) {
                g2.setColor(MENU_SELECIONADO);
                g2.fillRect(0, 0, w, h);
                g2.setColor(DESTAQUE);
                g2.fillRect(0, 0, 5, h);
            } else if (getModel().isRollover()) {
                g2.setColor(MENU_HOVER);
                g2.fillRect(0, 0, w, h);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(selecionado ? Color.WHITE : new Color(0xC9D9DD));
            g2.drawString(getText(), 26, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
