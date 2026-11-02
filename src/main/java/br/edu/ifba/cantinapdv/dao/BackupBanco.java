package br.edu.ifba.cantinapdv.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupBanco {

    // Pasta dos backups (fica ao lado do cantina.db)
    private static final Path PASTA_BACKUP = Caminhos.PASTA_DADOS.resolve("backups");

    // Quantos dias de backup guardar
    private static final int MAXIMO_BACKUPS = 30;

    // Faz o backup do dia. Devolve o caminho do arquivo, ou null se deu erro.
    public static Path fazerBackup() {
        try {
            Files.createDirectories(PASTA_BACKUP);

            Path destino = PASTA_BACKUP.resolve("cantina-" + LocalDate.now() + ".db").toAbsolutePath();
            Path temporario = PASTA_BACKUP.resolve("cantina-temporario.tmp").toAbsolutePath();

            // O VACUUM INTO não aceita arquivo que já existe
            Files.deleteIfExists(temporario);

            // Gera uma cópia consistente do banco no arquivo temporário
            try (Connection conn = ConexaoBanco.conectar(); Statement stmt = conn.createStatement()) {
                stmt.execute("VACUUM INTO '" + temporario.toString().replace("'", "''") + "'");
            }

            // Só agora troca o backup do dia pelo novo (se der erro antes, o antigo continua)
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);

            removerBackupsAntigos();

            System.out.println("Backup salvo em: " + destino);
            return destino;

        } catch (IOException | SQLException e) {
            System.err.println("Erro ao fazer backup: " + e.getMessage());
            return null;
        }
    }

    // Mantém só os últimos MAXIMO_BACKUPS arquivos (o nome tem a data, então a ordem alfabética é a ordem do tempo)
    private static void removerBackupsAntigos() throws IOException {
        try (Stream<Path> arquivos = Files.list(PASTA_BACKUP)) {
            List<Path> backups = arquivos
                    .filter(p -> p.getFileName().toString().matches("cantina-\\d{4}-\\d{2}-\\d{2}\\.db"))
                    .sorted()
                    .collect(Collectors.toList());

            for (int i = 0; i < backups.size() - MAXIMO_BACKUPS; i++) {
                Files.deleteIfExists(backups.get(i));
            }
        }
    }
}