package com.example.bsm_notatnik;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CryptoManager {

    private final KeyStore keyStore;

    public CryptoManager() throws Exception {
        keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
    }

    private Cipher getEncryptCipher() throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        return cipher;
    }

    private Cipher getDecryptCipherForIv(byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new IvParameterSpec(iv));
        return cipher;
    }

    private SecretKey getKey() throws Exception {
        KeyStore.SecretKeyEntry existingKey = (KeyStore.SecretKeyEntry) keyStore.getEntry("secret", null);
        return (existingKey != null) ? existingKey.getSecretKey() : createKey();
    }

    private SecretKey createKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                "secret",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
        );
        return keyGenerator.generateKey();
    }




    public byte[] encrypt(byte[] bytes, OutputStream outputStream) throws Exception {
        Cipher encryptCipher = getEncryptCipher();
        byte[] encryptedBytes = encryptCipher.doFinal(bytes);
        try {
            outputStream.write(encryptCipher.getIV().length);
            outputStream.write(encryptCipher.getIV());
            outputStream.write(encryptedBytes.length);
            outputStream.write(encryptedBytes);
        } finally {
            outputStream.close();
        }
        return encryptedBytes;
    }

    public byte[] decrypt(InputStream inputStream) throws Exception {
        byte[] iv;
        byte[] encryptedBytes;
        try {
            int ivSize = inputStream.read();
            iv = new byte[ivSize];
            inputStream.read(iv);

            int encryptedBytesSize = inputStream.read();
            encryptedBytes = new byte[encryptedBytesSize];
            inputStream.read(encryptedBytes);
        } finally {
            inputStream.close();
        }

        return getDecryptCipherForIv(iv).doFinal(encryptedBytes);
    }

    private static final String ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String TRANSFORMATION = ALGORITHM + "/" + BLOCK_MODE + "/" + PADDING;
}
