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

public class RelatorioPeriodoDAO {

    // Vendas válidas (não canceladas) do período: quantidade e total
    public static LinhaResumo resumoGeral(LocalDate de, LocalDate ate) {
        String sql = "SELECT COUNT(*) AS qtd, COALESCE(SUM(valor_total), 0) AS total "
                + "FROM vendas "
                + "WHERE date(data_hora, 'localtime') BETWEEN ? AND ? AND cancelada = 0";
        return consultarResumo("Vendas", sql, de, ate);
    }

    // Vendas canceladas do período: quantidade e valor
    public static LinhaResumo resumoCancelamentos(LocalDate de, LocalDate ate) {
        String sql = "SELECT COUNT(*) AS qtd, COALESCE(SUM(valor_total), 0) AS total "
                + "FROM vendas "
                + "WHERE date(data_hora, 'localtime') BETWEEN ? AND ? AND cancelada = 1";
        return consultarResumo("Canceladas", sql, de, ate);
    }

    private static LinhaResumo consultarResumo(String descricao, String sql, LocalDate de, LocalDate ate) {
        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, de.toString());
            pstmt.setString(2, ate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new LinhaResumo(descricao, rs.getInt("qtd"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar o resumo do período", e);
        }
        return new LinhaResumo(descricao, 0, 0.0);
    }

    // Total vendido em cada dia do período. Dias sem venda aparecem com 0.
    public static Map<LocalDate, Double> vendasPorDia(LocalDate de, LocalDate ate) {
        Map<LocalDate, Double> mapa = new LinkedHashMap<>();
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
            mapa.put(dia, 0.0);
        }

        String sql = "SELECT date(data_hora, 'localtime') AS dia, SUM(valor_total) AS total "
                + "FROM vendas "
                + "WHERE date(data_hora, 'localtime') BETWEEN ? AND ? AND cancelada = 0 "
                + "GROUP BY dia";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, de.toString());
            pstmt.setString(2, ate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mapa.put(LocalDate.parse(rs.getString("dia")), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar as vendas por dia", e);
        }
        return mapa;
    }

    // Total do período por forma de pagamento. Sempre devolve as 3 formas.
    public static List<LinhaResumo> resumoPorPagamento(LocalDate de, LocalDate ate) {
        Map<String, LinhaResumo> mapa = new LinkedHashMap<>();
        for (String metodo : new String[]{"Dinheiro", "Cartão", "PIX"}) {
            mapa.put(metodo, new LinhaResumo(metodo, 0, 0.0));
        }

        String sql = "SELECT metodo_pagamento, COUNT(*) AS qtd, SUM(valor_total) AS total "
                + "FROM vendas "
                + "WHERE date(data_hora, 'localtime') BETWEEN ? AND ? AND cancelada = 0 "
                + "GROUP BY metodo_pagamento";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, de.toString());
            pstmt.setString(2, ate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("metodo_pagamento");
                    mapa.put(metodo, new LinhaResumo(metodo, rs.getInt("qtd"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar o resumo por pagamento do período", e);
        }
        return new ArrayList<>(mapa.values());
    }

    // Produtos do período, do mais vendido para o menos vendido
    public static List<LinhaResumo> produtosMaisVendidos(LocalDate de, LocalDate ate) {
        List<LinhaResumo> lista = new ArrayList<>();

        String sql = "SELECT p.nome, SUM(i.quantidade) AS qtd, SUM(i.subtotal) AS total "
                + "FROM itens_venda i "
                + "JOIN vendas v ON v.id_venda = i.id_venda "
                + "JOIN produtos p ON p.id_produto = i.id_produto "
                + "WHERE date(v.data_hora, 'localtime') BETWEEN ? AND ? AND v.cancelada = 0 "
                + "GROUP BY p.id_produto, p.nome "
                + "ORDER BY qtd DESC, total DESC";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, de.toString());
            pstmt.setString(2, ate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new LinhaResumo(rs.getString("nome"), rs.getInt("qtd"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao gerar os produtos mais vendidos", e);
        }
        return lista;
    }
}