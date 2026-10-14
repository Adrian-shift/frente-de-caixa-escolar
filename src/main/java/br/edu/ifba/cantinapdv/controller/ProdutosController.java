package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

public class ProdutosController {

    @FXML
    private TableView<Produto> tabelaProdutos;
    @FXML
    private TableColumn<Produto, String> colNome;
    @FXML
    private TableColumn<Produto, String> colCategoria;
    @FXML
    private TableColumn<Produto, Double> colPreco;

    @FXML
    private TextField campoNome;
    @FXML
    private ComboBox<String> comboCategoria;
    @FXML
    private TextField campoPreco;

    @FXML
    private Button btnNovo;
    @FXML
    private Button btnSalvar;
    @FXML
    private Button btnRemover;

    @FXML
    private Label labelMensagem;

    // Produto que está sendo editado agora. Se for null, estamos criando um novo.
    private Produto produtoSelecionado = null;

    @FXML
    public void initialize() {
        // Diz a cada coluna qual dado do Produto mostrar (usa os getters: getNome, getCategoria, getPreco)
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("preco"));

        // Mostra o preço formatado como "R$ 4,50"
        colPreco.setCellFactory(coluna -> new TableCell<Produto, Double>() {
            @Override
            protected void updateItem(Double valor, boolean vazio) {
                super.updateItem(valor, vazio);
                if (vazio || valor == null) {
                    setText(null);
                } else {
                    setText(String.format("R$ %.2f", valor));
                }
            }
        });

        comboCategoria.getItems().addAll("Ficha", "Bebida", "Lanche", "Marmita", "Outro");

        // Quando clica em uma linha da tabela, joga os dados no formulário
        tabelaProdutos.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                preencherFormulario(novo);
            }
        });

        btnNovo.setOnAction(e -> limparFormulario());
        btnSalvar.setOnAction(e -> salvar());
        btnRemover.setOnAction(e -> remover());

        recarregarTabela();
    }

    // Busca os produtos ativos no banco e mostra na tabela
    private void recarregarTabela() {
        tabelaProdutos.getItems().setAll(ProdutoDAO.listarAtivos());
    }

    private void preencherFormulario(Produto produto) {
        produtoSelecionado = produto;
        campoNome.setText(produto.getNome());
        comboCategoria.setValue(produto.getCategoria());
        campoPreco.setText(String.format("%.2f", produto.getPreco()));
        labelMensagem.setText("");
    }

    private void limparFormulario() {
        produtoSelecionado = null;
        tabelaProdutos.getSelectionModel().clearSelection();
        campoNome.clear();
        comboCategoria.setValue(null);
        campoPreco.clear();
        labelMensagem.setText("");
    }

    private void salvar() {
        String nome = campoNome.getText().trim();
        String categoria = comboCategoria.getValue();
        String textoPreco = campoPreco.getText().trim().replace(",", ".");

        // Validações simples
        if (nome.isEmpty()) {
            mostrarAviso("Digite o nome do produto.");
            return;
        }
        if (categoria == null) {
            mostrarAviso("Escolha a categoria.");
            return;
        }

        double preco;
        try {
            preco = Double.parseDouble(textoPreco);
        } catch (NumberFormatException e) {
            mostrarAviso("Preço inválido. Use números, por exemplo: 4,50");
            return;
        }
        if (preco <= 0) {
            mostrarAviso("O preço precisa ser maior que zero.");
            return;
        }

        boolean sucesso;
        if (produtoSelecionado == null) {
            // Produto novo
            sucesso = ProdutoDAO.inserir(new Produto(nome, categoria, preco));
        } else {
            // Produto que já existe: atualiza
            Produto editado = new Produto(produtoSelecionado.getId(), nome, categoria, preco, true);
            sucesso = ProdutoDAO.atualizar(editado);
        }

        if (sucesso) {
            recarregarTabela();
            limparFormulario();
            labelMensagem.setText("Produto salvo com sucesso!");
        } else {
            mostrarAviso("Não foi possível salvar. Veja o erro no console.");
        }
    }

    private void remover() {
        if (produtoSelecionado == null) {
            mostrarAviso("Clique em um produto da tabela para remover.");
            return;
        }

        // Pergunta antes de remover
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Remover produto");
        confirmacao.setHeaderText("Remover \"" + produtoSelecionado.getNome() + "\"?");
        confirmacao.setContentText("Ele deixará de aparecer na tela de vendas. As vendas antigas serão mantidas.");

        Optional<ButtonType> resposta = confirmacao.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.OK) {
            if (ProdutoDAO.desativar(produtoSelecionado.getId())) {
                recarregarTabela();
                limparFormulario();
                labelMensagem.setText("Produto removido.");
            } else {
                mostrarAviso("Não foi possível remover. Veja o erro no console.");
            }
        }
    }

    private void mostrarAviso(String texto) {
        labelMensagem.setText(texto);
    }
}
