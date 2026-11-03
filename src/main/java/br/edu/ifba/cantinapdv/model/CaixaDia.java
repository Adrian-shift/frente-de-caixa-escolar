package br.edu.ifba.cantinapdv.model;

public class CaixaDia {
    private final double trocoInicial;
    private final Double dinheiroContado; // null = ainda não foi contado

    public CaixaDia(double trocoInicial, Double dinheiroContado) {
        this.trocoInicial = trocoInicial;
        this.dinheiroContado = dinheiroContado;
    }

    public double getTrocoInicial() { return trocoInicial; }
    public Double getDinheiroContado() { return dinheiroContado; }
}