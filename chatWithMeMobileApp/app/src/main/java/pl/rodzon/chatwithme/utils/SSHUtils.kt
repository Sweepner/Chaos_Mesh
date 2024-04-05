package pl.rodzon.chatwithme.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SSHUtils {

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun generatePrivateAndPublicKey() {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(1024)
            val pair = keyGen.generateKeyPair()
            val privateKey: String = Base64.getEncoder().encodeToString(pair.private.encoded)
            val publicKey: String = Base64.getEncoder().encodeToString(pair.public.encoded)

            ChatContext.setPrivateKey(privateKey)
            ChatContext.setPublicKey(publicKey)
        }

        // convert String publickey to Key object
        @RequiresApi(Build.VERSION_CODES.O)
        @Throws(GeneralSecurityException::class, IOException::class)
        fun loadPublicKey(stored: String): Key? {
            try {
                val data: ByteArray = Base64.getDecoder().decode(stored.toByteArray())
                val spec = X509EncodedKeySpec(data)
                val fact = KeyFactory.getInstance("RSA")
                return fact.generatePublic(spec)
            } catch (e: java.lang.Exception) {
                println(e.message)
                return null
            }
        }

        // Encrypt using publickey
        @RequiresApi(Build.VERSION_CODES.O)
        @Throws(Exception::class)
        fun encryptMessage(plainText: String, publickey: String): String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, loadPublicKey(publickey))
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray()))
        }

        // Decrypt using privatekey
        @RequiresApi(Build.VERSION_CODES.O)
        /*@Throws(Exception::class)*/
        fun decryptMessage(encryptedText: String?, privatekey: String):
                String {
            try {
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.DECRYPT_MODE, loadPrivateKey(privatekey))
                val decryptedMessage = String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)))
                ChatContext.setIsWrongPrivateKey(false)
                return decryptedMessage
            } catch (e: Exception) {
                ChatContext.setIsWrongPrivateKey(true)
                return encryptedText!!
            }
        }

        // Convert String private key to privateKey object
        @RequiresApi(Build.VERSION_CODES.O)
        @Throws(GeneralSecurityException::class)
        fun loadPrivateKey(key64: String): PrivateKey {
            val clear: ByteArray = Base64.getDecoder().
            decode(key64.toByteArray())
            val keySpec = PKCS8EncodedKeySpec(clear)
            val fact = KeyFactory.getInstance("RSA")
            val priv = fact.generatePrivate(keySpec)
            Arrays.fill(clear, 0.toByte())
            return priv
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @Throws(Exception::class)
        fun encryptImage(data: ByteArray, publicKey: String): ByteArray {

            // Encrypt the data with the symmetric key (AES)
            val cipherAES = Cipher.getInstance("AES")
            cipherAES.init(Cipher.ENCRYPT_MODE, loadPublicKey(publicKey))
            val encryptedData = cipherAES.doFinal(data)

            // Encrypt the symmetric key with the RSA public key
            val cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipherRSA.init(Cipher.ENCRYPT_MODE, loadPublicKey(publicKey))
            val encryptedSymmetricKey = cipherRSA.doFinal(loadPublicKey(publicKey)!!.encoded)

            // Concatenate the encrypted symmetric key and the encrypted data
            val result = ByteArray(encryptedSymmetricKey.size + encryptedData.size)
            System.arraycopy(encryptedSymmetricKey, 0, result, 0, encryptedSymmetricKey.size)
            System.arraycopy(encryptedData, 0, result, encryptedSymmetricKey.size, encryptedData.size)

            return result
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @Throws(Exception::class)
        fun decryptEncryptedImage(encryptedData: ByteArray?, privateKey: String): ByteArray {
            val cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipherRSA.init(Cipher.DECRYPT_MODE, loadPrivateKey(privateKey))

            // Separate the encrypted symmetric key and the encrypted data
            val encryptedSymmetricKeySize = 128 // Assuming a 1024-bit RSA key
            val encryptedSymmetricKey = ByteArray(encryptedSymmetricKeySize)
            val encryptedImageData = ByteArray(encryptedData!!.size - encryptedSymmetricKeySize)

            System.arraycopy(encryptedData, 0, encryptedSymmetricKey, 0, encryptedSymmetricKeySize)
            System.arraycopy(encryptedData, encryptedSymmetricKeySize, encryptedImageData, 0, encryptedImageData.size)

            // Decrypt the symmetric key with the private key
            val decryptedSymmetricKey = cipherRSA.doFinal(encryptedSymmetricKey)

            // Reconstruct the SecretKey from the decrypted symmetric key bytes
            val secretKey: SecretKey = SecretKeySpec(decryptedSymmetricKey, 0, decryptedSymmetricKey.size, "AES")

            // Decrypt the actual image data with the symmetric key
            val cipherAES = Cipher.getInstance("AES")
            cipherAES.init(Cipher.DECRYPT_MODE, secretKey)
            return cipherAES.doFinal(encryptedImageData)
        }
    }
}