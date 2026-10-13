package br.edu.ifba.cantinapdv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Carrega o arquivo visual que acabamos de criar
        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("CaixaView.fxml"));

        // Define o tamanho da janela principal
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

        // Configura a janela
        primaryStage.setTitle("CantinaFast - PDV IFBA Irecê");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
