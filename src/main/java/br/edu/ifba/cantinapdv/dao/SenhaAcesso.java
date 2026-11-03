package br.edu.ifba.cantinapdv.dao;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SenhaAcesso {

    // Tamanho mínimo aceito (pode ser um PIN numérico, como 1234)
    public static final int TAMANHO_MINIMO = 4;

    private static final String CHAVE_SENHA = "senha";
    private static final int ITERACOES = 120_000;

    // Tem senha definida?
    public static boolean existeSenha() {
        return ConfiguracaoDAO.obter(CHAVE_SENHA) != null;
    }

    // Define (ou troca) a senha. Guarda "sal:hash" em Base64, nunca a senha em si.
    public static boolean definirSenha(String senha) {
        try {
            byte[] sal = new byte[16];
            new SecureRandom().nextBytes(sal);
            byte[] hash = derivar(senha.toCharArray(), sal);

            String guardado = Base64.getEncoder().encodeToString(sal)
                    + ":" + Base64.getEncoder().encodeToString(hash);
            return ConfiguracaoDAO.salvar(CHAVE_SENHA, guardado);

        } catch (GeneralSecurityException e) {
            Log.erro("Erro ao definir a senha de acesso", e);
            return false;
        }
    }

    // Confere a senha digitada. Se não houver senha definida, tudo é liberado.
    public static boolean verificar(String senhaDigitada) {
        String guardado = ConfiguracaoDAO.obter(CHAVE_SENHA);
        if (guardado == null) {
            return true;
        }

        try {
            String[] partes = guardado.split(":");
            byte[] sal = Base64.getDecoder().decode(partes[0]);
            byte[] esperado = Base64.getDecoder().decode(partes[1]);
            byte[] calculado = derivar(senhaDigitada.toCharArray(), sal);

            // Comparação que leva sempre o mesmo tempo, aceitando ou não
            return MessageDigest.isEqual(esperado, calculado);

        } catch (GeneralSecurityException | RuntimeException e) {
            Log.erro("Erro ao conferir a senha de acesso", e);
            return false;
        }
    }

    public static boolean removerSenha() {
        return ConfiguracaoDAO.remover(CHAVE_SENHA);
    }

    // Transforma a senha em um código irreversível (PBKDF2 com SHA-256)
    private static byte[] derivar(char[] senha, byte[] sal) throws GeneralSecurityException {
        PBEKeySpec especificacao = new PBEKeySpec(senha, sal, ITERACOES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao).getEncoded();
        } finally {
            especificacao.clearPassword();
        }
    }
}