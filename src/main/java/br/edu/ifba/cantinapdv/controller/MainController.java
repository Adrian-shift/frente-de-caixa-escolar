package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.BackupBanco;
import br.edu.ifba.cantinapdv.dao.Log;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.nio.file.Path;

public class MainController {

    @FXML
    private StackPane areaConteudo;

    @FXML
    private ToggleGroup grupoMenu;

    @FXML
    private ToggleButton btnVenda;
    @FXML
    private ToggleButton btnEncomendas;
    @FXML
    private ToggleButton btnFechamento;
    @FXML
    private ToggleButton btnVendasDia;
    @FXML
    private ToggleButton btnProdutos;

    @FXML
    private Button btnBackup;

    // A tela de vendas é carregada uma vez só e guardada,
    // para o carrinho não se perder quando a atendente visita outra área.
    private Parent telaVenda;
    private CaixaController controladorVenda;

    @FXML
    public void initialize() {
        // Impede "desmarcar" o botão da área atual ao clicar nele de novo
        grupoMenu.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null && antigo != null) {
                antigo.setSelected(true);
            }
        });

        // Cada botão abre a sua área
        btnVenda.setOnAction(e -> mostrarVenda());
        btnEncomendas.setOnAction(e -> mostrarTela("EncomendasView.fxml"));
        btnFechamento.setOnAction(e -> mostrarTela("FechamentoView.fxml"));
        btnVendasDia.setOnAction(e -> mostrarTela("VendasDiaView.fxml"));
        btnProdutos.setOnAction(e -> mostrarTela("ProdutosView.fxml"));

        btnBackup.setOnAction(e -> fazerBackupManual());

        // Começa na tela de vendas
        btnVenda.setSelected(true);
        mostrarVenda();
    }

    // Mostra a tela de vendas (carrega só na primeira vez; depois reaproveita)
    private void mostrarVenda() {
        try {
            if (telaVenda == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/edu/ifba/cantinapdv/CaixaView.fxml"));
                telaVenda = loader.load();
                controladorVenda = loader.getController();
            }

            // Atualiza os botões de produto (caso algo tenha mudado em Gerenciar Produtos).
            // O carrinho NÃO é mexido.
            controladorVenda.atualizarProdutos();

            areaConteudo.getChildren().setAll(telaVenda);
        } catch (IOException ex) {
            mostrarErroDeTela(ex);
        }
    }

    // Mostra qualquer outra tela, recarregando do zero (assim os dados sempre estão atuais)
    private void mostrarTela(String arquivoFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/edu/ifba/cantinapdv/" + arquivoFxml));
            Parent tela = loader.load();
            areaConteudo.getChildren().setAll(tela);
        } catch (IOException ex) {
            mostrarErroDeTela(ex);
        }
    }

    // Grava o erro no log e avisa na tela (antes a tela simplesmente ficava em branco)
    private void mostrarErroDeTela(IOException ex) {
        Log.erro("Erro ao abrir uma tela do sistema", ex);

        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Erro");
        alerta.setHeaderText("Não foi possível abrir esta tela.");
        alerta.setContentText("Detalhes no arquivo de log:\n" + Log.getArquivo());
        alerta.showAndWait();
    }

    // Faz o backup na hora e avisa onde o arquivo foi salvo
    private void fazerBackupManual() {
        Path arquivo = BackupBanco.fazerBackup();

        Alert alerta = new Alert(arquivo != null ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alerta.setTitle("Backup");
        if (arquivo != null) {
            alerta.setHeaderText("Backup feito com sucesso!");
            alerta.setContentText("Arquivo salvo em:\n" + arquivo);
        } else {
            alerta.setHeaderText("Não foi possível fazer o backup.");
            alerta.setContentText("Detalhes no arquivo de log:\n" + Log.getArquivo());
        }
        alerta.showAndWait();
    }
}