package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.BackupBanco;
import br.edu.ifba.cantinapdv.dao.ConexaoBanco;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Garante que o banco e as tabelas existem antes de abrir a tela
        ConexaoBanco.inicializarBanco();

        // Backup ao abrir o programa
        BackupBanco.fazerBackup();

        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1250, 720);

        primaryStage.setTitle("CantinaFast - PDV IFBA Irecê");
        primaryStage.setScene(scene);

        // Ícone da janela (barra de título e barra de tarefas)
        var iconeLogo = Launcher.class.getResourceAsStream("/br/edu/ifba/cantinapdv/icones/logo.png");
        if (iconeLogo != null) {
            primaryStage.getIcons().add(new Image(iconeLogo));
        }

        // Tamanho mínimo: impede a janela de ficar tão pequena que as telas se espremem
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(650);

        // Abre ocupando toda a área de trabalho
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    // Chamado pelo JavaFX quando o programa é fechado
    @Override
    public void stop() {
        // Backup ao fechar o programa (guarda o estado final do dia)
        BackupBanco.fazerBackup();
    }

    public static void main(String[] args) {
        launch(args);
    }
}