package br.edu.ifba.cantinapdv.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Caminhos {

    // Pasta onde ficam o banco e os backups: C:\Users\<usuario>\CantinaFast
    public static final Path PASTA_DADOS = Paths.get(System.getProperty("user.home"), "CantinaFast");

    // Roda uma vez, quando a classe é usada pela primeira vez: cria a pasta se ela não existir
    static {
        try {
            Files.createDirectories(PASTA_DADOS);
        } catch (IOException e) {
            Log.erro("Não foi possível criar a pasta de dados: " + e.getMessage());
        }
    }

    public static Path arquivoBanco() {
        return PASTA_DADOS.resolve("cantina.db");
    }
}