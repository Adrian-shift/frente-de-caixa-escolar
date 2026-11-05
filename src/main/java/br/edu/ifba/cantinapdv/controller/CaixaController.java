package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.Log;
import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

public class CaixaController {

    // Estado "ativo" dos botões de pagamento (o estilo dele está no estilo.css)
    private static final PseudoClass PSEUDO_ATIVO = PseudoClass.getPseudoClass("ativo");

    // Formato aceito no valor recebido: números com até 2 casas decimais (vírgula ou ponto)
    private static final Pattern PADRAO_VALOR = Pattern.compile("\\d*([.,]\\d{0,2})?");

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

    // Aviso em vermelho abaixo dos campos (ex.: "Faltam R$ 3,00")
    @FXML
    private Label labelAviso;

    // Forma de pagamento escolhida (padrão: Dinheiro)
    private String formaPagamento = "Dinheiro";

    // Soma dos itens
    private double totalCompra = 0.0;

    // Um produto por clique (é isso que vai para o banco; o VendaDAO agrupa por produto)
    private final List<Produto> itensCarrinho = new ArrayList<>();

    // Para cada linha da lista da tela, o id do produto que ela representa
    private final List<Integer> idsDasLinhas = new ArrayList<>();

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

        // O aviso de "faltam R$..." só vale para dinheiro, então atualiza ao trocar a forma
        calcularTroco();
    }

    // Lê os produtos ativos do banco e cria um botão para cada um
    private void carregarProdutos() {
        painelProdutos.getChildren().clear();

        for (Produto produto : ProdutoDAO.listarAtivos()) {

            // Botão principal
            Button botao = new Button();
            botao.setPrefSize(200, 140);

            // Nome do produto
            Label nome = new Label(produto.getNome());
            nome.getStyleClass().add("produto-nome");
            nome.setWrapText(true);
            nome.setMaxWidth(Double.MAX_VALUE);
            nome.setAlignment(Pos.CENTER);

            // Preço do produto
            Label preco = new Label(
                    String.format("R$ %.2f", produto.getPreco())
            );

            preco.getStyleClass().add("produto-preco");
            preco.setMaxWidth(Double.MAX_VALUE);
            preco.setAlignment(Pos.CENTER);

            // Organiza nome e preço verticalmente
            VBox conteudo = new VBox(6);
            conteudo.setAlignment(Pos.CENTER);
            conteudo.getChildren().addAll(nome, preco);

            // Coloca o conteúdo dentro do botão
            botao.setGraphic(conteudo);
            botao.setContentDisplay(
                    javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
            );

            // Mantém as cores das categorias
            botao.getStyleClass().addAll(
                    "botao-produto",
                    classeDaCategoria(produto)
            );

            // Mantém a funcionalidade do carrinho
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

    private void adicionarItemAoCarrinho(Produto produto) {
        itensCarrinho.add(produto);
        atualizarListaVisual(null);

        // Sem nada selecionado, "Remover item" desfaz sempre o último clique
        listaCarrinho.getSelectionModel().clearSelection();
        // Mostra a linha do produto recém-clicado, mesmo com o carrinho cheio
        listaCarrinho.scrollTo(idsDasLinhas.indexOf(produto.getId()));

        recalcularTotal();
    }

    // Monta a lista da tela juntando os itens iguais: "3 × Ficha R$ 5,00 — R$ 15,00".
    // Se idParaSelecionar não for null, deixa a linha desse produto selecionada.
    private void atualizarListaVisual(Integer idParaSelecionar) {
        Map<Integer, Produto> produtos = new LinkedHashMap<>();
        Map<Integer, Integer> quantidades = new LinkedHashMap<>();
        for (Produto p : itensCarrinho) {
            produtos.putIfAbsent(p.getId(), p);
            quantidades.merge(p.getId(), 1, Integer::sum);
        }

        List<String> linhas = new ArrayList<>();
        idsDasLinhas.clear();
        for (Map.Entry<Integer, Integer> entrada : quantidades.entrySet()) {
            Produto p = produtos.get(entrada.getKey());
            int quantidade = entrada.getValue();

            linhas.add(quantidade + " × " + p.getNome() + " — R$ " + String.format("%.2f", p.getPreco() * quantidade));
            idsDasLinhas.add(entrada.getKey());
        }

        listaCarrinho.getItems().setAll(linhas);

        if (idParaSelecionar != null) {
            int indice = idsDasLinhas.indexOf(idParaSelecionar);
            if (indice >= 0) {
                listaCarrinho.getSelectionModel().select(indice);
            }
        }
    }

    // Tira UMA unidade: do produto da linha selecionada ou, sem seleção, do último clique
    private void removerItemSelecionado() {
        if (itensCarrinho.isEmpty()) {
            return;
        }

        int linha = listaCarrinho.getSelectionModel().getSelectedIndex();

        if (linha >= 0 && linha < idsDasLinhas.size()) {
            int idRemovido = idsDasLinhas.get(linha);

            // Remove a última unidade desse produto
            for (int i = itensCarrinho.size() - 1; i >= 0; i--) {
                if (itensCarrinho.get(i).getId() == idRemovido) {
                    itensCarrinho.remove(i);
                    break;
                }
            }
            // Mantém a linha selecionada (se ainda existir) para poder clicar de novo
            atualizarListaVisual(idRemovido);
        } else {
            itensCarrinho.remove(itensCarrinho.size() - 1);
            atualizarListaVisual(null);
            listaCarrinho.getSelectionModel().clearSelection();
        }

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
        idsDasLinhas.clear();
        itensCarrinho.clear();
        totalCompra = 0.0;
        labelTotal.setText("R$ 0,00");
        campoValorRecebido.clear();
        campoTroco.clear();

        // Volta para Dinheiro (e o destaque verde volta junto)
        selecionarFormaPagamento("Dinheiro");
    }

    // Converte o texto digitado em número. Lança NumberFormatException se o formato for inválido.
    private double converterValor(String texto) {
        String limpo = texto.trim();
        if (!PADRAO_VALOR.matcher(limpo).matches()) {
            throw new NumberFormatException("valor inválido");
        }
        return Double.parseDouble(limpo.replace(",", "."));
    }

    // Calcula o troco e mostra o aviso "Faltam R$ ..." quando o dinheiro não cobre o total
    private void calcularTroco() {
        labelAviso.setText("");

        if (campoValorRecebido.getText().trim().isEmpty()) {
            campoTroco.setText("0,00");
            return;
        }

        try {
            double valorRecebido = converterValor(campoValorRecebido.getText());

            // Compara em centavos, para não ter erro de arredondamento
            long recebidoCentavos = Math.round(valorRecebido * 100);
            long totalCentavos = Math.round(totalCompra * 100);

            if (recebidoCentavos >= totalCentavos) {
                campoTroco.setText(String.format("%.2f", (recebidoCentavos - totalCentavos) / 100.0));
            } else {
                campoTroco.setText("0,00");
                if (formaPagamento.equals("Dinheiro") && totalCentavos > 0) {
                    labelAviso.setText(String.format("Faltam R$ %.2f", (totalCentavos - recebidoCentavos) / 100.0));
                }
            }

        } catch (NumberFormatException e) {
            campoTroco.setText("0,00");
            if (formaPagamento.equals("Dinheiro")) {
                labelAviso.setText("Valor recebido inválido");
            }
        }
    }

    // Em dinheiro, se a atendente digitou o valor recebido, ele precisa cobrir o total.
    // Se não digitou nada, tudo bem (ex.: pagou o valor exato).
    private boolean valorRecebidoEstaCerto() {
        if (campoValorRecebido.getText().trim().isEmpty()) {
            return true;
        }

        double recebido;
        try {
            recebido = converterValor(campoValorRecebido.getText());
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Valor recebido", "Valor recebido inválido",
                    "Digite só números, com até 2 casas decimais. Exemplo: 20,00");
            focarValorRecebido();
            return false;
        }

        long recebidoCentavos = Math.round(recebido * 100);
        long totalCentavos = Math.round(totalCompra * 100);

        if (recebidoCentavos < totalCentavos) {
            double faltam = (totalCentavos - recebidoCentavos) / 100.0;
            mostrarAlerta(Alert.AlertType.WARNING, "Valor recebido", "O valor recebido é menor que o total",
                    String.format("Total: R$ %.2f%nRecebido: R$ %.2f%nFaltam: R$ %.2f", totalCompra, recebido, faltam));
            focarValorRecebido();
            return false;
        }

        return true;
    }

    private void focarValorRecebido() {
        campoValorRecebido.requestFocus();
        campoValorRecebido.selectAll();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String cabecalho, String texto) {
        Alert alerta = new Alert(tipo);
        alerta.initOwner(btnFinalizar.getScene().getWindow());
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        alerta.setContentText(texto);
        alerta.showAndWait();
    }

    private void finalizarVenda() {
        if (totalCompra <= 0) {
            return;
        }

        // Em dinheiro, o valor recebido (se foi digitado) precisa cobrir o total
        if (formaPagamento.equals("Dinheiro") && !valorRecebidoEstaCerto()) {
            return;
        }

        boolean sucesso = VendaDAO.salvarVenda(totalCompra, formaPagamento, itensCarrinho);

        if (sucesso) {
            Log.info(String.format("Venda registrada: R$ %.2f em %s", totalCompra, formaPagamento));
            limparPedido();
        } else {
            // O pedido NÃO é apagado: a atendente pode tentar de novo
            mostrarAlerta(Alert.AlertType.ERROR, "Venda não registrada",
                    "ATENÇÃO: esta venda NÃO foi registrada!",
                    "O pedido continua na tela. Tente finalizar de novo.\n\n"
                            + "Se o erro continuar, anote esta venda no caderno e avise o responsável pelo sistema.\n\n"
                            + "Detalhes no arquivo de log:\n" + Log.getArquivo());
        }
    }
}