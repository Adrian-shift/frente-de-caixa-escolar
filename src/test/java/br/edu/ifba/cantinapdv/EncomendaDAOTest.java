package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.EncomendaDAO;
import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.dao.RelatorioDAO;
import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.Encomenda;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import br.edu.ifba.cantinapdv.model.Produto;
import br.edu.ifba.cantinapdv.model.VendaDoDia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EncomendaDAOTest {

    private final LocalDate hoje = LocalDate.now();
    private Produto marmita;

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
        ProdutoDAO.inserir(new Produto("PF", "Marmita", 18.0));
        marmita = BancoDeTeste.produto("PF");
    }

    private Encomenda nova(String cliente, int quantidade, LocalDate dia) {
        return new Encomenda(cliente, marmita.getId(), quantidade, marmita.getPreco() * quantidade, dia, "");
    }

    private Encomenda buscar(List<Encomenda> lista, String cliente) {
        for (Encomenda e : lista) {
            if (e.getNomeCliente().equals(cliente)) {
                return e;
            }
        }
        return null;
    }

    @Test
    void encomendaPendenteNaoGeraVenda() {
        assertTrue(EncomendaDAO.inserir(nova("Ana", 1, hoje), null));

        assertEquals(1, BancoDeTeste.contar("encomendas"));
        assertEquals(0, BancoDeTeste.contar("vendas"));

        Encomenda ana = EncomendaDAO.listarPorData(hoje).get(0);
        assertEquals("Pendente", ana.getStatusPagamento());
        assertEquals("Pendente", ana.getStatusEntrega());
    }

    @Test
    void encomendaPagaNaHoraGeraUmaVendaComOMetodoCerto() {
        assertTrue(EncomendaDAO.inserir(nova("Ana", 2, hoje), "PIX"));

        assertEquals(1, BancoDeTeste.contar("vendas"));
        assertEquals(1, BancoDeTeste.contar("itens_venda"));

        Encomenda ana = EncomendaDAO.listarPorData(hoje).get(0);
        assertEquals("Pago", ana.getStatusPagamento());
        assertEquals(2, ana.getQuantidade());
        assertEquals("PF", ana.getNomeProduto());

        // o dinheiro entra no fechamento, na forma de pagamento escolhida
        LinhaResumo pix = BancoDeTeste.linha(RelatorioDAO.resumoPorPagamento(hoje), "PIX");
        assertEquals(1, pix.getQuantidade());
        assertEquals(36.0, pix.getTotal(), 0.001);

        // e a venda criada é marcada como de encomenda
        VendaDoDia venda = VendaDAO.listarPorData(hoje).get(0);
        assertTrue(venda.isDeEncomenda());
        assertFalse(VendaDAO.cancelarVenda(venda.getId(), "teste")); // não pode ser cancelada por aqui
    }

    @Test
    void registrarPagamentoCriaAVendaUmaVezSo() {
        EncomendaDAO.inserir(nova("Bruno", 1, hoje), null);
        Encomenda bruno = EncomendaDAO.listarPorData(hoje).get(0);

        assertTrue(EncomendaDAO.registrarPagamento(bruno, "Dinheiro"));
        assertEquals(1, BancoDeTeste.contar("vendas"));
        assertEquals("Pago", EncomendaDAO.listarPorData(hoje).get(0).getStatusPagamento());

        // segunda tentativa: recusada, e não pode sobrar uma venda duplicada
        assertFalse(EncomendaDAO.registrarPagamento(bruno, "Dinheiro"));
        assertEquals(1, BancoDeTeste.contar("vendas"));
    }

    @Test
    void marcaComoEntregue() {
        EncomendaDAO.inserir(nova("Ana", 1, hoje), "PIX");
        Encomenda ana = EncomendaDAO.listarPorData(hoje).get(0);

        assertTrue(EncomendaDAO.marcarEntregue(ana.getId()));

        assertEquals("Entregue", EncomendaDAO.listarPorData(hoje).get(0).getStatusEntrega());
    }

    @Test
    void cancelaSomenteEncomendaQueAindaNaoFoiPaga() {
        EncomendaDAO.inserir(nova("Ana", 1, hoje), null);   // pendente
        EncomendaDAO.inserir(nova("Bia", 1, hoje), "PIX");  // paga

        List<Encomenda> lista = EncomendaDAO.listarPorData(hoje);
        Encomenda pendente = buscar(lista, "Ana");
        Encomenda paga = buscar(lista, "Bia");

        assertTrue(EncomendaDAO.cancelar(pendente.getId()));
        assertFalse(EncomendaDAO.cancelar(paga.getId()));
        assertEquals(1, BancoDeTeste.contar("encomendas")); // só a paga sobrou
    }

    @Test
    void listaSomenteAsEncomendasDoDiaEscolhido() {
        EncomendaDAO.inserir(nova("Ana", 1, hoje), null);
        EncomendaDAO.inserir(nova("Bia", 1, hoje.plusDays(1)), null);

        assertEquals(1, EncomendaDAO.listarPorData(hoje).size());
        assertEquals("Ana", EncomendaDAO.listarPorData(hoje).get(0).getNomeCliente());
        assertEquals("Bia", EncomendaDAO.listarPorData(hoje.plusDays(1)).get(0).getNomeCliente());
    }
}