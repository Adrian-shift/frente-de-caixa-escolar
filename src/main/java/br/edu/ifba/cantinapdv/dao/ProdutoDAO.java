package br.edu.ifba.cantinapdv.dao;

import br.edu.ifba.cantinapdv.model.Produto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    // Devolve só os produtos que NÃO foram removidos
    public static List<Produto> listarAtivos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT id_produto, nome, categoria, preco, ativo "
                + "FROM produtos WHERE ativo = 1 ORDER BY id_produto";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                lista.add(new Produto(
                        rs.getInt("id_produto"),
                        rs.getString("nome"),
                        rs.getString("categoria"),
                        rs.getDouble("preco"),
                        rs.getInt("ativo") == 1));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar produtos: " + e.getMessage());
        }
        return lista;
    }

    // Adiciona um produto novo
    public static boolean inserir(Produto produto) {
        String sql = "INSERT INTO produtos (nome, categoria, preco) VALUES (?, ?, ?)";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, produto.getNome());
            pstmt.setString(2, produto.getCategoria());
            pstmt.setDouble(3, produto.getPreco());
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao inserir produto: " + e.getMessage());
            return false;
        }
    }

    // Altera nome, categoria e preço de um produto existente
    public static boolean atualizar(Produto produto) {
        String sql = "UPDATE produtos SET nome = ?, categoria = ?, preco = ? WHERE id_produto = ?";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, produto.getNome());
            pstmt.setString(2, produto.getCategoria());
            pstmt.setDouble(3, produto.getPreco());
            pstmt.setInt(4, produto.getId());
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar produto: " + e.getMessage());
            return false;
        }
    }

    // "Remove" o produto: só esconde (ativo = 0), sem apagar do banco
    public static boolean desativar(int idProduto) {
        String sql = "UPDATE produtos SET ativo = 0 WHERE id_produto = ?";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idProduto);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao remover produto: " + e.getMessage());
            return false;
        }
    }
}
