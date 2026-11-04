package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.RelatorioPeriodoDAO;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import br.edu.ifba.cantinapdv.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioPeriodoDAOTest {

    // Período de 5 dias: 10 a 14 de março
    private static final LocalDate DE = LocalDate.of(2026, 3, 10);
    private static final LocalDate ATE = LocalDate.of(2026, 3, 14);

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();

        BancoDeTeste.inserirVenda(DE, 10.0, "Dinheiro", false);              // primeiro dia (conta)
        BancoDeTeste.inserirVenda(DE.plusDays(2), 20.0, "PIX", false);       // meio (conta)
        BancoDeTeste.inserirVenda(DE.plusDays(2), 5.0, "PIX", true);         // cancelada (não conta)
        BancoDeTeste.inserirVenda(ATE, 7.0, "Cartão", false);                // último dia (conta)
        BancoDeTeste.inserirVenda(DE.minusDays(1), 99.0, "Dinheiro", false); // antes do período (fora)
        BancoDeTeste.inserirVenda(ATE.plusDays(1), 99.0, "Dinheiro", false); // depois do período (fora)
    }

    @Test
    void resumoGeralSomaSoOsDiasDoPeriodoSemAsCanceladas() {
        LinhaResumo geral = RelatorioPeriodoDAO.resumoGeral(DE, ATE);

        assertEquals(3, geral.getQuantidade());
        assertEquals(37.0, geral.getTotal(), 0.001);
    }

    @Test
    void contaAsCanceladasDoPeriodo() {
        LinhaResumo canceladas = RelatorioPeriodoDAO.resumoCancelamentos(DE, ATE);

        assertEquals(1, canceladas.getQuantidade());
        assertEquals(5.0, canceladas.getTotal(), 0.001);
    }

    @Test
    void vendasPorDiaTemUmaEntradaParaCadaDiaInclusiveOsSemVenda() {
        Map<LocalDate, Double> porDia = RelatorioPeriodoDAO.vendasPorDia(DE, ATE);

        assertEquals(5, porDia.size());
        assertEquals(10.0, porDia.get(DE), 0.001);
        assertEquals(0.0, porDia.get(DE.plusDays(1)), 0.001);   // dia sem venda aparece zerado
        assertEquals(20.0, porDia.get(DE.plusDays(2)), 0.001);  // a cancelada não entrou
        assertEquals(7.0, porDia.get(ATE), 0.001);              // o último dia também conta
    }

    @Test
    void somaPorFormaDePagamentoNoPeriodo() {
        List<LinhaResumo> resumo = RelatorioPeriodoDAO.resumoPorPagamento(DE, ATE);

        assertEquals(10.0, BancoDeTeste.linha(resumo, "Dinheiro").getTotal(), 0.001);
        assertEquals(20.0, BancoDeTeste.linha(resumo, "PIX").getTotal(), 0.001);
        assertEquals(7.0, BancoDeTeste.linha(resumo, "Cartão").getTotal(), 0.001);
    }

    @Test
    void produtosMaisVendidosVemEmOrdemDecrescente() {
        Produto ficha5 = BancoDeTeste.produto("Ficha R$ 5,00");
        Produto cafe = BancoDeTeste.produto("Café / Chá");

        long venda1 = BancoDeTeste.inserirVenda(DE, 15.0, "Dinheiro", false);
        BancoDeTeste.inserirItem(venda1, ficha5, 3);
        long venda2 = BancoDeTeste.inserirVenda(DE.plusDays(1), 10.0, "PIX", false);
        BancoDeTeste.inserirItem(venda2, cafe, 5);

        List<LinhaResumo> produtos = RelatorioPeriodoDAO.produtosMaisVendidos(DE, ATE);

        assertEquals("Café / Chá", produtos.get(0).getDescricao()); // 5 unidades
        assertEquals(5, produtos.get(0).getQuantidade());
        assertEquals("Ficha R$ 5,00", produtos.get(1).getDescricao()); // 3 unidades
        assertEquals(3, produtos.get(1).getQuantidade());
    }
}