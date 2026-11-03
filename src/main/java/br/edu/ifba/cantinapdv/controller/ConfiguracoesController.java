package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.Log;
import br.edu.ifba.cantinapdv.dao.SenhaAcesso;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class ConfiguracoesController {

    @FXML
    private Label labelStatus;
    @FXML
    private VBox boxSenhaAtual;
    @FXML
    private PasswordField campoSenhaAtual;
    @FXML
    private PasswordField campoNovaSenha;
    @FXML
    private PasswordField campoRepetirSenha;
    @FXML
    private Button btnSalvarSenha;
    @FXML
    private Button btnRemoverSenha;
    @FXML
    private Label labelMensagem;

    @FXML
    public void initialize() {
        btnSalvarSenha.setOnAction(e -> salvarSenha());
        btnRemoverSenha.setOnAction(e -> removerSenha());
        atualizarTela();
    }

    // Ajusta a tela conforme exista ou não uma senha definida
    private void atualizarTela() {
        boolean existe = SenhaAcesso.existeSenha();

        labelStatus.setText(existe
                ? "Senha definida: as áreas protegidas pedem a senha."
                : "Nenhuma senha definida: qualquer pessoa acessa todas as áreas.");

        // A "senha atual" só é pedida quando já existe uma senha
        boxSenhaAtual.setVisible(existe);
        boxSenhaAtual.setManaged(existe);

        btnSalvarSenha.setText(existe ? "Alterar senha" : "Definir senha");
        btnRemoverSenha.setDisable(!existe);

        campoSenhaAtual.clear();
        campoNovaSenha.clear();
        campoRepetirSenha.clear();
    }

    private void salvarSenha() {
        boolean existe = SenhaAcesso.existeSenha();

        if (existe && !SenhaAcesso.verificar(campoSenhaAtual.getText())) {
            Log.info("Senha atual incorreta ao tentar alterar a senha de acesso");
            labelMensagem.setText("Senha atual incorreta.");
            return;
        }

        String nova = campoNovaSenha.getText();

        if (nova.length() < SenhaAcesso.TAMANHO_MINIMO) {
            labelMensagem.setText("A nova senha precisa ter pelo menos " + SenhaAcesso.TAMANHO_MINIMO + " caracteres.");
            return;
        }
        if (!nova.equals(campoRepetirSenha.getText())) {
            labelMensagem.setText("As duas senhas digitadas não são iguais.");
            return;
        }

        if (SenhaAcesso.definirSenha(nova)) {
            Log.info(existe ? "Senha de acesso alterada" : "Senha de acesso definida");
            atualizarTela();
            labelMensagem.setText("Senha salva. Guarde-a em um lugar seguro.");
        } else {
            labelMensagem.setText("Não foi possível salvar a senha. Detalhes no arquivo de log: " + Log.getArquivo());
        }
    }

    private void removerSenha() {
        if (!SenhaAcesso.verificar(campoSenhaAtual.getText())) {
            Log.info("Senha atual incorreta ao tentar remover a senha de acesso");
            labelMensagem.setText("Digite a senha atual para poder remover a senha.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Remover senha");
        confirmacao.setHeaderText("Remover a senha de acesso?");
        confirmacao.setContentText("Depois disso, qualquer pessoa poderá abrir todas as áreas do sistema.");

        Optional<ButtonType> resposta = confirmacao.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.OK) {
            if (SenhaAcesso.removerSenha()) {
                Log.info("Senha de acesso removida");
                atualizarTela();
                labelMensagem.setText("Senha removida.");
            } else {
                labelMensagem.setText("Não foi possível remover a senha. Detalhes no arquivo de log: " + Log.getArquivo());
            }
        }
    }
}