package br.edu.ifba.cantinapdv.model;

public class Produto {
    private int id;
    private String nome;
    private String categoria;
    private double preco;
    private boolean ativo;

    public Produto(int id, String nome, String categoria, double preco, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.ativo = ativo;
    }

    // Construtor usado ao criar um produto novo (ainda sem id, o banco gera)
    public Produto(String nome, String categoria, double preco) {
        this(0, nome, categoria, preco, true);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}