package br.edu.ifba.cantinapdv.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoBanco {
    // Define o nome do arquivo do banco de dados que será criado na pasta do projeto
    private static final String URL = "jdbc:sqlite:cantina.db";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Erro ao conectar ao banco: " + e.getMessage());
            return null;
        }
    }

    // Método para criar as tabelas caso o arquivo seja novo
    public static void inicializarBanco() {
        String sql = """
            CREATE TABLE IF NOT EXISTS produtos (
                id_produto INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                categoria TEXT NOT NULL,
                preco REAL NOT NULL
            );
            
            CREATE TABLE IF NOT EXISTS vendas (
                id_venda INTEGER PRIMARY KEY AUTOINCREMENT,
                data_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
                valor_total REAL NOT NULL,
                metodo_pagamento TEXT NOT NULL 
            );
            
            CREATE TABLE IF NOT EXISTS itens_venda (
                id_item_venda INTEGER PRIMARY KEY AUTOINCREMENT,
                id_venda INTEGER NOT NULL,
                id_produto INTEGER NOT NULL,
                quantidade INTEGER NOT NULL,
                subtotal REAL NOT NULL,
                FOREIGN KEY (id_venda) REFERENCES vendas(id_venda),
                FOREIGN KEY (id_produto) REFERENCES produtos(id_produto)
            );
            
            CREATE TABLE IF NOT EXISTS encomendas (
                id_encomenda INTEGER PRIMARY KEY AUTOINCREMENT,
                nome_cliente TEXT NOT NULL,
                id_produto INTEGER NOT NULL,
                data_pedido DATETIME DEFAULT CURRENT_TIMESTAMP,
                status_pagamento TEXT DEFAULT 'Pendente',
                status_entrega TEXT DEFAULT 'Pendente',
                observacoes TEXT,
                FOREIGN KEY (id_produto) REFERENCES produtos(id_produto)
            );
            """;

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Tabelas do banco de dados criadas/verificadas com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao criar tabelas: " + e.getMessage());
        }
    }
}