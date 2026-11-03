package br.edu.ifba.cantinapdv.dao;

import br.edu.ifba.cantinapdv.model.CaixaDia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

public class CaixaDiaDAO {

    // Devolve a conferência salva para o dia, ou null se ainda não existe nenhuma
    public static CaixaDia buscar(LocalDate data) {
        String sql = "SELECT troco_inicial, dinheiro_contado FROM caixa_dia WHERE data = ?";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double troco = rs.getDouble("troco_inicial");
                    double contado = rs.getDouble("dinheiro_contado");
                    Double contadoOuNulo = rs.wasNull() ? null : contado;
                    return new CaixaDia(troco, contadoOuNulo);
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao buscar a conferência do caixa", e);
        }
        return null;
    }

    // Cria a conferência do dia ou atualiza, se já existir
    public static boolean salvar(LocalDate data, double trocoInicial, Double dinheiroContado) {
        String sql = "INSERT INTO caixa_dia (data, troco_inicial, dinheiro_contado) VALUES (?, ?, ?) "
                + "ON CONFLICT(data) DO UPDATE SET "
                + "troco_inicial = excluded.troco_inicial, dinheiro_contado = excluded.dinheiro_contado";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.toString());
            pstmt.setDouble(2, trocoInicial);
            if (dinheiroContado == null) {
                pstmt.setNull(3, Types.REAL);
            } else {
                pstmt.setDouble(3, dinheiroContado);
            }
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            Log.erro("Erro ao salvar a conferência do caixa", e);
            return false;
        }
    }
}
