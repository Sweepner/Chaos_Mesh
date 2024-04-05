package pl.rodzon.chatwithme.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import org.bouncycastle.util.Arrays;

import android.util.Base64;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeySaver {
    private static final int iterations = 2000;
    private static final int keyLength = 256;
    private static final SecureRandom random = new SecureRandom();
    private static KeySaver instance = null;

    public static final int REQUEST_CODE_SAVE = 1;
    public static final int REQUEST_CODE_LOAD = 2;
    private Activity activity;
    private String privateKey;
    private String username;
    private String password;

    public KeySaver(Activity activity, String privateKey, String username){
        this.activity = activity;
        this.privateKey = privateKey;
        this.username = username;
        Security.addProvider(new BouncyCastleProvider());
    }

    public KeySaver(Activity activity, String username){
        this.activity = activity;
        this.username = username;
        Security.addProvider(new BouncyCastleProvider());
    }

    public void loadKey(String password) {
        this.password = password;

        // Create and start the load file intent
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        activity.startActivityForResult(intent, REQUEST_CODE_LOAD);
    }


    public void saveKey(String password) {
        try {
            // Encrypt the key
            // Convert to base64 for easier storage
            //String encryptedKey = encryptString(privateKey, password, "mdfenujdfbesufbsefuyfgsujh");

            // Create and start the save file intent
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "ChatWithMe_" + username + "_privateKey.txt");
            activity.startActivityForResult(intent, REQUEST_CODE_SAVE);

            // Write the encrypted key to the file
            Uri uri = intent.getData();
            try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
                outputStream.write(privateKey.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String loadKeyFromFile(Uri uri) {
        try {
            // Read the encrypted key from the file
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            byte[] encryptedKeyBytes = new byte[inputStream.available()];
            inputStream.read(encryptedKeyBytes);
            String encryptedKey = new String(encryptedKeyBytes);

            // Decrypt the key
            /*String decryptedKey = decryptString(encryptedKey, password, "mdfenujdfbesufbsefuyfgsujh");
            return decryptedKey;*/
            return encryptedKey;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load key from file.");
        }
    }

    public void saveKeyToFile(Uri uri) {
        try {
            OutputStream outputStream = activity.getContentResolver().openOutputStream(uri);
            outputStream.write(privateKey.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] extractFirst16Bytes(byte[] originalArray) {
        // Check if the original array has at least 16 elements
        if (originalArray.length >= 16) {
            // Create a new array with length 16
            byte[] newArray = new byte[16];

            // Copy the first 16 elements from the original array to the new array
            System.arraycopy(originalArray, 0, newArray, 0, 16);

            return newArray;
        } else {
            // Handle the case where the original array is too short
            throw new IllegalArgumentException("Original array must have at least 16 elements.");
        }
    }

    private static boolean isLengthDivisibleBy4(String str) {
        // Use the modulo operator to check if the length is divisible by 4
        return str.length() % 4 == 0;
    }

    private static final String ALGORITHM = "AES";
    private static final byte[] keyValue =
            new byte[] { 'T', 'h', 'i', 's', 'I', 's', 'A', 'S', 'e', 'c', 'r', 'e', 't', 'K', 'e', 'y' };

    public String encryptString(String plaintext, String passphrase, String salt)
            throws Exception {
        return encrypt(plaintext, passphrase, salt);
    }

    public String decryptString(String encrypted, String passphrase, String salt)
            throws Exception {
        return decrypt(encrypted, passphrase, salt);
    }

    private KeySaver() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /*public static KeySaver getInstance() {
        if (instance == null) {
            instance = new KeySaver();
        }
        return instance;
    }*/

    private String encrypt(String plaintext, String passphrase, String salt)
            throws Exception {
        SecretKey key = generateKey(passphrase, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        byte[] ivBytes = generateIVBytes();
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivBytes),
                random);
        return Base64.encodeToString(Arrays.concatenate(ivBytes, cipher.doFinal(plaintext.getBytes(StandardCharsets.US_ASCII))), Base64.DEFAULT);
    }

    private String decrypt(String encrypted, String passphrase, String salt)
            throws Exception {
        byte[] encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT);
        SecretKey key = generateKey(passphrase, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, key,
                new IvParameterSpec(Arrays.copyOfRange(encryptedBytes, 0, 12)),
                random);
        return Base64.encodeToString(cipher.doFinal(Arrays.copyOfRange(encryptedBytes, 12, encryptedBytes.length)), Base64.DEFAULT);
    }

    private SecretKey generateKey(String passphrase, String salt)
            throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(passphrase.toCharArray(),
                salt.getBytes(), iterations, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWITHSHA256AND256BITAES-CBC-BC");
        return keyFactory.generateSecret(keySpec);
    }

    private byte[] generateIVBytes() {
        byte[] ivBytes = new byte[12];
        random.nextBytes(ivBytes);
        return ivBytes;
    }



    /*public static String encrypt(String data, String password) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
        return textEncryptor.encrypt(data);
    }

    public static String decrypt(String encryptedData, String password) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
        return textEncryptor.decrypt(encryptedData);
    }*/

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
