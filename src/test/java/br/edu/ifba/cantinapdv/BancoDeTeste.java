package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.Caminhos;
import br.edu.ifba.cantinapdv.dao.ConexaoBanco;
import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import br.edu.ifba.cantinapdv.model.Produto;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

// Ferramentas usadas por todos os testes: prepara um banco de TESTE limpo e ajuda a montar situações.
public class BancoDeTeste {

    // Ao carregar esta classe, manda o sistema usar uma pasta de TESTE (e nunca a pasta real dos dados)
    static {
        System.setProperty("cantina.pasta", Paths.get("target", "dados-teste").toAbsolutePath().toString());
    }

    // Deixa o banco de teste limpo, só com os produtos iniciais. Chame no início de cada teste.
    public static void preparar() {
        // TRAVA DE SEGURANÇA: nunca apaga nada se não for a pasta de teste
        Path pasta = Caminhos.PASTA_DADOS.toAbsolutePath().normalize();
        if (!pasta.endsWith("dados-teste")) {
            throw new IllegalStateException("RECUSADO: os testes não estão usando a pasta de teste ("
                    + pasta + "). Nada foi apagado.");
        }

        ConexaoBanco.inicializarBanco(); // garante que as tabelas existem

        try (Connection conn = ConexaoBanco.conectar(); Statement stmt = conn.createStatement()) {
            // Ordem importa: primeiro as tabelas que dependem das outras
            stmt.execute("DELETE FROM itens_venda");
            stmt.execute("DELETE FROM encomendas");
            stmt.execute("DELETE FROM vendas");
            stmt.execute("DELETE FROM caixa_dia");
            stmt.execute("DELETE FROM configuracoes");
            stmt.execute("DELETE FROM produtos");
            stmt.execute("DELETE FROM sqlite_sequence"); // os números (ids) voltam a começar do 1
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível limpar o banco de teste", e);
        }

        ConexaoBanco.inicializarBanco(); // recria os produtos iniciais
    }

    // Quantas linhas a tabela tem
    public static int contar(String tabela) {
        try (Connection conn = ConexaoBanco.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // Procura um produto ATIVO pelo nome (null se não existir)
    public static Produto produto(String nome) {
        for (Produto p : ProdutoDAO.listarAtivos()) {
            if (p.getNome().equals(nome)) {
                return p;
            }
        }
        return null;
    }

    // Grava uma venda em um dia específico (ao meio-dia UTC, para o dia não mudar com o fuso horário)
    public static long inserirVenda(LocalDate dia, double valor, String metodo, boolean cancelada) {
        String sql = "INSERT INTO vendas (data_hora, valor_total, metodo_pagamento, cancelada) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, dia + " 12:00:00");
            pstmt.setDouble(2, valor);
            pstmt.setString(3, metodo);
            pstmt.setInt(4, cancelada ? 1 : 0);
            pstmt.executeUpdate();

            try (ResultSet chaves = pstmt.getGeneratedKeys()) {
                chaves.next();
                return chaves.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // Grava um item (produto + quantidade) em uma venda
    public static void inserirItem(long idVenda, Produto produto, int quantidade) {
        String sql = "INSERT INTO itens_venda (id_venda, id_produto, quantidade, subtotal) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoBanco.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, idVenda);
            pstmt.setInt(2, produto.getId());
            pstmt.setInt(3, quantidade);
            pstmt.setDouble(4, produto.getPreco() * quantidade);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // Acha a linha de um resumo pelo nome (por exemplo, "PIX")
    public static LinhaResumo linha(List<LinhaResumo> linhas, String descricao) {
        for (LinhaResumo l : linhas) {
            if (l.getDescricao().equals(descricao)) {
                return l;
            }
        }
        return null;
    }
}