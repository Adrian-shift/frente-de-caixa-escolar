package br.edu.ifba.cantinapdv.model;

public class LinhaResumo {
    private final String descricao;
    private final int quantidade;
    private final double total;

    public LinhaResumo(String descricao, int quantidade, double total) {
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.total = total;
    }

    public String getDescricao() { return descricao; }
    public int getQuantidade() { return quantidade; }
    public double getTotal() { return total; }
}