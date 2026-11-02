package br.edu.ifba.cantinapdv.model;

public class VendaDoDia {
    private final int id;
    private final String hora;
    private final String itens;
    private final String metodoPagamento;
    private final double valorTotal;
    private final boolean cancelada;
    private final boolean deEncomenda;
    private final String motivoCancelamento;

    public VendaDoDia(int id, String hora, String itens, String metodoPagamento, double valorTotal,
                      boolean cancelada, boolean deEncomenda, String motivoCancelamento) {
        this.id = id;
        this.hora = hora;
        this.itens = itens;
        this.metodoPagamento = metodoPagamento;
        this.valorTotal = valorTotal;
        this.cancelada = cancelada;
        this.deEncomenda = deEncomenda;
        this.motivoCancelamento = motivoCancelamento;
    }

    public int getId() { return id; }
    public String getHora() { return hora; }
    public String getItens() { return itens; }
    public String getMetodoPagamento() { return metodoPagamento; }
    public double getValorTotal() { return valorTotal; }
    public boolean isCancelada() { return cancelada; }
    public boolean isDeEncomenda() { return deEncomenda; }
    public String getMotivoCancelamento() { return motivoCancelamento; }

    // Texto da coluna "Situação" da tabela
    public String getSituacao() {
        return cancelada ? "Cancelada" : "Ativa";
    }
}