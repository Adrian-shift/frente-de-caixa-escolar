package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CaixaController {

    // Botões de forma de pagamento
    @FXML
    private Button btnDinheiro;
    @FXML
    private Button btnCartao;
    @FXML
    private Button btnPix;
    @FXML
    private Button btnFinalizar;

    // Botão do menu lateral que abre a tela de produtos
    @FXML
    private Button btnGerenciarProdutos;

    // Painel onde os botões dos produtos são criados
    @FXML
    private FlowPane painelProdutos;

    @FXML
    private ListView<String> listaCarrinho;

    @FXML
    private Label labelTotal;

    @FXML
    private TextField campoValorRecebido;
    @FXML
    private TextField campoTroco;

    // Forma de pagamento escolhida (padrão: Dinheiro)
    private String formaPagamento = "Dinheiro";

    // Soma dos itens
    private double totalCompra = 0.0;

    // Guarda os produtos do carrinho (vamos usar na Etapa 5)
    private final List<Produto> itensCarrinho = new ArrayList<>();

    // Roda automaticamente quando a tela abre
    @FXML
    public void initialize() {
        carregarProdutos();

        campoValorRecebido.textProperty().addListener((observador, valorAntigo, valorNovo) -> {
            calcularTroco();
        });

        btnDinheiro.setOnAction(e -> formaPagamento = "Dinheiro");
        btnCartao.setOnAction(e -> formaPagamento = "Cartão");
        btnPix.setOnAction(e -> formaPagamento = "PIX");

        btnFinalizar.setOnAction(e -> finalizarVenda());

        btnGerenciarProdutos.setOnAction(e -> abrirGerenciarProdutos());
    }

    // Abre a janela de gerenciar produtos e, quando ela fechar, atualiza os botões
    private void abrirGerenciarProdutos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/edu/ifba/cantinapdv/ProdutosView.fxml"));
            Scene cena = new Scene(loader.load());

            Stage janela = new Stage();
            janela.setTitle("Gerenciar Produtos");
            janela.initModality(Modality.APPLICATION_MODAL); // bloqueia a tela de vendas enquanto aberta
            janela.setScene(cena);
            janela.showAndWait(); // espera a janela fechar

            carregarProdutos(); // atualiza os botões de venda
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Lê os produtos ativos do banco e cria um botão para cada um
    private void carregarProdutos() {
        painelProdutos.getChildren().clear();

        for (Produto produto : ProdutoDAO.listarAtivos()) {
            Button botao = new Button(textoDoBotao(produto));
            botao.setPrefSize(140, 90);
            botao.setWrapText(true);
            botao.setTextAlignment(TextAlignment.CENTER);
            botao.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            botao.setOnAction(e -> adicionarItemAoCarrinho(produto));

            painelProdutos.getChildren().add(botao);
        }
    }

    // Fichas já têm o valor no nome; os outros mostram o preço embaixo
    private String textoDoBotao(Produto produto) {
        if (produto.getCategoria().equals("Ficha")) {
            return produto.getNome();
        }
        return produto.getNome() + "\nR$ " + String.format("%.2f", produto.getPreco());
    }

    private void adicionarItemAoCarrinho(Produto produto) {
        listaCarrinho.getItems().add(produto.getNome() + " - R$ " + String.format("%.2f", produto.getPreco()));
        itensCarrinho.add(produto);

        totalCompra = totalCompra + produto.getPreco();
        labelTotal.setText(String.format("R$ %.2f", totalCompra));

        calcularTroco();
    }

    private void calcularTroco() {
        try {
            String textoRecebido = campoValorRecebido.getText().replace(",", ".");

            if (textoRecebido.isEmpty()) {
                campoTroco.setText("0,00");
                return;
            }

            double valorRecebido = Double.parseDouble(textoRecebido);
            double troco = valorRecebido - totalCompra;

            if (troco < 0) {
                campoTroco.setText("0,00");
            } else {
                campoTroco.setText(String.format("%.2f", troco));
            }

        } catch (NumberFormatException e) {
            campoTroco.setText("0,00");
        }
    }

    private void finalizarVenda() {
        if (totalCompra <= 0) {
            return;
        }

        boolean sucesso = VendaDAO.salvarVenda(totalCompra, formaPagamento);

        if (sucesso) {
            System.out.println("Venda de R$ " + totalCompra + " em " + formaPagamento + " guardada com sucesso!");

            listaCarrinho.getItems().clear();
            itensCarrinho.clear();
            totalCompra = 0.0;
            labelTotal.setText("R$ 0,00");
            campoValorRecebido.clear();
            campoTroco.clear();
            formaPagamento = "Dinheiro";
        }
    }
}