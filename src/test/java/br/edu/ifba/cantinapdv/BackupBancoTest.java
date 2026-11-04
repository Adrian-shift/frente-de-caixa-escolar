package br.edu.ifba.cantinapdv;

import br.edu.ifba.cantinapdv.dao.BackupBanco;
import br.edu.ifba.cantinapdv.dao.VendaDAO;
import br.edu.ifba.cantinapdv.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupBancoTest {

    @BeforeEach
    void preparar() {
        BancoDeTeste.preparar();
    }

    @Test
    void backupGeraUmaCopiaQueAbreEContemOsDados() throws Exception {
        Produto ficha5 = BancoDeTeste.produto("Ficha R$ 5,00");
        VendaDAO.salvarVenda(5.0, "PIX", List.of(ficha5));

        Path arquivo = BackupBanco.fazerBackup();

        assertNotNull(arquivo);
        assertTrue(Files.exists(arquivo));
        assertEquals("cantina-" + LocalDate.now() + ".db", arquivo.getFileName().toString());

        // Abre a CÓPIA e confere que a venda está dentro dela
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vendas")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}