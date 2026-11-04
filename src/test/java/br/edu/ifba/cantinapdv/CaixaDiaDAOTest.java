package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.CaixaDiaDAO;
import br.edu.ifba.cantinapdv.model.CaixaDia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CaixaDiaDAOTest {

    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
    }

    @Test
    void diaSemConferenciaDevolveNull() {
        assertNull(CaixaDiaDAO.buscar(DIA));
    }

    @Test
    void salvaTrocoInicialSemDinheiroContado() {
        assertTrue(CaixaDiaDAO.salvar(DIA, 20.0, null));

        CaixaDia caixa = CaixaDiaDAO.buscar(DIA);
        assertNotNull(caixa);
        assertEquals(20.0, caixa.getTrocoInicial(), 0.001);
        assertNull(caixa.getDinheiroContado());
    }

    @Test
    void salvarDeNovoAtualizaEmVezDeDuplicar() {
        CaixaDiaDAO.salvar(DIA, 20.0, null);
        CaixaDiaDAO.salvar(DIA, 20.0, 33.5);

        CaixaDia caixa = CaixaDiaDAO.buscar(DIA);
        assertEquals(33.5, caixa.getDinheiroContado(), 0.001);
        assertEquals(1, BancoDeTeste.contar("caixa_dia"));
    }

    @Test
    void cadaDiaTemASuaConferencia() {
        CaixaDiaDAO.salvar(DIA, 10.0, 15.0);
        CaixaDiaDAO.salvar(DIA.plusDays(1), 50.0, null);

        assertEquals(10.0, CaixaDiaDAO.buscar(DIA).getTrocoInicial(), 0.001);
        assertEquals(50.0, CaixaDiaDAO.buscar(DIA.plusDays(1)).getTrocoInicial(), 0.001);
    }
}