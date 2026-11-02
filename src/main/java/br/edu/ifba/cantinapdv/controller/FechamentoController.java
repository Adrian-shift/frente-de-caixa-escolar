package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.RelatorioDAO;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FechamentoController {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private DatePicker datePicker;
    @FXML
    private Button btnExportar;

    @FXML
    private TableView<LinhaResumo> tabelaPagamentos;
    @FXML
    private TableColumn<LinhaResumo, String> colMetodo;
    @FXML
    private TableColumn<LinhaResumo, Integer> colQtdVendas;
    @FXML
    private TableColumn<LinhaResumo, Double> colTotalMetodo;

    @FXML
    private Label labelTotalDia;

    @FXML
    private TableView<LinhaResumo> tabelaProdutos;
    @FXML
    private TableColumn<LinhaResumo, String> colProduto;
    @FXML
    private TableColumn<LinhaResumo, Integer> colQtdProduto;
    @FXML
    private TableColumn<LinhaResumo, Double> colTotalProduto;

    @FXML
    private Label labelMensagem;

    @FXML
    public void initialize() {

        tabelaPagamentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaProdutos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Liga cada coluna a um dado da LinhaResumo (getDescricao, getQuantidade, getTotal)
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colQtdVendas.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colTotalMetodo.setCellValueFactory(new PropertyValueFactory<>("total"));

        colProduto.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colQtdProduto.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colTotalProduto.setCellValueFactory(new PropertyValueFactory<>("total"));

        formatarMoeda(colTotalMetodo);
        formatarMoeda(colTotalProduto);

        // Começa mostrando o dia de hoje
        datePicker.setValue(LocalDate.now());

        // Sempre que a data mudar, atualiza os números
        datePicker.valueProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                atualizar();
            }
        });

        btnExportar.setOnAction(e -> exportarCsv());

        atualizar();
    }

    // Faz as colunas de total aparecerem como "R$ 12,50"
    private void formatarMoeda(TableColumn<LinhaResumo, Double> coluna) {
        coluna.setCellFactory(c -> new TableCell<LinhaResumo, Double>() {
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
    }

    // Busca os dados do dia escolhido e preenche as tabelas
    private void atualizar() {
        LocalDate data = datePicker.getValue();
        if (data == null) {
            return;
        }

        tabelaPagamentos.getItems().setAll(RelatorioDAO.resumoPorPagamento(data));
        tabelaProdutos.getItems().setAll(RelatorioDAO.resumoPorProduto(data));

        double totalDia = 0.0;
        for (LinhaResumo linha : tabelaPagamentos.getItems()) {
            totalDia += linha.getTotal();
        }
        labelTotalDia.setText(String.format("R$ %.2f", totalDia));
        labelMensagem.setText("");
    }

    // Salva o fechamento do dia em um arquivo CSV (abre no Excel / LibreOffice)
    private void exportarCsv() {
        LocalDate data = datePicker.getValue();
        if (data == null) {
            return;
        }

        FileChooser escolhedor = new FileChooser();
        escolhedor.setTitle("Salvar fechamento do caixa");
        escolhedor.setInitialFileName("fechamento-" + data + ".csv");
        escolhedor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo CSV", "*.csv"));

        File arquivo = escolhedor.showSaveDialog(btnExportar.getScene().getWindow());
        if (arquivo == null) {
            return; // a pessoa cancelou
        }

        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {

            escritor.write("\uFEFF"); // faz o Excel entender os acentos

            escritor.write("Fechamento do caixa;" + data.format(FORMATO_DATA) + "\r\n");
            escritor.write("\r\n");

            escritor.write("Forma de pagamento;Nº de vendas;Total (R$)\r\n");
            double totalDia = 0.0;
            for (LinhaResumo linha : tabelaPagamentos.getItems()) {
                escritor.write(limpar(linha.getDescricao()) + ";" + linha.getQuantidade() + ";"
                        + String.format(PT_BR, "%.2f", linha.getTotal()) + "\r\n");
                totalDia += linha.getTotal();
            }
            escritor.write("TOTAL DO DIA;;" + String.format(PT_BR, "%.2f", totalDia) + "\r\n");
            escritor.write("\r\n");

            escritor.write("Produto;Quantidade;Total (R$)\r\n");
            for (LinhaResumo linha : tabelaProdutos.getItems()) {
                escritor.write(limpar(linha.getDescricao()) + ";" + linha.getQuantidade() + ";"
                        + String.format(PT_BR, "%.2f", linha.getTotal()) + "\r\n");
            }

            labelMensagem.setText("Arquivo salvo em: " + arquivo.getAbsolutePath());

        } catch (IOException e) {
            labelMensagem.setText("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    // O ";" separa as colunas no CSV, então não pode aparecer dentro de um nome
    private String limpar(String texto) {
        return texto.replace(";", ",");
    }
}
