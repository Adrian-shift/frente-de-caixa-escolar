package br.edu.ifba.cantinapdv.dao;

import br.edu.ifba.cantinapdv.model.Produto;
import br.edu.ifba.cantinapdv.model.VendaDoDia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
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
            Log.erro("Erro ao guardar venda", e);
            return false;
        }
    }

    // Lista as vendas de um dia (as mais recentes primeiro), incluindo as canceladas
    public static List<VendaDoDia> listarPorData(LocalDate data) {
        List<VendaDoDia> lista = new ArrayList<>();

        // 'localtime' converte a hora (guardada em UTC) para o horário do computador
        String sql = "SELECT v.id_venda, "
                + "strftime('%H:%M', v.data_hora, 'localtime') AS hora, "
                + "v.metodo_pagamento, v.valor_total, v.cancelada, v.motivo_cancelamento, "
                + "(SELECT GROUP_CONCAT(i.quantidade || 'x ' || p.nome, ', ') "
                + "   FROM itens_venda i JOIN produtos p ON p.id_produto = i.id_produto "
                + "  WHERE i.id_venda = v.id_venda) AS itens, "
                + "EXISTS (SELECT 1 FROM encomendas e WHERE e.id_venda = v.id_venda) AS de_encomenda "
                + "FROM vendas v "
                + "WHERE date(v.data_hora, 'localtime') = ? "
                + "ORDER BY v.id_venda DESC";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String itens = rs.getString("itens");
                    if (itens == null) {
                        itens = "(sem itens registrados)";
                    }

                    boolean deEncomenda = rs.getInt("de_encomenda") == 1;
                    if (deEncomenda) {
                        itens = "[Encomenda] " + itens;
                    }

                    lista.add(new VendaDoDia(
                            rs.getInt("id_venda"),
                            rs.getString("hora"),
                            itens,
                            rs.getString("metodo_pagamento"),
                            rs.getDouble("valor_total"),
                            rs.getInt("cancelada") == 1,
                            deEncomenda,
                            rs.getString("motivo_cancelamento")));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao listar vendas do dia", e);
        }
        return lista;
    }

    // Marca a venda como cancelada (nunca apaga). Não cancela venda que veio de encomenda.
    public static boolean cancelarVenda(int idVenda, String motivo) {
        String sql = "UPDATE vendas SET cancelada = 1, motivo_cancelamento = ?, "
                + "data_cancelamento = CURRENT_TIMESTAMP "
                + "WHERE id_venda = ? AND cancelada = 0 "
                + "AND NOT EXISTS (SELECT 1 FROM encomendas WHERE id_venda = ?)";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (motivo == null || motivo.trim().isEmpty()) {
                pstmt.setNull(1, Types.VARCHAR);
            } else {
                pstmt.setString(1, motivo.trim());
            }
            pstmt.setInt(2, idVenda);
            pstmt.setInt(3, idVenda);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            Log.erro("Erro ao cancelar venda", e);
            return false;
        }
    }
}