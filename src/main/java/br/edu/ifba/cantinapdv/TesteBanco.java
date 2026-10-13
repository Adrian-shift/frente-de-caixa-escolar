package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.ConexaoBanco;

public class TesteBanco {
    public static void main(String[] args) {
        System.out.println("Iniciando a criação do banco de dados...");

        // Executa a inicialização que cria o arquivo cantina.db e as 4 tabelas
        ConexaoBanco.inicializarBanco();
    }
}
