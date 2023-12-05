package com.example.bsm_notatnik;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Utility {

    protected static byte[] generateSalt(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    protected static String hashEmail(String email){
        byte[] emailSalt;
        emailSalt = getFirst16BytesOfHash(email);

        return hashCredential(email, emailSalt, 1000);
    }
    protected static String hashCredential(String credential, byte[] salt, int iterations){
        int iteratiions = iterations;
        int keyLen = 256;

        KeySpec keySpec = new PBEKeySpec(credential.toCharArray(), salt, iteratiions, keyLen);
        try{
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
            byte[] hashedCredential = secretKey.getEncoded();
            return Base64.getEncoder().encodeToString(hashedCredential);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static byte[] getFirst16BytesOfHash(String input){
        try {
            // Create MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Get the hash value by updating the digest with the input bytes
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Truncate the hash to the first 16 bytes
            byte[] truncatedHash = new byte[16];
            System.arraycopy(hashBytes, 0, truncatedHash, 0, 16);

            return truncatedHash;
        } catch (NoSuchAlgorithmException e) {
            // Handle the exception (e.g., print an error message)
            e.printStackTrace();
            return null;
        }
    }
}
