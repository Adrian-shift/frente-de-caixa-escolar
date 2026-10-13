package br.edu.ifba.cantinapdv.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VendaDAO {
    // Método que guarda a venda física no ficheiro cantina.db
    public static boolean salvarVenda(double valorTotal, String metodoPagamento) {
        String sql = "INSERT INTO vendas (valor_total, metodo_pagamento) VALUES (?, ?)";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, valorTotal);
            pstmt.setString(2, metodoPagamento);

            pstmt.executeUpdate();
            return true; // Retorna verdadeiro se guardou com sucesso

        } catch (SQLException e) {
            System.err.println("Erro ao guardar venda: " + e.getMessage());
            return false;
        }
    }
}
