package br.edu.ifba.cantinapdv.dao;

import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoBanco {
    // Nome do arquivo do banco, criado na pasta do projeto
    private static final String URL = "jdbc:sqlite:cantina.db";

    // Abre uma conexão. Quem chama é responsável por tratar o erro.
    public static Connection conectar() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        // Faz o SQLite respeitar as ligações entre tabelas (chaves estrangeiras)
        config.enforceForeignKeys(true);
        return DriverManager.getConnection(URL, config.toProperties());
    }

    // Cria as tabelas (se ainda não existirem) e os produtos iniciais
    public static void inicializarBanco() {
        String[] comandos = {
                """
            CREATE TABLE IF NOT EXISTS produtos (
                id_produto INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                categoria TEXT NOT NULL,
                preco REAL NOT NULL,
                ativo INTEGER NOT NULL DEFAULT 1
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS vendas (
                id_venda INTEGER PRIMARY KEY AUTOINCREMENT,
                data_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
                valor_total REAL NOT NULL,
                metodo_pagamento TEXT NOT NULL
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS itens_venda (
                id_item_venda INTEGER PRIMARY KEY AUTOINCREMENT,
                id_venda INTEGER NOT NULL,
                id_produto INTEGER NOT NULL,
                quantidade INTEGER NOT NULL,
                subtotal REAL NOT NULL,
                FOREIGN KEY (id_venda) REFERENCES vendas(id_venda),
                FOREIGN KEY (id_produto) REFERENCES produtos(id_produto)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS encomendas (
                id_encomenda INTEGER PRIMARY KEY AUTOINCREMENT,
                nome_cliente TEXT NOT NULL,
                id_produto INTEGER NOT NULL,
                data_pedido DATETIME DEFAULT CURRENT_TIMESTAMP,
                status_pagamento TEXT DEFAULT 'Pendente',
                status_entrega TEXT DEFAULT 'Pendente',
                observacoes TEXT,
                FOREIGN KEY (id_produto) REFERENCES produtos(id_produto)
            )
            """
        };

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            // Executa um comando de cada vez (mais seguro no SQLite)
            for (String sql : comandos) {
                stmt.execute(sql);
            }
            cadastrarProdutosIniciais(conn);
            System.out.println("Banco de dados pronto!");
        } catch (SQLException e) {
            System.err.println("Erro ao criar tabelas: " + e.getMessage());
        }
    }

    // Só cadastra os produtos iniciais se a tabela estiver vazia
    private static void cadastrarProdutosIniciais(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM produtos")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String sql = "INSERT INTO produtos (nome, categoria, preco) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Object[][] iniciais = {
                    {"Ficha R$ 3,00", "Ficha", 3.00},
                    {"Ficha R$ 5,00", "Ficha", 5.00},
                    {"Ficha R$ 7,00", "Ficha", 7.00},
                    {"Ficha R$ 10,00", "Ficha", 10.00},
                    {"Café / Chá", "Bebida", 2.00},
                    {"Suco Copo", "Bebida", 3.00}
            };
            for (Object[] p : iniciais) {
                pstmt.setString(1, (String) p[0]);
                pstmt.setString(2, (String) p[1]);
                pstmt.setDouble(3, (Double) p[2]);
                pstmt.executeUpdate();
            }
            System.out.println("Produtos iniciais cadastrados.");
        }
    }
}