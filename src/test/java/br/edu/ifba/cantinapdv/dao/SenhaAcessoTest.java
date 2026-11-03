package br.edu.ifba.cantinapdv.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SenhaAcessoTest {

    @BeforeEach
    void setUp() {
        // Garante que o estado comece limpo antes de cada teste
        SenhaAcesso.removerSenha();
    }

    @AfterEach
    void tearDown() {
        // Limpa resíduos para não afetar outros testes
        SenhaAcesso.removerSenha();
    }

    @Test
    @DisplayName("Quando não há senha definida, qualquer digitação deve ser aceita")
    void devePermitirAcessoQuandoNaoExisteSenhaConfigurada() {
        assertFalse(SenhaAcesso.existeSenha(), "Não deve haver senha no início");
        assertTrue(SenhaAcesso.verificar("qualquerCoisa"), "Sem senha definida, o retorno deve ser true");
    }

    @Test
    @DisplayName("Deve salvar hash da senha e validar corretamente a senha digitada")
    void deveDefinirEVerificarSenhaComSucesso() {
        String senhaCorreta = "admin123";
        boolean definida = SenhaAcesso.definirSenha(senhaCorreta);

        assertTrue(definida, "A senha deveria ter sido salva com sucesso");
        assertTrue(SenhaAcesso.existeSenha(), "Agora deve constar que a senha existe");
        assertTrue(SenhaAcesso.verificar(senhaCorreta), "A senha correta deve passar");
        assertFalse(SenhaAcesso.verificar("senhaErrada"), "A senha errada deve ser rejeitada");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "admin", "P@ssw0rdSuperSegura!"})
    @DisplayName("Deve aceitar senhas válidas com diferentes comprimentos acima do mínimo")
    void deveAceitarSenhasComTamanhoValido(String senhaValida) {
        assertTrue(senhaValida.length() >= SenhaAcesso.TAMANHO_MINIMO);
        assertTrue(SenhaAcesso.definirSenha(senhaValida));
        assertTrue(SenhaAcesso.verificar(senhaValida));
    }
}