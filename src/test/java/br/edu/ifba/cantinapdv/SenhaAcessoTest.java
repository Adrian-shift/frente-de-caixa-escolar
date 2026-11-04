package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.ConfiguracaoDAO;
import br.edu.ifba.cantinapdv.dao.SenhaAcesso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SenhaAcessoTest {

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
    }

    @Test
    void semSenhaDefinidaTudoELiberado() {
        assertFalse(SenhaAcesso.existeSenha());
        assertTrue(SenhaAcesso.verificar("qualquer coisa"));
    }

    @Test
    void aceitaASenhaCertaERecusaAsErradas() {
        assertTrue(SenhaAcesso.definirSenha("1234"));

        assertTrue(SenhaAcesso.existeSenha());
        assertTrue(SenhaAcesso.verificar("1234"));
        assertFalse(SenhaAcesso.verificar("12345"));
        assertFalse(SenhaAcesso.verificar("123"));
        assertFalse(SenhaAcesso.verificar(""));
    }

    @Test
    void naoGuardaASenhaEmTexto() {
        SenhaAcesso.definirSenha("senhaSecreta-ZYX");

        String guardado = ConfiguracaoDAO.obter("senha");

        assertNotNull(guardado);
        assertFalse(guardado.contains("senhaSecreta-ZYX"));
    }

    @Test
    void duasVezesaMesmaSenhaGuardaCodigosDiferentesPorCausaDoSal() {
        SenhaAcesso.definirSenha("1234");
        String primeira = ConfiguracaoDAO.obter("senha");

        SenhaAcesso.definirSenha("1234");
        String segunda = ConfiguracaoDAO.obter("senha");

        assertNotEquals(primeira, segunda);
        assertTrue(SenhaAcesso.verificar("1234")); // e as duas continuam funcionando
    }

    @Test
    void trocarASenhaInvalidaAAntiga() {
        SenhaAcesso.definirSenha("1234");
        SenhaAcesso.definirSenha("abcd");

        assertFalse(SenhaAcesso.verificar("1234"));
        assertTrue(SenhaAcesso.verificar("abcd"));
    }

    @Test
    void removerASenhaLiberaTudoDeNovo() {
        SenhaAcesso.definirSenha("1234");

        assertTrue(SenhaAcesso.removerSenha());

        assertFalse(SenhaAcesso.existeSenha());
        assertTrue(SenhaAcesso.verificar("qualquer"));
    }
}