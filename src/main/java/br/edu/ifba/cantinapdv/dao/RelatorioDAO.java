package br.edu.ifba.cantinapdv.dao;

import br.edu.ifba.cantinapdv.model.LinhaResumo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RelatorioDAO {

    // Total vendido no dia, separado por forma de pagamento (sem as vendas canceladas).
    // Sempre devolve as 3 formas, mesmo que alguma tenha zero vendas.
    public static List<LinhaResumo> resumoPorPagamento(LocalDate data) {
        Map<String, LinhaResumo> mapa = new LinkedHashMap<>();
        for (String metodo : new String[]{"Dinheiro", "Cartão", "PIX"}) {
            mapa.put(metodo, new LinhaResumo(metodo, 0, 0.0));
        }

        // 'localtime' converte a hora (que o SQLite guarda em UTC) para o horário do computador
        String sql = "SELECT metodo_pagamento, COUNT(*) AS qtd, SUM(valor_total) AS total "
                + "FROM vendas "
                + "WHERE date(data_hora, 'localtime') = ? AND cancelada = 0 "
                + "GROUP BY metodo_pagamento";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString()); // formato 2026-10-20

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("metodo_pagamento");
                    mapa.put(metodo, new LinhaResumo(metodo, rs.getInt("qtd"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar resumo por pagamento", e);
        }
        return new ArrayList<>(mapa.values());
    }

    // O que foi vendido no dia (sem as vendas canceladas): produto, quantidade e total
    public static List<LinhaResumo> resumoPorProduto(LocalDate data) {
        List<LinhaResumo> lista = new ArrayList<>();

        String sql = "SELECT p.nome, SUM(i.quantidade) AS qtd, SUM(i.subtotal) AS total "
                + "FROM itens_venda i "
                + "JOIN vendas v ON v.id_venda = i.id_venda "
                + "JOIN produtos p ON p.id_produto = i.id_produto "
                + "WHERE date(v.data_hora, 'localtime') = ? AND v.cancelada = 0 "
                + "GROUP BY p.id_produto, p.nome "
                + "ORDER BY qtd DESC";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new LinhaResumo(rs.getString("nome"), rs.getInt("qtd"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar resumo por produto", e);
        }
        return lista;
    }

    // Quantas vendas foram canceladas no dia e quanto valiam (só para aviso no Fechamento)
    public static LinhaResumo resumoCancelamentos(LocalDate data) {
        String sql = "SELECT COUNT(*) AS qtd, COALESCE(SUM(valor_total), 0) AS total "
                + "FROM vendas "
                + "WHERE date(data_hora, 'localtime') = ? AND cancelada = 1";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new LinhaResumo("Canceladas", rs.getInt("qtd"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar resumo de cancelamentos", e);
        }
        return new LinhaResumo("Canceladas", 0, 0.0);
    }
}