package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.RelatorioDAO;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import br.edu.ifba.cantinapdv.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioDAOTest {

    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
    }

    @Test
    void semVendasDevolveAsTresFormasZeradas() {
        List<LinhaResumo> resumo = RelatorioDAO.resumoPorPagamento(DIA);

        assertEquals(3, resumo.size());
        assertEquals("Dinheiro", resumo.get(0).getDescricao());
        assertEquals("Cartão", resumo.get(1).getDescricao());
        assertEquals("PIX", resumo.get(2).getDescricao());
        for (LinhaResumo linha : resumo) {
            assertEquals(0, linha.getQuantidade());
            assertEquals(0.0, linha.getTotal(), 0.001);
        }
    }

    @Test
    void somaCadaFormaDePagamento() {
        BancoDeTeste.inserirVenda(DIA, 8.0, "Dinheiro", false);
        BancoDeTeste.inserirVenda(DIA, 5.0, "Dinheiro", false);
        BancoDeTeste.inserirVenda(DIA, 10.0, "PIX", false);

        List<LinhaResumo> resumo = RelatorioDAO.resumoPorPagamento(DIA);

        assertEquals(2, BancoDeTeste.linha(resumo, "Dinheiro").getQuantidade());
        assertEquals(13.0, BancoDeTeste.linha(resumo, "Dinheiro").getTotal(), 0.001);
        assertEquals(1, BancoDeTeste.linha(resumo, "PIX").getQuantidade());
        assertEquals(10.0, BancoDeTeste.linha(resumo, "PIX").getTotal(), 0.001);
        assertEquals(0, BancoDeTeste.linha(resumo, "Cartão").getQuantidade());
    }

    @Test
    void vendasCanceladasNaoEntramNoTotal() {
        BancoDeTeste.inserirVenda(DIA, 8.0, "Dinheiro", false);
        BancoDeTeste.inserirVenda(DIA, 50.0, "Dinheiro", true); // cancelada

        LinhaResumo dinheiro = BancoDeTeste.linha(RelatorioDAO.resumoPorPagamento(DIA), "Dinheiro");

        assertEquals(1, dinheiro.getQuantidade());
        assertEquals(8.0, dinheiro.getTotal(), 0.001);
    }

    @Test
    void vendasDeOutrosDiasNaoEntram() {
        BancoDeTeste.inserirVenda(DIA, 8.0, "Dinheiro", false);
        BancoDeTeste.inserirVenda(DIA.plusDays(1), 99.0, "Dinheiro", false);
        BancoDeTeste.inserirVenda(DIA.minusDays(1), 99.0, "Dinheiro", false);

        LinhaResumo dinheiro = BancoDeTeste.linha(RelatorioDAO.resumoPorPagamento(DIA), "Dinheiro");

        assertEquals(1, dinheiro.getQuantidade());
        assertEquals(8.0, dinheiro.getTotal(), 0.001);
    }

    @Test
    void somaOsProdutosVendidosSemContarOsCancelados() {
        Produto ficha5 = BancoDeTeste.produto("Ficha R$ 5,00");
        Produto cafe = BancoDeTeste.produto("Café / Chá");

        long venda1 = BancoDeTeste.inserirVenda(DIA, 10.0, "Dinheiro", false);
        BancoDeTeste.inserirItem(venda1, ficha5, 2);

        long venda2 = BancoDeTeste.inserirVenda(DIA, 7.0, "PIX", false);
        BancoDeTeste.inserirItem(venda2, ficha5, 1);
        BancoDeTeste.inserirItem(venda2, cafe, 1);

        long cancelada = BancoDeTeste.inserirVenda(DIA, 50.0, "PIX", true);
        BancoDeTeste.inserirItem(cancelada, ficha5, 10); // não pode aparecer

        List<LinhaResumo> produtos = RelatorioDAO.resumoPorProduto(DIA);

        assertEquals(2, produtos.size());
        assertEquals("Ficha R$ 5,00", produtos.get(0).getDescricao()); // o mais vendido primeiro
        assertEquals(3, produtos.get(0).getQuantidade());
        assertEquals(3 * ficha5.getPreco(), produtos.get(0).getTotal(), 0.001);
        assertEquals("Café / Chá", produtos.get(1).getDescricao());
        assertEquals(1, produtos.get(1).getQuantidade());
    }

    @Test
    void contaAsVendasCanceladasDoDia() {
        BancoDeTeste.inserirVenda(DIA, 5.0, "Dinheiro", true);
        BancoDeTeste.inserirVenda(DIA, 3.0, "PIX", true);
        BancoDeTeste.inserirVenda(DIA, 20.0, "PIX", false);

        LinhaResumo canceladas = RelatorioDAO.resumoCancelamentos(DIA);

        assertEquals(2, canceladas.getQuantidade());
        assertEquals(8.0, canceladas.getTotal(), 0.001);
    }
}