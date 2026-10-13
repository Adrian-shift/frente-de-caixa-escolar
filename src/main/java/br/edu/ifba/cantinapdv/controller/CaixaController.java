package br.edu.ifba.cantinapdv.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class CaixaController {

    @FXML
    private Button btnDinheiro;
    @FXML
    private Button btnCartao;
    @FXML
    private Button btnPix;
    @FXML
    private Button btnFinalizar;

    // Variável para guardar a forma de pagamento escolhida (Padrão: Dinheiro)
    private String formaPagamento = "Dinheiro";

    // Fazendo o link com os botões e campos do Scene Builder / FXML
    @FXML
    private Button btnFicha3;
    @FXML
    private Button btnFicha5;

    @FXML
    private ListView<String> listaCarrinho;

    @FXML
    private Label labelTotal;

    @FXML
    private TextField campoValorRecebido;
    @FXML
    private TextField campoTroco;

    // Variável para guardar a soma dos itens
    private double totalCompra = 0.0;

    // O método initialize roda automaticamente assim que a tela abre
    @FXML
    public void initialize() {
        // Configurando a ação do botão da Ficha de R$ 5,00
        btnFicha5.setOnAction(event -> adicionarItemAoCarrinho("Ficha R$ 5,00", 5.00));

        // Configurando a ação do botão da Ficha de R$ 3,00
        btnFicha3.setOnAction(event -> adicionarItemAoCarrinho("Ficha R$ 3,00", 3.00));

        campoValorRecebido.textProperty().addListener((observador, valorAntigo, valorNovo) -> {
            calcularTroco();
        });

        // Configurar a escolha do método de pagamento
        btnDinheiro.setOnAction(e -> formaPagamento = "Dinheiro");
        btnCartao.setOnAction(e -> formaPagamento = "Cartão");
        btnPix.setOnAction(e -> formaPagamento = "PIX");

        // Ação do botão Finalizar Venda
        btnFinalizar.setOnAction(e -> finalizarVenda());
    }

    // Método que calcula o troco automaticamente
    private void calcularTroco() {
        try {
            // Pega o texto que a atendente digitou e troca vírgula por ponto (para o Java entender a matemática)
            String textoRecebido = campoValorRecebido.getText().replace(",", ".");

            // Se o campo estiver vazio, zera o troco
            if (textoRecebido.isEmpty()) {
                campoTroco.setText("0,00");
                return;
            }

            // Converte o texto digitado para número decimal
            double valorRecebido = Double.parseDouble(textoRecebido);

            // Faz a conta do troco (Recebido - Total da Compra)
            double troco = valorRecebido - totalCompra;

            // Se o valor recebido for menor que o total (ainda falta dinheiro), não mostra troco negativo
            if (troco < 0) {
                campoTroco.setText("0,00");
            } else {
                // Se estiver tudo certo, mostra o troco formatado com duas casas decimais
                campoTroco.setText(String.format("%.2f", troco));
            }

        } catch (NumberFormatException e) {
            // Se a atendente digitar sem querer uma letra em vez de número, zera o troco
            campoTroco.setText("0,00");
        }
    }

    // Método que adiciona o texto na lista e soma o valor
    private void adicionarItemAoCarrinho(String descricao, double preco) {
        // Adiciona o item visualmente na lista do carrinho
        listaCarrinho.getItems().add(descricao + " - R$ " + String.format("%.2f", preco));

        // Faz a matemática do total
        totalCompra = totalCompra + preco;

        // Atualiza o texto vermelho do "TOTAL" na tela
        labelTotal.setText(String.format("R$ %.2f", totalCompra));
    }

    private void finalizarVenda() {
        // Impede que a atendente guarde uma venda com valor zero
        if (totalCompra <= 0) {
            return;
        }

        // Chama a classe DAO para guardar no banco de dados SQLite
        boolean sucesso = br.edu.ifba.cantinapdv.dao.VendaDAO.salvarVenda(totalCompra, formaPagamento);

        if (sucesso) {
            System.out.println("Venda de R$ " + totalCompra + " em " + formaPagamento + " guardada com sucesso!");

            // Limpa a tela em 1 milissegundo para o próximo aluno da fila
            listaCarrinho.getItems().clear();
            totalCompra = 0.0;
            labelTotal.setText("R$ 0,00");
            campoValorRecebido.clear();
            campoTroco.clear();
            formaPagamento = "Dinheiro"; // Volta ao padrão
        }
    }
}
