package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.ConexaoBanco;
import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.model.Produto;

public class TesteBanco {
    public static void main(String[] args) {
        System.out.println("Iniciando a criação do banco de dados...");
        ConexaoBanco.inicializarBanco();

        System.out.println("--- Produtos ativos ---");
        for (Produto p : ProdutoDAO.listarAtivos()) {
            System.out.println(p.getId() + " | " + p.getNome() + " | " + p.getCategoria() + " | R$ " + p.getPreco());
        }
    }
}
