package br.edu.ifba.cantinapdv.dao;

import br.edu.ifba.cantinapdv.model.Encomenda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EncomendaDAO {

    // Lista as encomendas de um dia (as ainda não entregues aparecem primeiro)
    public static List<Encomenda> listarPorData(LocalDate data) {
        List<Encomenda> lista = new ArrayList<>();

        String sql = "SELECT e.id_encomenda, e.nome_cliente, e.id_produto, p.nome AS nome_produto, "
                + "e.quantidade, e.valor_total, e.data_entrega, e.status_pagamento, "
                + "e.status_entrega, e.observacoes "
                + "FROM encomendas e "
                + "JOIN produtos p ON p.id_produto = e.id_produto "
                + "WHERE e.data_entrega = ? "
                + "ORDER BY CASE e.status_entrega WHEN 'Pendente' THEN 0 ELSE 1 END, e.id_encomenda";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Encomenda(
                            rs.getInt("id_encomenda"),
                            rs.getString("nome_cliente"),
                            rs.getInt("id_produto"),
                            rs.getString("nome_produto"),
                            rs.getInt("quantidade"),
                            rs.getDouble("valor_total"),
                            LocalDate.parse(rs.getString("data_entrega")),
                            rs.getString("status_pagamento"),
                            rs.getString("status_entrega"),
                            rs.getString("observacoes")));
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao listar encomendas: " + e.getMessage());
        }
        return lista;
    }

    // Cadastra a encomenda. Se metodoPagamento não for null, já registra o pagamento (cria a venda).
    public static boolean inserir(Encomenda encomenda, String metodoPagamento) {
        String sql = "INSERT INTO encomendas (nome_cliente, id_produto, quantidade, valor_total, "
                + "data_entrega, status_pagamento, observacoes, id_venda) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexaoBanco.conectar()) {
            conn.setAutoCommit(false);

            try {
                Long idVenda = null;
                String statusPagamento = "Pendente";

                if (metodoPagamento != null) {
                    idVenda = registrarVenda(conn, encomenda.getIdProduto(), encomenda.getQuantidade(),
                            encomenda.getValorTotal(), metodoPagamento);
                    statusPagamento = "Pago";
                }

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, encomenda.getNomeCliente());
                    pstmt.setInt(2, encomenda.getIdProduto());
                    pstmt.setInt(3, encomenda.getQuantidade());
                    pstmt.setDouble(4, encomenda.getValorTotal());
                    pstmt.setString(5, encomenda.getDataEntrega().toString());
                    pstmt.setString(6, statusPagamento);
                    pstmt.setString(7, encomenda.getObservacoes());
                    if (idVenda != null) {
                        pstmt.setLong(8, idVenda);
                    } else {
                        pstmt.setNull(8, Types.INTEGER);
                    }
                    pstmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            Log.erro("Erro ao salvar encomenda: " + e.getMessage());
            return false;
        }
    }

    // Registra o pagamento de uma encomenda pendente (cria a venda e liga na encomenda)
    public static boolean registrarPagamento(Encomenda encomenda, String metodoPagamento) {
        // "AND status_pagamento = 'Pendente'" impede pagar duas vezes
        String sql = "UPDATE encomendas SET status_pagamento = 'Pago', id_venda = ? "
                + "WHERE id_encomenda = ? AND status_pagamento = 'Pendente'";

        try (Connection conn = ConexaoBanco.conectar()) {
            conn.setAutoCommit(false);

            try {
                long idVenda = registrarVenda(conn, encomenda.getIdProduto(), encomenda.getQuantidade(),
                        encomenda.getValorTotal(), metodoPagamento);

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, idVenda);
                    pstmt.setInt(2, encomenda.getId());
                    if (pstmt.executeUpdate() == 0) {
                        throw new SQLException("A encomenda já estava paga ou não existe mais.");
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            Log.erro("Erro ao registrar pagamento: " + e.getMessage());
            return false;
        }
    }

    public static boolean marcarEntregue(int idEncomenda) {
        String sql = "UPDATE encomendas SET status_entrega = 'Entregue' WHERE id_encomenda = ?";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEncomenda);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            Log.erro("Erro ao marcar entrega: " + e.getMessage());
            return false;
        }
    }

    // Só cancela encomenda que ainda não foi paga
    public static boolean cancelar(int idEncomenda) {
        String sql = "DELETE FROM encomendas WHERE id_encomenda = ? AND status_pagamento = 'Pendente'";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEncomenda);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            Log.erro("Erro ao cancelar encomenda: " + e.getMessage());
            return false;
        }
    }

    // Cria a venda (e o item dela) usando a conexão/transação que já está aberta
    private static long registrarVenda(Connection conn, int idProduto, int quantidade,
                                       double valorTotal, String metodoPagamento) throws SQLException {
        long idVenda;

        String sqlVenda = "INSERT INTO vendas (valor_total, metodo_pagamento) VALUES (?, ?)";
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

        String sqlItem = "INSERT INTO itens_venda (id_venda, id_produto, quantidade, subtotal) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlItem)) {
            pstmt.setLong(1, idVenda);
            pstmt.setInt(2, idProduto);
            pstmt.setInt(3, quantidade);
            pstmt.setDouble(4, valorTotal);
            pstmt.executeUpdate();
        }

        return idVenda;
    }
}
