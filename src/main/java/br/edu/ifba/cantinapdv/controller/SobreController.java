package br.edu.ifba.cantinapdv.controller;

import br.edu.ifba.cantinapdv.dao.Caminhos;
import br.edu.ifba.cantinapdv.dao.Log;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SobreController {

    // Mantenha igual ao --app-version do empacotar.bat quando mudar de versão
    private static final String VERSAO = "1.0.0";

    @FXML
    private Label labelVersao;
    @FXML
    private Label labelAmbiente;
    @FXML
    private Label labelPastaDados;

    @FXML
    public void initialize() {
        labelVersao.setText("Versão " + VERSAO);

        labelAmbiente.setText("Java " + System.getProperty("java.version")
                + "   |   JavaFX " + System.getProperty("javafx.version", "?"));

        labelPastaDados.setText("Pasta dos dados e backups: " + Caminhos.PASTA_DADOS
                + "\nArquivo de log de erros: " + Log.getArquivo());
    }
}