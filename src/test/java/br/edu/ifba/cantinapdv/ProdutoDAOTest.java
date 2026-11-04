package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoDAOTest {

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
    }

    @Test
    void bancoNovoJaVemComProdutosIniciais() {
        assertFalse(ProdutoDAO.listarAtivos().isEmpty());
    }

    @Test
    void insereProdutoNovo() {
        assertTrue(ProdutoDAO.inserir(new Produto("Pão com ovo", "Lanche", 4.5)));

        Produto novo = BancoDeTeste.produto("Pão com ovo");
        assertNotNull(novo);
        assertEquals("Lanche", novo.getCategoria());
        assertEquals(4.5, novo.getPreco(), 0.001);
    }

    @Test
    void atualizaNomeCategoriaEPreco() {
        Produto suco = BancoDeTeste.produto("Suco Copo");
        assertNotNull(suco);

        assertTrue(ProdutoDAO.atualizar(new Produto(suco.getId(), "Suco Grande", "Bebida", 6.0, true)));

        Produto alterado = BancoDeTeste.produto("Suco Grande");
        assertNotNull(alterado);
        assertEquals(6.0, alterado.getPreco(), 0.001);
        assertNull(BancoDeTeste.produto("Suco Copo")); // o nome antigo não existe mais
    }

    @Test
    void removerSoEscondeOProdutoSemApagarDoBanco() {
        Produto suco = BancoDeTeste.produto("Suco Copo");
        int totalAntes = BancoDeTeste.contar("produtos");

        assertTrue(ProdutoDAO.desativar(suco.getId()));

        assertNull(BancoDeTeste.produto("Suco Copo"));                 // some da lista de venda
        assertEquals(totalAntes, BancoDeTeste.contar("produtos"));     // mas continua no banco
    }
}