package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.Log;
import br.edu.ifba.cantinapdv.dao.RelatorioPeriodoDAO;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
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
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RelatoriosController {

    // Período máximo (mantém o gráfico legível)
    private static final int MAXIMO_DIAS = 92;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_CURTO = DateTimeFormatter.ofPattern("dd/MM");

    @FXML
    private DatePicker dataInicial;
    @FXML
    private DatePicker dataFinal;

    @FXML
    private Button btnHoje;
    @FXML
    private Button btn7Dias;
    @FXML
    private Button btnMesAtual;
    @FXML
    private Button btnMesPassado;
    @FXML
    private Button btnExportar;

    @FXML
    private Label labelTotal;
    @FXML
    private Label labelQtdVendas;
    @FXML
    private Label labelTicketMedio;
    @FXML
    private Label labelCanceladas;
    @FXML
    private Label labelMensagem;

    @FXML
    private BarChart<String, Number> graficoDias;

    @FXML
    private TableView<LinhaResumo> tabelaPagamentos;
    @FXML
    private TableColumn<LinhaResumo, String> colMetodo;
    @FXML
    private TableColumn<LinhaResumo, Integer> colQtdVendas;
    @FXML
    private TableColumn<LinhaResumo, Double> colTotalMetodo;
    @FXML
    private TableColumn<LinhaResumo, String> colPercentual;

    @FXML
    private TableView<LinhaResumo> tabelaProdutos;
    @FXML
    private TableColumn<LinhaResumo, String> colProduto;
    @FXML
    private TableColumn<LinhaResumo, Integer> colQtdProduto;
    @FXML
    private TableColumn<LinhaResumo, Double> colTotalProduto;

    // Resultado do último relatório gerado (usado também na exportação)
    private double totalPeriodo = 0.0;
    private LinhaResumo resumo = new LinhaResumo("Vendas", 0, 0.0);
    private LinhaResumo canceladas = new LinhaResumo("Canceladas", 0, 0.0);
    private Map<LocalDate, Double> porDia = new LinkedHashMap<>();
    private boolean periodoValido = false;

    // Evita gerar o relatório duas vezes quando um atalho muda as duas datas
    private boolean ajustandoDatas = false;

    @FXML
    public void initialize() {
        tabelaPagamentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabelaProdutos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colMetodo.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colQtdVendas.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colTotalMetodo.setCellValueFactory(new PropertyValueFactory<>("total"));
        // Porcentagem de cada forma de pagamento no total do período
        colPercentual.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f%%", porcentagem(c.getValue().getTotal()))));

        colProduto.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colQtdProduto.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colTotalProduto.setCellValueFactory(new PropertyValueFactory<>("total"));

        formatarMoeda(colTotalMetodo);
        formatarMoeda(colTotalProduto);

        // O gráfico não anima nem mostra legenda (só tem uma série)
        graficoDias.setAnimated(false);
        graficoDias.setLegendVisible(false);

        dataInicial.valueProperty().addListener((obs, antigo, novo) -> {
            if (!ajustandoDatas) {
                atualizar();
            }
        });
        dataFinal.valueProperty().addListener((obs, antigo, novo) -> {
            if (!ajustandoDatas) {
                atualizar();
            }
        });

        LocalDate hoje = LocalDate.now();
        btnHoje.setOnAction(e -> definirPeriodo(hoje, hoje));
        btn7Dias.setOnAction(e -> definirPeriodo(LocalDate.now().minusDays(6), LocalDate.now()));
        btnMesAtual.setOnAction(e -> definirPeriodo(LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        btnMesPassado.setOnAction(e -> {
            LocalDate primeiro = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            definirPeriodo(primeiro, primeiro.withDayOfMonth(primeiro.lengthOfMonth()));
        });
        btnExportar.setOnAction(e -> exportarCsv());

        // Começa mostrando os últimos 7 dias
        definirPeriodo(hoje.minusDays(6), hoje);
    }

    // Faz as colunas de total aparecerem como "R$ 12,50"
    private void formatarMoeda(TableColumn<LinhaResumo, Double> coluna) {
        coluna.setCellFactory(c -> new TableCell<LinhaResumo, Double>() {
            @Override
            protected void updateItem(Double valor, boolean vazio) {
                super.updateItem(valor, vazio);
                setText(vazio || valor == null ? null : moeda(valor));
            }
        });
    }

    private String moeda(double valor) {
        return String.format("R$ %.2f", valor);
    }

    private double porcentagem(double valor) {
        return totalPeriodo > 0 ? valor * 100.0 / totalPeriodo : 0.0;
    }

    // Coloca as duas datas nos calendários e gera o relatório uma vez só
    private void definirPeriodo(LocalDate de, LocalDate ate) {
        ajustandoDatas = true;
        dataInicial.setValue(de);
        dataFinal.setValue(ate);
        ajustandoDatas = false;
        atualizar();
    }

    // Confere o período escolhido e gera o relatório
    private void atualizar() {
        LocalDate de = dataInicial.getValue();
        LocalDate ate = dataFinal.getValue();
        periodoValido = false;

        if (de == null || ate == null) {
            limparTela("Escolha as duas datas.");
            return;
        }
        if (de.isAfter(ate)) {
            limparTela("A data inicial não pode ser depois da data final.");
            return;
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > MAXIMO_DIAS) {
            limparTela("Escolha um período de no máximo " + MAXIMO_DIAS + " dias (cerca de 3 meses).");
            return;
        }

        resumo = RelatorioPeriodoDAO.resumoGeral(de, ate);
        canceladas = RelatorioPeriodoDAO.resumoCancelamentos(de, ate);
        porDia = RelatorioPeriodoDAO.vendasPorDia(de, ate);
        totalPeriodo = resumo.getTotal();

        labelTotal.setText(moeda(totalPeriodo));
        labelQtdVendas.setText(String.valueOf(resumo.getQuantidade()));
        labelTicketMedio.setText(resumo.getQuantidade() == 0 ? "—" : moeda(totalPeriodo / resumo.getQuantidade()));
        labelCanceladas.setText(canceladas.getQuantidade() + " (" + moeda(canceladas.getTotal()) + ")");

        // O total do período precisa estar certo antes das tabelas, por causa da coluna "%"
        tabelaPagamentos.getItems().setAll(RelatorioPeriodoDAO.resumoPorPagamento(de, ate));
        tabelaProdutos.getItems().setAll(RelatorioPeriodoDAO.produtosMaisVendidos(de, ate));
        montarGrafico();

        periodoValido = true;
        labelMensagem.setText("");
    }

    // Esvazia a tela e mostra o motivo
    private void limparTela(String mensagem) {
        resumo = new LinhaResumo("Vendas", 0, 0.0);
        canceladas = new LinhaResumo("Canceladas", 0, 0.0);
        porDia = new LinkedHashMap<>();
        totalPeriodo = 0.0;

        labelTotal.setText("R$ 0,00");
        labelQtdVendas.setText("0");
        labelTicketMedio.setText("—");
        labelCanceladas.setText("0");
        tabelaPagamentos.getItems().clear();
        tabelaProdutos.getItems().clear();
        graficoDias.getData().clear();

        labelMensagem.setText(mensagem);
    }

    // Uma barra para cada dia do período
    private void montarGrafico() {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Map.Entry<LocalDate, Double> dia : porDia.entrySet()) {
            serie.getData().add(new XYChart.Data<>(dia.getKey().format(FORMATO_CURTO), dia.getValue()));
        }
        graficoDias.getData().setAll(serie);
    }

    // ---------- Exportação ----------

    private String decimal(double valor) {
        return String.format(PT_BR, "%.2f", valor);
    }

    // O ";" separa as colunas no CSV, então não pode aparecer dentro de um nome
    private String limpar(String texto) {
        return texto.replace(";", ",");
    }

    private void exportarCsv() {
        if (!periodoValido) {
            labelMensagem.setText("Escolha um período válido antes de exportar.");
            return;
        }

        LocalDate de = dataInicial.getValue();
        LocalDate ate = dataFinal.getValue();

        FileChooser escolhedor = new FileChooser();
        escolhedor.setTitle("Salvar relatório de vendas");
        escolhedor.setInitialFileName("relatorio-" + de + "-a-" + ate + ".csv");
        escolhedor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo CSV", "*.csv"));

        File arquivo = escolhedor.showSaveDialog(btnExportar.getScene().getWindow());
        if (arquivo == null) {
            return; // a pessoa cancelou
        }

        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {

            escritor.write("\uFEFF"); // faz o Excel entender os acentos

            escritor.write("Relatório de vendas;" + de.format(FORMATO_DATA) + " a " + ate.format(FORMATO_DATA) + "\r\n");
            escritor.write("\r\n");

            escritor.write("Resumo;Valor\r\n");
            escritor.write("Total vendido (R$);" + decimal(totalPeriodo) + "\r\n");
            escritor.write("Nº de vendas;" + resumo.getQuantidade() + "\r\n");
            escritor.write("Ticket médio (R$);"
                    + (resumo.getQuantidade() == 0 ? "" : decimal(totalPeriodo / resumo.getQuantidade())) + "\r\n");
            escritor.write("Vendas canceladas (quantidade);" + canceladas.getQuantidade() + "\r\n");
            escritor.write("Vendas canceladas (R$);" + decimal(canceladas.getTotal()) + "\r\n");
            escritor.write("\r\n");

            escritor.write("Forma de pagamento;Nº de vendas;Total (R$);% do total\r\n");
            for (LinhaResumo linha : tabelaPagamentos.getItems()) {
                escritor.write(limpar(linha.getDescricao()) + ";" + linha.getQuantidade() + ";"
                        + decimal(linha.getTotal()) + ";" + decimal(porcentagem(linha.getTotal())) + "\r\n");
            }
            escritor.write("\r\n");

            escritor.write("Dia;Total vendido (R$)\r\n");
            for (Map.Entry<LocalDate, Double> dia : porDia.entrySet()) {
                escritor.write(dia.getKey().format(FORMATO_DATA) + ";" + decimal(dia.getValue()) + "\r\n");
            }
            escritor.write("\r\n");

            escritor.write("Produto;Quantidade;Total (R$)\r\n");
            for (LinhaResumo linha : tabelaProdutos.getItems()) {
                escritor.write(limpar(linha.getDescricao()) + ";" + linha.getQuantidade() + ";"
                        + decimal(linha.getTotal()) + "\r\n");
            }

            labelMensagem.setText("");
            Log.info("Relatório exportado: " + arquivo.getAbsolutePath());
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Arquivo salvo em:\n" + arquivo.getAbsolutePath()).showAndWait();

        } catch (IOException e) {
            Log.erro("Erro ao salvar o CSV do relatório", e);
            labelMensagem.setText("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }
}