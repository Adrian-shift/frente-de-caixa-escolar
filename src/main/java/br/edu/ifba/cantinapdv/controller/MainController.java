package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.BackupBanco;
import br.edu.ifba.cantinapdv.dao.Log;
import br.edu.ifba.cantinapdv.dao.SenhaAcesso;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
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
    private ToggleButton btnVendasDia;
    @FXML
    private ToggleButton btnFechamento;
    @FXML
    private ToggleButton btnProdutos;
    @FXML
    private ToggleButton btnConfiguracoes;
    @FXML
    private ToggleButton btnRelatorios;
    @FXML
    private ToggleButton btnSobre;

    @FXML
    private Button btnBackup;

    // A tela de vendas é carregada uma vez só e guardada,
    // para o carrinho não se perder quando a atendente visita outra área.
    private Parent telaVenda;
    private CaixaController controladorVenda;

    // Área em que o sistema está agora (para voltar a ela se a senha falhar)
    private ToggleButton areaAtual;

    // Depois de digitar a senha, as áreas protegidas ficam liberadas
    // até a pessoa sair delas (ao ir para uma área livre, tranca de novo).
    private boolean desbloqueado = false;

    @FXML
    public void initialize() {
        // Impede "desmarcar" o botão da área atual ao clicar nele de novo
        grupoMenu.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null && antigo != null) {
                antigo.setSelected(true);
            }
        });

        // Áreas livres
        btnVenda.setOnAction(e -> abrirArea(btnVenda, false, this::mostrarVenda));
        btnEncomendas.setOnAction(e -> abrirArea(btnEncomendas, false, () -> mostrarTela("EncomendasView.fxml")));
        btnSobre.setOnAction(e -> abrirArea(btnSobre, false, () -> mostrarTela("SobreView.fxml")));

        // Áreas protegidas (só pedem senha se existir uma senha definida)
        btnVendasDia.setOnAction(e -> abrirArea(btnVendasDia, true, () -> mostrarTela("VendasDiaView.fxml")));
        btnFechamento.setOnAction(e -> abrirArea(btnFechamento, true, () -> mostrarTela("FechamentoView.fxml")));
        btnRelatorios.setOnAction(e -> abrirArea(btnRelatorios, true, () -> mostrarTela("RelatoriosView.fxml")));
        btnProdutos.setOnAction(e -> abrirArea(btnProdutos, true, () -> mostrarTela("ProdutosView.fxml")));
        btnConfiguracoes.setOnAction(e -> abrirArea(btnConfiguracoes, true, () -> mostrarTela("ConfiguracoesView.fxml")));

        btnBackup.setOnAction(e -> fazerBackupManual());

        // Começa na tela de vendas
        areaAtual = btnVenda;
        btnVenda.setSelected(true);
        mostrarVenda();
    }

    // Abre a área pedida. Se ela for protegida e a senha não for aceita, volta para a área anterior.
    private void abrirArea(ToggleButton botao, boolean protegida, Runnable abrir) {
        if (protegida) {
            if (!liberarAcesso(botao.getText())) {
                areaAtual.setSelected(true); // senha errada ou cancelada: fica onde estava
                return;
            }
        } else {
            desbloqueado = false; // saiu das áreas protegidas: tranca de novo
        }

        areaAtual = botao;
        abrir.run();
    }

    // Devolve true se o acesso está liberado (sem senha definida, já desbloqueado ou senha correta)
    private boolean liberarAcesso(String nomeDaArea) {
        if (!SenhaAcesso.existeSenha() || desbloqueado) {
            return true;
        }

        String digitada = pedirSenha();
        if (digitada == null) {
            return false; // a pessoa cancelou
        }

        if (SenhaAcesso.verificar(digitada)) {
            desbloqueado = true;
            Log.info("Acesso liberado por senha: " + nomeDaArea);
            return true;
        }

        Log.info("Senha incorreta ao tentar abrir: " + nomeDaArea);
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.initOwner(areaConteudo.getScene().getWindow());
        alerta.setTitle("Senha incorreta");
        alerta.setHeaderText("Senha incorreta.");
        alerta.setContentText("Não foi possível abrir esta área.");
        alerta.showAndWait();
        return false;
    }

    // Janela pequena pedindo a senha (os caracteres aparecem como bolinhas). Devolve null se cancelar.
    private String pedirSenha() {
        Dialog<String> dialogo = new Dialog<>();
        dialogo.initOwner(areaConteudo.getScene().getWindow());
        dialogo.setTitle("Área protegida");
        dialogo.setHeaderText("Digite a senha para acessar esta área.");

        ButtonType entrar = new ButtonType("Entrar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(entrar, ButtonType.CANCEL);

        PasswordField campo = new PasswordField();
        campo.setPromptText("Senha");
        dialogo.getDialogPane().setContent(campo);

        // O cursor já começa no campo, e o Enter aciona o botão "Entrar"
        Platform.runLater(campo::requestFocus);

        dialogo.setResultConverter(botao -> botao == entrar ? campo.getText() : null);

        return dialogo.showAndWait().orElse(null);
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