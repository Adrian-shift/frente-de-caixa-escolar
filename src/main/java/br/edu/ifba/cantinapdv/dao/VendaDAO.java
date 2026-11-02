package br.edu.ifba.cantinapdv.dao;

import br.edu.ifba.cantinapdv.model.Produto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VendaDAO {

    // Guarda a venda e os itens dela. Devolve true se deu tudo certo.
    public static boolean salvarVenda(double valorTotal, String metodoPagamento, List<Produto> itens) {
        String sqlVenda = "INSERT INTO vendas (valor_total, metodo_pagamento) VALUES (?, ?)";
        String sqlItem = "INSERT INTO itens_venda (id_venda, id_produto, quantidade, subtotal) VALUES (?, ?, ?, ?)";

        // Junta produtos repetidos: 3 fichas de R$ 5 viram "produto X, quantidade 3"
        Map<Integer, Produto> produtos = new LinkedHashMap<>();
        Map<Integer, Integer> quantidades = new LinkedHashMap<>();
        for (Produto p : itens) {
            produtos.put(p.getId(), p);
            quantidades.merge(p.getId(), 1, Integer::sum);
        }

        try (Connection conn = ConexaoBanco.conectar()) {
            // Desliga o salvamento automático: só salva de verdade no commit()
            conn.setAutoCommit(false);

            try {
                // 1) Salva a venda e descobre o id que o banco gerou
                long idVenda;
                try (PreparedStatement pstmt = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setDouble(1, valorTotal);
                    pstmt.setString(2, metodoPagamento);
                    pstmt.executeUpdate();

                    try (ResultSet chaves = pstmt.getGeneratedKeys()) {
                        if (!chaves.next()) {
                            throw new SQLException("Não foi possível obter o id da venda.");
                        }
                        idVenda = chaves.getLong(1);
                    }
                }

                // 2) Salva uma linha em itens_venda para cada produto
                try (PreparedStatement pstmt = conn.prepareStatement(sqlItem)) {
                    for (Map.Entry<Integer, Integer> entrada : quantidades.entrySet()) {
                        Produto produto = produtos.get(entrada.getKey());
                        int quantidade = entrada.getValue();

                        pstmt.setLong(1, idVenda);
                        pstmt.setInt(2, produto.getId());
                        pstmt.setInt(3, quantidade);
                        pstmt.setDouble(4, produto.getPreco() * quantidade);
                        pstmt.executeUpdate();
                    }
                }

                // 3) Deu tudo certo: confirma
                conn.commit();
                return true;

            } catch (SQLException e) {
                // Deu erro no meio: desfaz tudo, como se nada tivesse acontecido
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Erro ao guardar venda: " + e.getMessage());
            return false;
        }
    }
}