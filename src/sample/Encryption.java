package sample;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

public class Encryption {

    public static byte[] rsaEnc(byte[] msg, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(msg);
    }


    public static class aesEnc {
        private KeyGenerator keyGenerator;
        private Key key;
        private Cipher cipher;

        public aesEnc() throws NoSuchAlgorithmException, NoSuchPaddingException {
            this.keyGenerator = KeyGenerator.getInstance("AES");
            this.cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            this.key = keyGenerator.generateKey();
        }

        public Cipher getCipher() {
            return cipher;
        }

        public Key getKey() {
            return key;
        }
    }

}
