package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import br.edu.ifba.cantinapdv.model.VendaDoDia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VendaDAOTest {

    private Produto ficha5;
    private Produto cafe;

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
        ficha5 = BancoDeTeste.produto("Ficha R$ 5,00");
        cafe = BancoDeTeste.produto("Café / Chá");
    }

    @Test
    void salvaAVendaEAgrupaItensRepetidos() {
        List<Produto> carrinho = List.of(ficha5, ficha5, ficha5, cafe);
        double total = 3 * ficha5.getPreco() + cafe.getPreco();

        assertTrue(VendaDAO.salvarVenda(total, "PIX", carrinho));

        assertEquals(1, BancoDeTeste.contar("vendas"));
        assertEquals(2, BancoDeTeste.contar("itens_venda")); // 3 fichas viram 1 linha + 1 linha do café

        List<VendaDoDia> hoje = VendaDAO.listarPorData(LocalDate.now());
        assertEquals(1, hoje.size());
        assertEquals("PIX", hoje.get(0).getMetodoPagamento());
        assertEquals(total, hoje.get(0).getValorTotal(), 0.001);
        assertTrue(hoje.get(0).getItens().contains("3x Ficha R$ 5,00"));
        assertFalse(hoje.get(0).isCancelada());
    }

    @Test
    void vendaComErroNaoGravaNada() {
        // Produto que não existe no banco: gravar o item falha, e a venda inteira deve ser desfeita
        Produto fantasma = new Produto(9999, "Fantasma", "Outro", 1.0, true);

        assertFalse(VendaDAO.salvarVenda(1.0, "Dinheiro", List.of(fantasma)));

        assertEquals(0, BancoDeTeste.contar("vendas"));
        assertEquals(0, BancoDeTeste.contar("itens_venda"));
    }

    @Test
    void cancelaAVendaSemApagar() {
        VendaDAO.salvarVenda(5.0, "Dinheiro", List.of(ficha5));
        int id = VendaDAO.listarPorData(LocalDate.now()).get(0).getId();

        assertTrue(VendaDAO.cancelarVenda(id, "lançada por engano"));

        assertEquals(1, BancoDeTeste.contar("vendas")); // continua registrada
        VendaDoDia venda = VendaDAO.listarPorData(LocalDate.now()).get(0);
        assertTrue(venda.isCancelada());
        assertEquals("Cancelada", venda.getSituacao());
        assertEquals("lançada por engano", venda.getMotivoCancelamento());
    }

    @Test
    void naoCancelaAMesmaVendaDuasVezes() {
        VendaDAO.salvarVenda(5.0, "Dinheiro", List.of(ficha5));
        int id = VendaDAO.listarPorData(LocalDate.now()).get(0).getId();

        assertTrue(VendaDAO.cancelarVenda(id, ""));
        assertFalse(VendaDAO.cancelarVenda(id, ""));
    }

    @Test
    void listaSomenteAsVendasDoDiaEscolhido() {
        VendaDAO.salvarVenda(5.0, "Dinheiro", List.of(ficha5));

        assertEquals(1, VendaDAO.listarPorData(LocalDate.now()).size());
        assertEquals(0, VendaDAO.listarPorData(LocalDate.now().minusDays(1)).size());
    }
}