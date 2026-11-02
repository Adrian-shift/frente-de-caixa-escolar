package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.Log;
import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.VendaDoDia;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class VendasDiaController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private DatePicker datePicker;
    @FXML
    private Label labelResumoDia;

    @FXML
    private TableView<VendaDoDia> tabelaVendas;
    @FXML
    private TableColumn<VendaDoDia, String> colHora;
    @FXML
    private TableColumn<VendaDoDia, String> colItens;
    @FXML
    private TableColumn<VendaDoDia, String> colPagamento;
    @FXML
    private TableColumn<VendaDoDia, Double> colTotal;
    @FXML
    private TableColumn<VendaDoDia, String> colSituacao;

    @FXML
    private Button btnCancelar;
    @FXML
    private Label labelMensagem;

    @FXML
    public void initialize() {
        tabelaVendas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Liga cada coluna a um dado da VendaDoDia (usa os getters)
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colItens.setCellValueFactory(new PropertyValueFactory<>("itens"));
        colPagamento.setCellValueFactory(new PropertyValueFactory<>("metodoPagamento"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colSituacao.setCellValueFactory(new PropertyValueFactory<>("situacao"));

        // Total como "R$ 8,00"
        colTotal.setCellFactory(coluna -> new TableCell<VendaDoDia, Double>() {
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

        // Linhas de vendas canceladas ficam em cinza (o estilo está no estilo.css)
        tabelaVendas.setRowFactory(tabela -> new TableRow<VendaDoDia>() {
            @Override
            protected void updateItem(VendaDoDia venda, boolean vazio) {
                super.updateItem(venda, vazio);
                getStyleClass().remove("linha-cancelada");
                if (!vazio && venda != null && venda.isCancelada()) {
                    getStyleClass().add("linha-cancelada");
                }
            }
        });

        // Começa no dia de hoje; ao mudar o dia, recarrega a lista
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                carregar();
            }
        });

        btnCancelar.setOnAction(e -> cancelarSelecionada());

        carregar();
    }

    private void carregar() {
        LocalDate data = datePicker.getValue();
        if (data == null) {
            return;
        }

        List<VendaDoDia> vendas = VendaDAO.listarPorData(data);
        tabelaVendas.getItems().setAll(vendas);

        int ativas = 0;
        int canceladas = 0;
        double totalAtivas = 0.0;
        for (VendaDoDia venda : vendas) {
            if (venda.isCancelada()) {
                canceladas++;
            } else {
                ativas++;
                totalAtivas += venda.getValorTotal();
            }
        }

        labelResumoDia.setText(String.format("Vendas: %d   |   Total: R$ %.2f   |   Canceladas: %d",
                ativas, totalAtivas, canceladas));
        labelMensagem.setText("");
    }

    private void cancelarSelecionada() {
        VendaDoDia venda = tabelaVendas.getSelectionModel().getSelectedItem();

        if (venda == null) {
            labelMensagem.setText("Clique em uma venda da tabela primeiro.");
            return;
        }
        if (venda.isCancelada()) {
            labelMensagem.setText("Esta venda já está cancelada.");
            return;
        }
        if (venda.isDeEncomenda()) {
            labelMensagem.setText("Esta venda é o pagamento de uma encomenda e ainda não pode ser cancelada por aqui.");
            return;
        }

        // Monta o texto de confirmação com os detalhes da venda
        StringBuilder cabecalho = new StringBuilder();
        cabecalho.append("Cancelar a venda das ").append(venda.getHora()).append("?\n\n");
        cabecalho.append(venda.getItens()).append("\n");
        cabecalho.append(String.format("R$ %.2f em %s", venda.getValorTotal(), venda.getMetodoPagamento()));
        cabecalho.append("\n\nEla deixará de contar no Fechamento do Caixa. Esta ação não pode ser desfeita.");

        if (!datePicker.getValue().equals(LocalDate.now())) {
            cabecalho.append("\n\nATENÇÃO: esta venda é de ").append(datePicker.getValue().format(FORMATO_DATA))
                    .append(". O fechamento daquele dia vai mudar.");
        }

        TextInputDialog dialogo = new TextInputDialog();
        dialogo.initOwner(btnCancelar.getScene().getWindow());
        dialogo.setTitle("Cancelar venda");
        dialogo.setHeaderText(cabecalho.toString());
        dialogo.setContentText("Motivo (opcional):");

        Optional<String> resposta = dialogo.showAndWait();
        if (!resposta.isPresent()) {
            return; // clicou em Cancelar no aviso: não faz nada
        }

        String motivo = resposta.get().trim();

        if (VendaDAO.cancelarVenda(venda.getId(), motivo)) {
            Log.info(String.format("Venda #%d cancelada (R$ %.2f em %s). Motivo: %s",
                    venda.getId(), venda.getValorTotal(), venda.getMetodoPagamento(),
                    motivo.isEmpty() ? "(não informado)" : motivo));
            carregar();
            labelMensagem.setText("Venda cancelada. Ela não entra mais no Fechamento do Caixa.");
        } else {
            labelMensagem.setText("Não foi possível cancelar a venda. Detalhes no arquivo de log: " + Log.getArquivo());
        }
    }
}