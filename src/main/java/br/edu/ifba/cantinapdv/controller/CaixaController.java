package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class CaixaController {

    // Estado "ativo" dos botões de pagamento (o estilo dele está no estilo.css)
    private static final PseudoClass PSEUDO_ATIVO = PseudoClass.getPseudoClass("ativo");

    // Teclas de atalho (só valem enquanto a tela de vendas está visível)
    private final KeyCodeCombination atalhoF1 = new KeyCodeCombination(KeyCode.F1);
    private final KeyCodeCombination atalhoF2 = new KeyCodeCombination(KeyCode.F2);
    private final KeyCodeCombination atalhoF3 = new KeyCodeCombination(KeyCode.F3);
    private final KeyCodeCombination atalhoEnter = new KeyCodeCombination(KeyCode.ENTER);

    // Botões de forma de pagamento
    @FXML
    private Button btnDinheiro;
    @FXML
    private Button btnCartao;
    @FXML
    private Button btnPix;
    @FXML
    private Button btnFinalizar;

    // Botões para corrigir o pedido
    @FXML
    private Button btnRemoverItem;
    @FXML
    private Button btnLimparPedido;

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

    // Produtos que estão no carrinho (na mesma ordem da lista da tela)
    private final List<Produto> itensCarrinho = new ArrayList<>();

    @FXML
    public void initialize() {
        carregarProdutos();

        campoValorRecebido.textProperty().addListener((observador, valorAntigo, valorNovo) -> {
            calcularTroco();
        });

        // Escolha da forma de pagamento (clique)
        btnDinheiro.setOnAction(e -> selecionarFormaPagamento("Dinheiro"));
        btnCartao.setOnAction(e -> selecionarFormaPagamento("Cartão"));
        btnPix.setOnAction(e -> selecionarFormaPagamento("PIX"));

        // Começa com Dinheiro destacado
        selecionarFormaPagamento("Dinheiro");

        btnFinalizar.setOnAction(e -> finalizarVenda());

        // O botão Finalizar fica desativado (cinza) enquanto o carrinho está vazio
        btnFinalizar.disableProperty().bind(Bindings.isEmpty(listaCarrinho.getItems()));

        // Corrigir o pedido
        btnRemoverItem.setOnAction(e -> removerItemSelecionado());
        btnLimparPedido.setOnAction(e -> limparPedido());

        // Atalhos de teclado: ligados quando a tela de vendas entra na janela
        // e desligados quando ela sai (ao visitar outra área do sistema).
        btnFinalizar.sceneProperty().addListener((obs, cenaAntiga, cenaNova) -> {
            if (cenaAntiga != null) {
                cenaAntiga.getAccelerators().remove(atalhoF1);
                cenaAntiga.getAccelerators().remove(atalhoF2);
                cenaAntiga.getAccelerators().remove(atalhoF3);
                cenaAntiga.getAccelerators().remove(atalhoEnter);
            }
            if (cenaNova != null) {
                cenaNova.getAccelerators().put(atalhoF1, () -> selecionarFormaPagamento("Dinheiro"));
                cenaNova.getAccelerators().put(atalhoF2, () -> selecionarFormaPagamento("Cartão"));
                cenaNova.getAccelerators().put(atalhoF3, () -> selecionarFormaPagamento("PIX"));
                cenaNova.getAccelerators().put(atalhoEnter, () -> finalizarVenda());
            }
        });
    }

    // Chamado pelo MainController sempre que a tela de vendas volta a aparecer
    public void atualizarProdutos() {
        carregarProdutos();
    }

    // Guarda a forma de pagamento e liga o destaque (verde) só no botão escolhido
    private void selecionarFormaPagamento(String forma) {
        formaPagamento = forma;

        btnDinheiro.pseudoClassStateChanged(PSEUDO_ATIVO, forma.equals("Dinheiro"));
        btnCartao.pseudoClassStateChanged(PSEUDO_ATIVO, forma.equals("Cartão"));
        btnPix.pseudoClassStateChanged(PSEUDO_ATIVO, forma.equals("PIX"));
    }

    // Lê os produtos ativos do banco e cria um botão para cada um
    private void carregarProdutos() {
        painelProdutos.getChildren().clear();

        for (Produto produto : ProdutoDAO.listarAtivos()) {
            Button botao = new Button(textoDoBotao(produto));
            botao.setPrefSize(160, 100);
            botao.setWrapText(true);
            botao.setTextAlignment(TextAlignment.CENTER);

            // O visual (tamanho da letra, cor) vem do CSS, pela classe do botão e da categoria
            botao.getStyleClass().addAll("botao-produto", classeDaCategoria(produto));

            botao.setOnAction(e -> adicionarItemAoCarrinho(produto));

            painelProdutos.getChildren().add(botao);
        }
    }

    // Cada categoria tem uma cor no estilo.css
    private String classeDaCategoria(Produto produto) {
        switch (produto.getCategoria()) {
            case "Ficha":
                return "cat-ficha";
            case "Bebida":
                return "cat-bebida";
            case "Lanche":
                return "cat-lanche";
            case "Marmita":
                return "cat-marmita";
            default:
                return "cat-outro";
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

        // Sem nada selecionado, "Remover item" desfaz sempre o último adicionado
        listaCarrinho.getSelectionModel().clearSelection();
        // Mostra o item recém-adicionado, mesmo com o carrinho cheio
        listaCarrinho.scrollTo(itensCarrinho.size() - 1);

        recalcularTotal();
    }

    // Remove o item selecionado na lista; se nada estiver selecionado, remove o último
    private void removerItemSelecionado() {
        if (itensCarrinho.isEmpty()) {
            return;
        }

        int indice = listaCarrinho.getSelectionModel().getSelectedIndex();
        if (indice < 0) {
            indice = itensCarrinho.size() - 1;
        }

        listaCarrinho.getItems().remove(indice);
        itensCarrinho.remove(indice);
        listaCarrinho.getSelectionModel().clearSelection();

        recalcularTotal();
    }

    // Soma os itens do carrinho de novo e atualiza o total e o troco
    private void recalcularTotal() {
        double soma = 0.0;
        for (Produto produto : itensCarrinho) {
            soma += produto.getPreco();
        }
        totalCompra = soma;

        labelTotal.setText(String.format("R$ %.2f", totalCompra));
        calcularTroco();
    }

    // Esvazia o pedido e volta a tela ao estado inicial
    private void limparPedido() {
        listaCarrinho.getItems().clear();
        itensCarrinho.clear();
        totalCompra = 0.0;
        labelTotal.setText("R$ 0,00");
        campoValorRecebido.clear();
        campoTroco.clear();

        // Volta para Dinheiro (e o destaque verde volta junto)
        selecionarFormaPagamento("Dinheiro");
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

        boolean sucesso = VendaDAO.salvarVenda(totalCompra, formaPagamento, itensCarrinho);

        if (sucesso) {
            System.out.println("Venda de R$ " + totalCompra + " em " + formaPagamento + " guardada com sucesso!");
            limparPedido();
        }
    }
}