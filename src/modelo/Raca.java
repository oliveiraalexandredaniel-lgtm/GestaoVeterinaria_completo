package modelo;

/** Representa uma linha da tabela raca. */
public class Raca {

    public static final String TIPO_CACHORRO = "Cachorro";
    public static final String TIPO_GATO = "Gato";
    public static final String[] TIPOS = {TIPO_CACHORRO, TIPO_GATO};

    private int id;
    private String nome;
    private String tipoAnimal;
    private boolean status = true;

    public Raca() {
    }

    public Raca(String nome, String tipoAnimal) {
        this.nome = nome;
        this.tipoAnimal = tipoAnimal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipoAnimal() {
        return tipoAnimal;
    }

    public void setTipoAnimal(String tipoAnimal) {
        this.tipoAnimal = tipoAnimal;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    /** Usado pelos JComboBox da interface. */
    @Override
    public String toString() {
        return nome + " (" + tipoAnimal + ")";
    }
}
