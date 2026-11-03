package br.edu.ifba.cantinapdv.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraCaixaTest {

    // Método que espelha a lógica de precisão em centavos usada no CaixaController
    public static double calcularTroco(double totalCompra, double valorRecebido) {
        long recebidoCentavos = Math.round(valorRecebido * 100);
        long totalCentavos = Math.round(totalCompra * 100);

        if (recebidoCentavos < totalCentavos) {
            throw new IllegalArgumentException("Valor recebido é menor que o total");
        }
        return (recebidoCentavos - totalCentavos) / 100.0;
    }

    @Test
    @DisplayName("Deve calcular o troco com exatidão sem erros de ponto flutuante")
    void deveCalcularTrocoExato() {
        double total = 14.30;
        double recebido = 20.00;

        double troco = calcularTroco(total, recebido);

        assertEquals(5.70, troco, 0.0001, "O troco de 20.00 - 14.30 deve ser exatamente 5.70");
    }

    @ParameterizedTest(name = "Total: R$ {0}, Recebido: R$ {1} -> Troco esperado: R$ {2}")
    @CsvSource({
            "5.00, 5.00, 0.00",
            "3.50, 5.00, 1.50",
            "18.00, 50.00, 32.00",
            "7.25, 10.00, 2.75"
    })
    @DisplayName("Deve validar múltiplos cenários de troco via CsvSource")
    void deveValidarMultiplosCenariosDeTroco(double total, double recebido, double trocoEsperado) {
        double trocoObtido = calcularTroco(total, recebido);
        assertEquals(trocoEsperado, trocoObtido, 0.0001);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o dinheiro entregue for menor que o total")
    void deveLancarExcecaoQuandoDinheiroForInsuficiente() {
        double total = 10.00;
        double recebido = 7.00;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            calcularTroco(total, recebido);
        });

        assertEquals("Valor recebido é menor que o total", ex.getMessage());
    }
}