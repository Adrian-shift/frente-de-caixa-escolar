package br.edu.ifba.cantinapdv.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfiguracaoDAO {

    // Devolve o valor guardado para a chave, ou null se não existir
    public static String obter(String chave) {
        String sql = "SELECT valor FROM configuracoes WHERE chave = ?";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, chave);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valor");
                }
            }
        } catch (SQLException e) {
            Log.erro("Erro ao ler configuração", e);
        }
        return null;
    }

    // Cria a configuração ou atualiza, se já existir
    public static boolean salvar(String chave, String valor) {
        String sql = "INSERT INTO configuracoes (chave, valor) VALUES (?, ?) "
                + "ON CONFLICT(chave) DO UPDATE SET valor = excluded.valor";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, chave);
            pstmt.setString(2, valor);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            Log.erro("Erro ao salvar configuração", e);
            return false;
        }
    }

    public static boolean remover(String chave) {
        String sql = "DELETE FROM configuracoes WHERE chave = ?";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, chave);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            Log.erro("Erro ao remover configuração", e);
            return false;
        }
    }
}