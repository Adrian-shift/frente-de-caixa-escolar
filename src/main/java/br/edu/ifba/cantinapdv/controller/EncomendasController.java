package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.EncomendaDAO;
import br.edu.ifba.cantinapdv.dao.ProdutoDAO;
import br.edu.ifba.cantinapdv.model.Encomenda;
import br.edu.ifba.cantinapdv.model.Produto;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.Optional;

public class EncomendasController {

    @FXML
    private DatePicker datePicker;

    @FXML
    private TableView<Encomenda> tabelaEncomendas;
    @FXML
    private TableColumn<Encomenda, String> colCliente;
    @FXML
    private TableColumn<Encomenda, String> colProduto;
    @FXML
    private TableColumn<Encomenda, Integer> colQuantidade;
    @FXML
    private TableColumn<Encomenda, Double> colTotal;
    @FXML
    private TableColumn<Encomenda, String> colPagamento;
    @FXML
    private TableColumn<Encomenda, String> colEntrega;
    @FXML
    private TableColumn<Encomenda, String> colObservacoes;

    @FXML
    private Label labelResumo;

    @FXML
    private Button btnPagar;
    @FXML
    private Button btnEntregar;
    @FXML
    private Button btnCancelar;

    @FXML
    private TextField campoCliente;
    @FXML
    private ComboBox<Produto> comboProduto;
    @FXML
    private TextField campoQuantidade;
    @FXML
    private TextField campoObservacoes;
    @FXML
    private CheckBox chkPagoAgora;
    @FXML
    private ComboBox<String> comboPagamento;
    @FXML
    private Button btnAdicionar;

    @FXML
    private Label labelMensagem;

    @FXML
    public void initialize() {
        // Liga cada coluna a um dado da Encomenda (usa os getters)
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nomeCliente"));
        colProduto.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colPagamento.setCellValueFactory(new PropertyValueFactory<>("statusPagamento"));
        colEntrega.setCellValueFactory(new PropertyValueFactory<>("statusEntrega"));
        colObservacoes.setCellValueFactory(new PropertyValueFactory<>("observacoes"));

        // Total como "R$ 18,00"
        colTotal.setCellFactory(coluna -> new TableCell<Encomenda, Double>() {
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

        // Como o produto aparece na lista de escolha: "PF - R$ 18,00"
        comboProduto.setConverter(new StringConverter<Produto>() {
            @Override
            public String toString(Produto produto) {
                if (produto == null) {
                    return "";
                }
                return produto.getNome() + " - R$ " + String.format("%.2f", produto.getPreco());
            }

            @Override
            public Produto fromString(String texto) {
                return null;
            }
        });

        comboPagamento.getItems().addAll("Dinheiro", "Cartão", "PIX");

        carregarProdutosMarmita();

        // Começa no dia de hoje; ao mudar o dia, recarrega a lista
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                carregarEncomendas();
            }
        });

        btnAdicionar.setOnAction(e -> adicionar());
        btnPagar.setOnAction(e -> registrarPagamento());
        btnEntregar.setOnAction(e -> marcarEntregue());
        btnCancelar.setOnAction(e -> cancelar());

        carregarEncomendas();
    }

    // Só os produtos da categoria "Marmita" aparecem para escolher
    private void carregarProdutosMarmita() {
        comboProduto.getItems().clear();
        for (Produto produto : ProdutoDAO.listarAtivos()) {
            if ("Marmita".equals(produto.getCategoria())) {
                comboProduto.getItems().add(produto);
            }
        }
    }

    private void carregarEncomendas() {
        LocalDate data = datePicker.getValue();
        if (data == null) {
            return;
        }
        tabelaEncomendas.getItems().setAll(EncomendaDAO.listarPorData(data));
        atualizarResumo();
    }

    // Mostra quantas marmitas/PF existem no dia e quantas ainda faltam entregar
    private void atualizarResumo() {
        int total = 0;
        int faltam = 0;
        for (Encomenda e : tabelaEncomendas.getItems()) {
            total += e.getQuantidade();
            if (!"Entregue".equals(e.getStatusEntrega())) {
                faltam += e.getQuantidade();
            }
        }
        labelResumo.setText("Marmitas/PF no dia: " + total + "  (ainda não entregues: " + faltam + ")");
    }

    private void adicionar() {
        String cliente = campoCliente.getText().trim();
        Produto produto = comboProduto.getValue();
        String metodo = comboPagamento.getValue();
        LocalDate data = datePicker.getValue();

        if (cliente.isEmpty()) {
            aviso("Digite o nome do cliente.");
            return;
        }
        if (produto == null) {
            aviso("Escolha o produto. Se a lista estiver vazia, cadastre um produto da categoria Marmita em Gerenciar Produtos.");
            return;
        }
        if (data == null) {
            aviso("Escolha o dia da encomenda.");
            return;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(campoQuantidade.getText().trim());
        } catch (NumberFormatException e) {
            aviso("Quantidade inválida. Use um número inteiro, por exemplo: 2");
            return;
        }
        if (quantidade < 1 || quantidade > 100) {
            aviso("A quantidade precisa estar entre 1 e 100.");
            return;
        }

        boolean pagoAgora = chkPagoAgora.isSelected();
        if (pagoAgora && metodo == null) {
            aviso("Escolha a forma de pagamento.");
            return;
        }

        double total = produto.getPreco() * quantidade;
        Encomenda nova = new Encomenda(cliente, produto.getId(), quantidade, total, data,
                campoObservacoes.getText().trim());

        // Se já pagou, manda a forma de pagamento; senão, null (fica pendente)
        boolean sucesso = EncomendaDAO.inserir(nova, pagoAgora ? metodo : null);

        if (sucesso) {
            carregarEncomendas();
            limparFormulario();
            labelMensagem.setText("Encomenda cadastrada!");
        } else {
            aviso("Não foi possível salvar. Veja o erro no console.");
        }
    }

    private void registrarPagamento() {
        Encomenda selecionada = tabelaEncomendas.getSelectionModel().getSelectedItem();
        String metodo = comboPagamento.getValue();

        if (selecionada == null) {
            aviso("Clique em uma encomenda da tabela primeiro.");
            return;
        }
        if ("Pago".equals(selecionada.getStatusPagamento())) {
            aviso("Esta encomenda já está paga.");
            return;
        }
        if (metodo == null) {
            aviso("Escolha a forma de pagamento (campo no final da tela).");
            return;
        }

        if (EncomendaDAO.registrarPagamento(selecionada, metodo)) {
            carregarEncomendas();
            labelMensagem.setText("Pagamento registrado em " + metodo + ". Ele já entra no Fechamento do Caixa de hoje.");
        } else {
            aviso("Não foi possível registrar o pagamento. Veja o erro no console.");
        }
    }

    private void marcarEntregue() {
        Encomenda selecionada = tabelaEncomendas.getSelectionModel().getSelectedItem();

        if (selecionada == null) {
            aviso("Clique em uma encomenda da tabela primeiro.");
            return;
        }
        if ("Entregue".equals(selecionada.getStatusEntrega())) {
            aviso("Esta encomenda já foi entregue.");
            return;
        }
        if (!"Pago".equals(selecionada.getStatusPagamento())) {
            aviso("Registre o pagamento antes de marcar como entregue.");
            return;
        }

        if (EncomendaDAO.marcarEntregue(selecionada.getId())) {
            carregarEncomendas();
            labelMensagem.setText("Encomenda entregue.");
        } else {
            aviso("Não foi possível marcar a entrega. Veja o erro no console.");
        }
    }

    private void cancelar() {
        Encomenda selecionada = tabelaEncomendas.getSelectionModel().getSelectedItem();

        if (selecionada == null) {
            aviso("Clique em uma encomenda da tabela primeiro.");
            return;
        }
        if ("Pago".equals(selecionada.getStatusPagamento())) {
            aviso("Encomenda já paga não pode ser cancelada por aqui.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Cancelar encomenda");
        confirmacao.setHeaderText("Cancelar a encomenda de " + selecionada.getNomeCliente() + "?");
        confirmacao.setContentText("Ela será apagada da lista.");

        Optional<ButtonType> resposta = confirmacao.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.OK) {
            if (EncomendaDAO.cancelar(selecionada.getId())) {
                carregarEncomendas();
                labelMensagem.setText("Encomenda cancelada.");
            } else {
                aviso("Não foi possível cancelar. Veja o erro no console.");
            }
        }
    }

    private void limparFormulario() {
        campoCliente.clear();
        comboProduto.setValue(null);
        campoQuantidade.setText("1");
        campoObservacoes.clear();
        chkPagoAgora.setSelected(false);
        comboPagamento.setValue(null);
    }

    private void aviso(String texto) {
        labelMensagem.setText(texto);
    }
}
