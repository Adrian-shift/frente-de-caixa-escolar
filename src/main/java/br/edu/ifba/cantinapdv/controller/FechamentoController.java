package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.CaixaDiaDAO;
import br.edu.ifba.cantinapdv.dao.Log;
import br.edu.ifba.cantinapdv.dao.RelatorioDAO;
import br.edu.ifba.cantinapdv.model.CaixaDia;
import br.edu.ifba.cantinapdv.model.LinhaResumo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
import java.util.regex.Pattern;

public class FechamentoController {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Valor em dinheiro: número com até 2 casas decimais (vírgula ou ponto)
    private static final Pattern PADRAO_VALOR = Pattern.compile("\\d+([.,]\\d{1,2})?");

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
    private Label labelCancelamentos;

    // Conferência do dinheiro na gaveta
    @FXML
    private TextField campoTrocoInicial;
    @FXML
    private Label labelDinheiroEsperado;
    @FXML
    private TextField campoDinheiroContado;
    @FXML
    private Label labelDiferenca;
    @FXML
    private Button btnSalvarConferencia;

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

    // Vendas em dinheiro do dia escolhido (sem as canceladas)
    private double totalDinheiroDia = 0.0;

    // Resultado da conferência atual, em centavos (null = ainda não dá para calcular)
    private Long esperadoCentavos = null;
    private Long contadoCentavos = null;
    private Long diferencaCentavos = null;

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

        // A conferência é recalculada enquanto a atendente digita
        campoTrocoInicial.textProperty().addListener((obs, antigo, novo) -> atualizarConferencia());
        campoDinheiroContado.textProperty().addListener((obs, antigo, novo) -> atualizarConferencia());

        btnExportar.setOnAction(e -> exportarCsv());
        btnSalvarConferencia.setOnAction(e -> salvarConferencia());

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
        totalDinheiroDia = 0.0;
        for (LinhaResumo linha : tabelaPagamentos.getItems()) {
            totalDia += linha.getTotal();
            if ("Dinheiro".equals(linha.getDescricao())) {
                totalDinheiroDia = linha.getTotal();
            }
        }
        labelTotalDia.setText(String.format("R$ %.2f", totalDia));

        LinhaResumo canceladas = RelatorioDAO.resumoCancelamentos(data);
        if (canceladas.getQuantidade() > 0) {
            labelCancelamentos.setText(String.format(
                    "Vendas canceladas neste dia: %d (R$ %.2f). Elas não entram no total acima.",
                    canceladas.getQuantidade(), canceladas.getTotal()));
        } else {
            labelCancelamentos.setText("");
        }

        carregarConferencia(data);
        labelMensagem.setText("");
    }

    // ---------- Conferência do dinheiro ----------

    // Mostra nos campos o que foi salvo para o dia (ou deixa vazio, se não houver nada)
    private void carregarConferencia(LocalDate data) {
        CaixaDia caixa = CaixaDiaDAO.buscar(data);

        if (caixa == null) {
            campoTrocoInicial.clear();
            campoDinheiroContado.clear();
        } else {
            campoTrocoInicial.setText(String.format(PT_BR, "%.2f", caixa.getTrocoInicial()));
            campoDinheiroContado.setText(caixa.getDinheiroContado() == null
                    ? "" : String.format(PT_BR, "%.2f", caixa.getDinheiroContado()));
        }
        atualizarConferencia();
    }

    private boolean vazio(String texto) {
        return texto.trim().isEmpty();
    }

    private boolean valido(String texto) {
        return PADRAO_VALOR.matcher(texto.trim()).matches();
    }

    private double converter(String texto) {
        return Double.parseDouble(texto.trim().replace(",", "."));
    }

    // Recalcula "esperado" e "diferença" com o que está digitado agora
    private void atualizarConferencia() {
        esperadoCentavos = null;
        contadoCentavos = null;
        diferencaCentavos = null;

        // Troco inicial (vazio vale 0)
        double troco = 0.0;
        if (!vazio(campoTrocoInicial.getText())) {
            if (!valido(campoTrocoInicial.getText())) {
                labelDinheiroEsperado.setText("—");
                mostrarDiferenca("Troco inicial inválido. Use números, por exemplo: 50,00", "dif-alerta");
                return;
            }
            troco = converter(campoTrocoInicial.getText());
        }

        esperadoCentavos = Math.round(troco * 100) + Math.round(totalDinheiroDia * 100);
        labelDinheiroEsperado.setText(String.format("R$ %.2f", esperadoCentavos / 100.0));

        // Dinheiro contado
        if (vazio(campoDinheiroContado.getText())) {
            mostrarDiferenca("Digite o dinheiro contado na gaveta para ver a diferença.", null);
            return;
        }
        if (!valido(campoDinheiroContado.getText())) {
            mostrarDiferenca("Valor contado inválido. Use números, por exemplo: 83,50", "dif-alerta");
            return;
        }

        contadoCentavos = Math.round(converter(campoDinheiroContado.getText()) * 100);
        diferencaCentavos = contadoCentavos - esperadoCentavos;

        if (diferencaCentavos == 0) {
            mostrarDiferenca("Confere! O dinheiro da gaveta bate com as vendas.", "dif-ok");
        } else if (diferencaCentavos > 0) {
            mostrarDiferenca(String.format("Sobram R$ %.2f", diferencaCentavos / 100.0), "dif-alerta");
        } else {
            mostrarDiferenca(String.format("Faltam R$ %.2f", -diferencaCentavos / 100.0), "dif-alerta");
        }
    }

    // Escreve a mensagem da diferença e troca a cor (classe do estilo.css; null = cor normal)
    private void mostrarDiferenca(String texto, String classe) {
        labelDiferenca.setText(texto);
        labelDiferenca.getStyleClass().removeAll("dif-ok", "dif-alerta");
        if (classe != null) {
            labelDiferenca.getStyleClass().add(classe);
        }
    }

    private void salvarConferencia() {
        LocalDate data = datePicker.getValue();
        if (data == null) {
            return;
        }

        double troco = 0.0;
        if (!vazio(campoTrocoInicial.getText())) {
            if (!valido(campoTrocoInicial.getText())) {
                labelMensagem.setText("Troco inicial inválido. Use números, por exemplo: 50,00");
                return;
            }
            troco = converter(campoTrocoInicial.getText());
        }

        Double contado = null;
        if (!vazio(campoDinheiroContado.getText())) {
            if (!valido(campoDinheiroContado.getText())) {
                labelMensagem.setText("Valor contado inválido. Use números, por exemplo: 83,50");
                return;
            }
            contado = converter(campoDinheiroContado.getText());
        }

        if (CaixaDiaDAO.salvar(data, troco, contado)) {
            Log.info(String.format("Conferência do caixa de %s salva (troco inicial R$ %.2f, contado %s, esperado %s)",
                    data, troco,
                    contado == null ? "não informado" : String.format("R$ %.2f", contado),
                    esperadoCentavos == null ? "?" : String.format("R$ %.2f", esperadoCentavos / 100.0)));
            labelMensagem.setText("Conferência do dia salva.");
        } else {
            labelMensagem.setText("Não foi possível salvar a conferência. Detalhes no arquivo de log: " + Log.getArquivo());
        }
    }

    // ---------- Exportação ----------

    private String centavosParaCsv(Long centavos) {
        return centavos == null ? "" : String.format(PT_BR, "%.2f", centavos / 100.0);
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

            escritor.write("Conferência do dinheiro;Valor (R$)\r\n");
            escritor.write("Dinheiro esperado;" + centavosParaCsv(esperadoCentavos) + "\r\n");
            escritor.write("Dinheiro contado;" + centavosParaCsv(contadoCentavos) + "\r\n");
            escritor.write("Diferença (contado - esperado);" + centavosParaCsv(diferencaCentavos) + "\r\n");
            escritor.write("\r\n");

            escritor.write("Produto;Quantidade;Total (R$)\r\n");
            for (LinhaResumo linha : tabelaProdutos.getItems()) {
                escritor.write(limpar(linha.getDescricao()) + ";" + linha.getQuantidade() + ";"
                        + String.format(PT_BR, "%.2f", linha.getTotal()) + "\r\n");
            }

            labelMensagem.setText("Arquivo salvo em: " + arquivo.getAbsolutePath());

        } catch (IOException e) {
            Log.erro("Erro ao salvar o CSV do fechamento", e);
            labelMensagem.setText("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    // O ";" separa as colunas no CSV, então não pode aparecer dentro de um nome
    private String limpar(String texto) {
        return texto.replace(";", ",");
    }
}