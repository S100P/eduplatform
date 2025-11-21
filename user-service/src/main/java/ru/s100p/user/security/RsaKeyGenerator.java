package ru.s100p.user.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Временный утилитарный класс для генерации RSA-ключей.
 * <p>
 * Запустите метод main() и скопируйте сгенерированные ключи из консоли.
 * После этого этот класс можно удалить.
 * </p>
 */
public class RsaKeyGenerator {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        java.security.PrivateKey privateKey = pair.getPrivate();
        java.security.PublicKey publicKey = pair.getPublic();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        System.out.println("========== СГЕНЕРИРОВАННЫЕ RSA КЛЮЧИ ==========");
        System.out.println();
        System.out.println("----- ПУБЛИЧНЫЙ КЛЮЧ (для JWKS эндпоинта) -----");
        System.out.println(publicKeyBase64);
        System.out.println();
        System.out.println("----- ПРИВАТНЫЙ КЛЮЧ (для user-service) -----");
        System.out.println(privateKeyBase64);
        System.out.println();
        System.out.println("==============================================");
        System.out.println("Инструкция: Скопируйте эти ключи и пришлите мне для добавления в application.yml.");
    }
}
