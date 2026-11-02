package br.edu.ifba.cantinapdv.model;

import java.time.LocalDate;

public class Encomenda {
    private final int id;
    private final String nomeCliente;
    private final int idProduto;
    private final String nomeProduto;
    private final int quantidade;
    private final double valorTotal;
    private final LocalDate dataEntrega;
    private final String statusPagamento;
    private final String statusEntrega;
    private final String observacoes;

    // Usado ao ler do banco
    public Encomenda(int id, String nomeCliente, int idProduto, String nomeProduto, int quantidade,
                     double valorTotal, LocalDate dataEntrega, String statusPagamento,
                     String statusEntrega, String observacoes) {
        this.id = id;
        this.nomeCliente = nomeCliente;
        this.idProduto = idProduto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.valorTotal = valorTotal;
        this.dataEntrega = dataEntrega;
        this.statusPagamento = statusPagamento;
        this.statusEntrega = statusEntrega;
        this.observacoes = observacoes;
    }

    // Usado ao criar uma encomenda nova (ainda sem id)
    public Encomenda(String nomeCliente, int idProduto, int quantidade, double valorTotal,
                     LocalDate dataEntrega, String observacoes) {
        this(0, nomeCliente, idProduto, "", quantidade, valorTotal, dataEntrega, "Pendente", "Pendente", observacoes);
    }

    public int getId() { return id; }
    public String getNomeCliente() { return nomeCliente; }
    public int getIdProduto() { return idProduto; }
    public String getNomeProduto() { return nomeProduto; }
    public int getQuantidade() { return quantidade; }
    public double getValorTotal() { return valorTotal; }
    public LocalDate getDataEntrega() { return dataEntrega; }
    public String getStatusPagamento() { return statusPagamento; }
    public String getStatusEntrega() { return statusEntrega; }
    public String getObservacoes() { return observacoes; }
}
