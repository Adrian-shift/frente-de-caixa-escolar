package br.edu.ifba.cantinapdv.dao;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    // O log fica em C:\Users\<usuario>\CantinaFast\logs\cantinafast.log
    private static final Path PASTA_LOGS = Caminhos.PASTA_DADOS.resolve("logs");
    private static final Path ARQUIVO = PASTA_LOGS.resolve("cantinafast.log");

    // Passou de ~1 MB, o arquivo é guardado como "anterior" e um novo começa
    private static final long TAMANHO_MAXIMO = 1_000_000;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Caminho do arquivo de log (para mostrar nos avisos)
    public static Path getArquivo() {
        return ARQUIVO.toAbsolutePath();
    }

    public static void info(String mensagem) {
        escrever("INFO", mensagem, null);
    }

    public static void erro(String mensagem) {
        escrever("ERRO", mensagem, null);
    }

    // Versão com a causa do erro: grava também o "rastro" completo, útil para achar o problema
    public static void erro(String mensagem, Throwable causa) {
        escrever("ERRO", mensagem, causa);
    }

    private static synchronized void escrever(String nivel, String mensagem, Throwable causa) {
        String linha = LocalDateTime.now().format(FORMATO) + " [" + nivel + "] " + mensagem;

        // Também mostra no console (útil quando roda pelo IntelliJ)
        if (nivel.equals("ERRO")) {
            System.err.print(linha + System.lineSeparator());
        } else {
            System.out.print(linha + System.lineSeparator());
        }

        try {
            Files.createDirectories(PASTA_LOGS);
            girarSeNecessario();

            StringBuilder texto = new StringBuilder(linha).append(System.lineSeparator());
            if (causa != null) {
                StringWriter rastro = new StringWriter();
                causa.printStackTrace(new PrintWriter(rastro));
                texto.append(rastro).append(System.lineSeparator());
            }

            Files.write(ARQUIVO, texto.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            System.err.print("Não foi possível gravar o log: " + e.getMessage() + System.lineSeparator());
        }
    }

    private static void girarSeNecessario() throws IOException {
        if (Files.exists(ARQUIVO) && Files.size(ARQUIVO) > TAMANHO_MAXIMO) {
            Files.move(ARQUIVO, PASTA_LOGS.resolve("cantinafast-anterior.log"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}