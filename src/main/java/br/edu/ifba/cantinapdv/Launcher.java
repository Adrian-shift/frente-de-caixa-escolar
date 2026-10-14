package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.ConexaoBanco;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Garante que o banco e as tabelas existem antes de abrir a tela
        ConexaoBanco.inicializarBanco();

        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("CaixaView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

        primaryStage.setTitle("CantinaFast - PDV IFBA Irecê");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
