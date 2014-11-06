package de.qabel.core.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.*;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CryptoUtils {

	private final static CryptoUtils INSTANCE = new CryptoUtils();

	private final static String CRYPTOGRAPHIC_PROVIDER = "BC"; // BouncyCastle
	// https://github.com/Qabel/qabel-doc/wiki/Components-Crypto
	private final static String ASYM_KEY_ALGORITHM = "RSA";
	private final static String MESSAGE_DIGEST_ALGORITHM = "SHA-512";
	private final static String SIGNATURE_ALGORITHM = "RSASSA-PSS";
	private final static String RSA_CIPHER_ALGORITM = "RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING";
	private final static String HMAC_ALGORITHM = "HMac/" + "SHA512";
	private final static int RSA_SIGNATURE_SIZE_BYTE = 256;
	private final static int RSA_KEY_SIZE_BIT = 2048;
	private final static String SYMM_KEY_ALGORITHM = "AES";
	private final static String SYMM_TRANSFORMATION = "AES/CTR/NoPadding";
	private final static String SYMM_ALT_TRANSFORMATION = "AES/GCM/NoPadding";
	private final static int SYMM_IV_SIZE_BIT = 128;
	private final static int SYMM_NONCE_SIZE_BIT = 96;
	private final static int AES_KEY_SIZE_BYTE = 32;
	private final static int ENCRYPTED_AES_KEY_SIZE_BYTE = 256;

	private final static Logger logger = LogManager.getLogger(CryptoUtils.class
			.getName());

	private static final ThreadLocal<SecureRandom> secRandom = new ThreadLocal<SecureRandom>() {
		@Override
		protected SecureRandom initialValue() {
			return new SecureRandom();
		}
	};

	private static final ThreadLocal<Mac> hmac = new ThreadLocal<Mac>() {
		@Override
		protected Mac initialValue() {
			Mac hmac;
			try {
				hmac = Mac.getInstance(HMAC_ALGORITHM, CRYPTOGRAPHIC_PROVIDER);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			}
			return hmac;
		}
	};

	private static final ThreadLocal<MessageDigest> messageDigest = new ThreadLocal<MessageDigest>() {
		@Override
		protected MessageDigest initialValue() {
			MessageDigest messageDigest;
			try {
				messageDigest = MessageDigest.getInstance(
						MESSAGE_DIGEST_ALGORITHM, CRYPTOGRAPHIC_PROVIDER);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			}
			return messageDigest;
		}
	};

	private static final ThreadLocal<KeyPairGenerator> keyGen = new ThreadLocal<KeyPairGenerator>() {
		@Override
		protected KeyPairGenerator initialValue() {
			KeyPairGenerator keyGen;
			try {
				keyGen = KeyPairGenerator.getInstance(ASYM_KEY_ALGORITHM,
						CRYPTOGRAPHIC_PROVIDER);
				keyGen.initialize(RSA_KEY_SIZE_BIT);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			}
			return keyGen;
		}
	};

	private static final ThreadLocal<Cipher> symmetricCipher = new ThreadLocal<Cipher>() {
		@Override
		protected Cipher initialValue() {
			Cipher symmetricCipher;
			try {
				symmetricCipher = Cipher.getInstance(SYMM_TRANSFORMATION,
						CRYPTOGRAPHIC_PROVIDER);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			} catch (NoSuchPaddingException e) {
				logger.error("Cannot find selected padding! " + e.getMessage());
				throw new RuntimeException("Cannot find selected padding!", e);
			}
			return symmetricCipher;
		}
	};

	private static final ThreadLocal<Cipher> asymmetricCipher = new ThreadLocal<Cipher>() {
		@Override
		protected Cipher initialValue() {
			Cipher asymmetricCipher;
			try {
				asymmetricCipher = Cipher.getInstance(RSA_CIPHER_ALGORITM,
						CRYPTOGRAPHIC_PROVIDER);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			} catch (NoSuchPaddingException e) {
				logger.error("Cannot find selected padding! " + e.getMessage());
				throw new RuntimeException("Cannot find selected padding!", e);
			}
			return asymmetricCipher;
		}
	};

	private static final ThreadLocal<Cipher> gcmCipher = new ThreadLocal<Cipher>() {
		@Override
		protected Cipher initialValue() {
			Cipher gcmCipher;
			try {
				gcmCipher = Cipher.getInstance(SYMM_ALT_TRANSFORMATION,
						CRYPTOGRAPHIC_PROVIDER);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			} catch (NoSuchPaddingException e) {
				logger.error("Cannot find selected padding! " + e.getMessage());
				throw new RuntimeException("Cannot find selected padding!", e);
			}
			return gcmCipher;
		}
	};

	private static final ThreadLocal<Signature> signer = new ThreadLocal<Signature>() {
		@Override
		protected Signature initialValue() {
			Signature signer;
			try {
				signer = Signature.getInstance(SIGNATURE_ALGORITHM,
						CRYPTOGRAPHIC_PROVIDER);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Cannot find selected algorithm! "
						+ e.getMessage());
				throw new RuntimeException("Cannot find selected algorithm!", e);
			} catch (NoSuchProviderException e) {
				logger.error("Cannot find selected provider! " + e.getMessage());
				throw new RuntimeException("Cannot find selected provider!", e);
			}
			return signer;
		}
	};

	private CryptoUtils() {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static CryptoUtils getInstance() {
		return CryptoUtils.INSTANCE;
	}
	
	/**
	 * Removes all thread local variables. <b>Must</b> be called when a CryptoUtils
	 * thread is destroyed.
	 */
	public void removeThreadLocals() {
		secRandom.remove();
		hmac.remove();
		messageDigest.remove();
		symmetricCipher.remove();
		asymmetricCipher.remove();
		gcmCipher.remove();
		signer.remove();
		keyGen.remove();
	}

	/**
	 * Returns a new KeyPair
	 * 
	 * @return KeyPair
	 */
	KeyPair generateKeyPair() {
		return keyGen.get().generateKeyPair();
	}

	/**
	 * Returns a random byte array with an arbitrary size
	 * 
	 * @param numBytes
	 *            Number of random bytes
	 * @return byte[ ] with random bytes
	 */
	byte[] getRandomBytes(int numBytes) {
		byte[] ranBytes = new byte[numBytes];
		secRandom.get().nextBytes(ranBytes);
		return ranBytes;
	}

	/**
	 * Returns the SHA512 digest for a byte array
	 * 
	 * @param bytes
	 *            byte[ ] to get the digest from
	 * @return byte[ ] with SHA512 digest
	 */
	public byte[] getSHA512sum(byte[] bytes) {
		byte[] digest = messageDigest.get().digest(bytes);
		return digest;
	}

	/**
	 * Returns the SHA512 digest for a byte array
	 * 
	 * @param bytes
	 *            byte[ ] to get the digest from
	 * @return SHA512 digest as as String in the following format:
	 *         "00:11:aa:bb:..."
	 */
	public String getSHA512sumHumanReadable(byte[] bytes) {
		byte[] digest = getSHA512sum(bytes);

		StringBuilder sb = new StringBuilder(191);

		for (int i = 0; i < digest.length - 1; i++) {
			sb.append(String.format("%02x", digest[i] & 0xff));
			sb.append(":");
		}
		sb.append(String.format("%02x", digest[digest.length - 1] & 0xff));
		return sb.toString();
	}

	/**
	 * Returns the SHA512 digest for a String
	 * 
	 * @param plain
	 *            Input String
	 * @return byte[ ] with SHA512 digest
	 */
	public byte[] getSHA512sum(String plain) {
		return getSHA512sum(plain.getBytes());
	}

	/**
	 * Returns the SHA512 digest for a String
	 * 
	 * @param plain
	 *            Input String
	 * @return SHA512 digest as as String in the following format:
	 *         "00:11:aa:bb:..."
	 */
	public String getSHA512sumHumanReadable(String plain) {
		return getSHA512sumHumanReadable(plain.getBytes());
	}

	/**
	 * Create a signature over the SHA512 sum of message with signature key
	 * 
	 * @param message
	 *            Message to create signature for
	 * @param signatureKey
	 *            Signature key to sign with
	 * @return Signature over SHA512 sum of message
	 */
	private byte[] createSignature(byte[] message, QblSignKeyPair signatureKey) {
		byte[] sha512Sum = getSHA512sum(message);
		return rsaSign(sha512Sum, signatureKey);
	}

	/**
	 * Sign data with RSA
	 * 
	 * @param data
	 *            Data to sign. Usually a message digest.
	 * @param qpkp
	 *            QblPrimaryKeyPair to extract signature key from
	 * @return Signature of data
	 */
	private byte[] rsaSign(byte[] data, QblPrimaryKeyPair qpkp) {
		return rsaSign(data, qpkp.getQblSignPrivateKey());
	}

	/**
	 * Sign data with RSA
	 * 
	 * @param data
	 *            Data to sign. Usually a message digest.
	 * @param signatureKey
	 *            QblSignKeyPair to extract signature key from
	 * @return Signature of data
	 */
	private byte[] rsaSign(byte[] data, QblSignKeyPair signatureKey) {
		return rsaSign(data, signatureKey.getRSAPrivateKey());
	}

	/**
	 * Sign data with RSA
	 * 
	 * @param data
	 *            Data to sign. Usually a message digest.
	 * @param signatureKey
	 *            QblSignKeyPair to extract signature key from
	 * @return Signature of data. Can be null if error occured.
	 */
	private byte[] rsaSign(byte[] data, RSAPrivateKey signatureKey) {
		byte[] sign = null;
		try {
			signer.get().initSign(signatureKey);
			signer.get().update(data);
			sign = signer.get().sign();
		} catch (InvalidKeyException e) {
			logger.error("Invalid key!");
		} catch (SignatureException e) {
			logger.error("Signature exception!");
		}
		return sign;
	}

	/**
	 * Signs a sub-key pair with a primary key
	 * 
	 * @param qkp
	 *            Sub-key pair to sign
	 * @param qpkp
	 *            Primary key pair to sign with
	 * @return byte[ ] with the signature. Can be null.
	 */
	byte[] rsaSignKeyPair(QblKeyPair qkp, QblPrimaryKeyPair qpkp) {

		if (qkp == null || qpkp == null) {
			return null;
		}
		return rsaSign(qkp.getPublicKeyFingerprint(), qpkp.getRSAPrivateKey());
	}

	/**
	 * Validates the signature of a message. The SHA512 digest of the message is
	 * validated against the provided signature.
	 * 
	 * @param message
	 *            Message to validate signature from
	 * @param signature
	 *            Signature to validate
	 * @param signPublicKey
	 *            Public key to validate signature with
	 * @return is signature valid
	 * @throws InvalidKeyException
	 */
	private boolean validateSignature(byte[] message, byte[] signature,
			QblSignPublicKey signPublicKey) throws InvalidKeyException {
		byte[] sha512Sum = getSHA512sum(message);
		return rsaValidateSignature(sha512Sum, signature,
				signPublicKey.getRSAPublicKey());
	}

	/**
	 * Validate the RSA signature of a data.
	 * 
	 * @param data
	 *            Data to validate signature from. Usually a message digest.
	 * @param signature
	 *            Signature to validate with
	 * @param signatureKey
	 *            Public key to validate signature with
	 * @return is signature valid
	 * @throws InvalidKeyException
	 */
	private boolean rsaValidateSignature(byte[] data, byte[] signature,
			RSAPublicKey signatureKey) throws InvalidKeyException {
		boolean isValid = false;
		try {
			signer.get().initVerify(signatureKey);
			signer.get().update(data);
			isValid = signer.get().verify(signature);
		} catch (InvalidKeyException e) {
			logger.error("Invalid RSA public key!");
			throw new InvalidKeyException("Invalid RSA public key!");
		} catch (SignatureException e) {
			logger.error("Signature exception!");
		}
		return isValid;
	}

	/**
	 * Validates a signature from a sub-public key with a primary public key
	 * 
	 * @param subKey
	 *            Sub-public key to validate
	 * @param primaryKey
	 *            Primary public key to validate signature with
	 * @return is signature valid
	 * @throws InvalidKeyException
	 */
	boolean rsaValidateKeySignature(QblSubPublicKey subKey,
			QblPrimaryPublicKey primaryKey) throws InvalidKeyException {

		if (subKey == null || primaryKey == null) {
			return false;
		}
		return rsaValidateSignature(subKey.getPublicKeyFingerprint(),
				subKey.getPrimaryKeySignature(), primaryKey.getRSAPublicKey());
	}

	/**
	 * Encrypts a byte[ ] with RSA
	 * 
	 * @param message
	 *            message to encrypt
	 * @param reciPubKey
	 *            public key to encrypt with
	 * @return encrypted messsage. Can be null if error occurred.
	 * @throws InvalidKeyException
	 */
	private byte[] rsaEncryptForRecipient(byte[] message,
			QblEncPublicKey reciPubKey) throws InvalidKeyException {
		byte[] cipherText = null;
		try {
			asymmetricCipher.get().init(Cipher.ENCRYPT_MODE,
					reciPubKey.getRSAPublicKey(), secRandom.get());
			cipherText = asymmetricCipher.get().doFinal(message);
		} catch (InvalidKeyException e) {
			logger.error("Invalid RSA public key!");
			throw new InvalidKeyException("Invalid RSA public key!");
		} catch (IllegalBlockSizeException e) {
			logger.error("Illegal block size!");
		} catch (BadPaddingException e) {
			logger.error("Bad padding!");
		}
		return cipherText;
	}

	/**
	 * Decrypts a RSA encrypted ciphertext
	 * 
	 * @param cipherText
	 *            ciphertext to decrypt
	 * @param privKey
	 *            private key to decrypt with
	 * @return decrypted ciphertext, or null if undecryptable
	 * @throws InvalidKeyException
	 */
	private byte[] rsaDecrypt(byte[] cipherText, RSAPrivateKey privKey)
			throws InvalidKeyException {
		byte[] plaintext = null;
		try {
			asymmetricCipher.get().init(Cipher.DECRYPT_MODE, privKey,
					secRandom.get());
			plaintext = asymmetricCipher.get().doFinal(cipherText);
		} catch (InvalidKeyException e) {
			logger.error("Invalid RSA private key!");
			throw new InvalidKeyException("Invalid RSA private key!");
		} catch (IllegalBlockSizeException e) {
			logger.error("Illegal block size!");
		} catch (BadPaddingException e) {
			// This exception should occur if cipherText is decrypted with wrong
			// private key
			return null;
		} catch (DataLengthException e) {
			// This exception only occurs with Bouncy Castle while decrypting
			// with a wrong private key
			return null;
		}

		return plaintext;
	}

	/**
	 * Returns the encrypted byte[] of the given plaintext, i.e.
	 * ciphertext=enc(plaintext,key) The algorithm, mode and padding is set in
	 * constant SYMM_TRANSFORMATION. A random value is used for the nonce.
	 * 
	 * @param plainText
	 *            message which will be encrypted
	 * @param key
	 *            symmetric key which is used for en- and decryption
	 * @return ciphertext which is the result of the encryption
	 */
	byte[] encryptSymmetric(byte[] plainText, byte[] key) {
		return encryptSymmetric(plainText, key, null);
	}

	/**
	 * Returns the encrypted byte[] of the given plaintext, i.e.
	 * ciphertext=enc(plaintext,key,IV). The algorithm, mode and padding is set
	 * in constant SYMM_TRANSFORMATION. IV=(nonce||counter)
	 * 
	 * @param plainText
	 *            message which will be encrypted
	 * @param key
	 *            symmetric key which is used for en- and decryption
	 * @param nonce
	 *            random input that is concatenated to a counter
	 * @return ciphertext which is the result of the encryption
	 */

	byte[] encryptSymmetric(byte[] plainText, byte[] key,
			byte[] nonce) {
		ByteArrayOutputStream cipherText = new ByteArrayOutputStream();
		SecretKeySpec symmetricKey = new SecretKeySpec(key, SYMM_KEY_ALGORITHM);
		ByteArrayOutputStream ivOS = new ByteArrayOutputStream();
		IvParameterSpec iv;
		byte[] counter = new byte[(SYMM_IV_SIZE_BIT - SYMM_NONCE_SIZE_BIT) / 8];

		if (nonce == null || nonce.length != SYMM_NONCE_SIZE_BIT / 8) {
			nonce = getRandomBytes(SYMM_NONCE_SIZE_BIT / 8);
		}

		// Set counter to 1, if nonce is smaller than IV
		if (SYMM_IV_SIZE_BIT - SYMM_NONCE_SIZE_BIT > 0) {
			counter[counter.length - 1] = 1;
		}

		try {
			ivOS.write(nonce);
			ivOS.write(counter);
			cipherText.write(nonce);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		iv = new IvParameterSpec(ivOS.toByteArray());

		try {
			symmetricCipher.get()
					.init(Cipher.ENCRYPT_MODE, symmetricKey, iv);
			cipherText.write(symmetricCipher.get().doFinal(plainText));
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cipherText.toByteArray();
	}

	/**
	 * Returns the plaintext of the encrypted input
	 * plaintext=enc⁻¹(ciphertext,key) The algorithm, mode and padding is set in
	 * constant SYMM_TRANSFORMATION
	 * 
	 * @param cipherText
	 *            encrypted message which will be decrypted
	 * @param key
	 *            symmetric key which is used for en- and decryption
	 * @return plaintext which is the result of the decryption
	 */
	byte[] decryptSymmetric(byte[] cipherText, byte[] key) {
		ByteArrayInputStream bi = new ByteArrayInputStream(cipherText);
		byte[] nonce = new byte[SYMM_NONCE_SIZE_BIT / 8];
		byte[] counter = new byte[(SYMM_IV_SIZE_BIT - SYMM_NONCE_SIZE_BIT) / 8];
		byte[] encryptedPlainText = new byte[cipherText.length
				- SYMM_NONCE_SIZE_BIT / 8];
		byte[] plainText = null;
		ByteArrayOutputStream ivOS = new ByteArrayOutputStream();
		IvParameterSpec iv;
		SecretKeySpec symmetricKey;

		// Set counter to 1, if nonce is smaller than IV
		if (SYMM_IV_SIZE_BIT - SYMM_NONCE_SIZE_BIT > 0) {
			counter[counter.length - 1] = 1;
		}

		try {
			bi.read(nonce);
			ivOS.write(nonce);
			ivOS.write(counter);
			bi.read(encryptedPlainText);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		iv = new IvParameterSpec(ivOS.toByteArray());
		symmetricKey = new SecretKeySpec(key, SYMM_KEY_ALGORITHM);

		try {
			symmetricCipher.get()
					.init(Cipher.DECRYPT_MODE, symmetricKey, iv);
			plainText = symmetricCipher.get().doFinal(encryptedPlainText);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return plainText;
	}

	/**
	 * Hybrid encrypts a String message for a recipient. The String message is
	 * encrypted with a random AES key. The AES key gets RSA encrypted with the
	 * recipients public key. The cipher text gets signed.
	 * 
	 * @param message
	 *            String message to encrypt
	 * @param recipient
	 *            Recipient to encrypt message for
	 * @param signatureKey
	 *            private key to sign message with
	 * 
	 * @return hybrid encrypted String message
	 * @throws InvalidKeyException
	 */
	public byte[] encryptHybridAndSign(String message,
			QblEncPublicKey recipient, QblSignKeyPair signatureKey)
			throws InvalidKeyException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		byte[] aesKey = getRandomBytes(AES_KEY_SIZE_BYTE);

		try {
			bs.write(rsaEncryptForRecipient(aesKey, recipient));
			bs.write(encryptSymmetric(message.getBytes(), aesKey));
			bs.write(createSignature(bs.toByteArray(), signatureKey));
		} catch (IOException e) {
			logger.error("IOException while writing to ByteArrayOutputStream");
		}
		return bs.toByteArray();
	}

	/**
	 * Decrypts a hybrid encrypted String message. Before decryption, the
	 * signature over the cipher text gets validated. The AES key is decrypted
	 * using the own private key. The decrypted AES key is used to decrypt the
	 * String message
	 * 
	 * @param cipherText
	 *            hybrid encrypted String message
	 * @param privKey
	 *            private key to encrypt String message with
	 * @param signatureKey
	 *            public key to validate signature with
	 * @return decrypted String message or null if message is undecryptable or
	 *         signature is invalid
	 * @throws InvalidKeyException
	 */
	public String decryptHybridAndValidateSignature(byte[] cipherText,
			QblPrimaryKeyPair privKey, QblSignPublicKey signatureKey)
			throws InvalidKeyException {
		ByteArrayInputStream bs = new ByteArrayInputStream(cipherText);
		// TODO: Include header byte

		if (bs.available() < RSA_SIGNATURE_SIZE_BYTE) {
			logger.debug("Avaliable data is less than RSA signature size!");
			return null;
		}
		// Get RSA encrypted AES key and encrypted data and signature over the
		// RSA
		// encrypted AES key and encrypted data
		byte[] encryptedMessage = new byte[bs.available()
				- RSA_SIGNATURE_SIZE_BYTE];
		byte[] rsaSignature = new byte[RSA_SIGNATURE_SIZE_BYTE];
		try {
			bs.read(encryptedMessage);
			bs.read(rsaSignature);
		} catch (IOException e) {
			logger.error("IOException while reading from ByteArrayInputStream");
		}

		// Validate signature over RSA encrypted AES key and encrypted data
		if (!validateSignature(encryptedMessage, rsaSignature, signatureKey)) {
			logger.debug("Message signature invalid!");
			return null;
		}

		// Read RSA encrypted AES key and encryptedData
		bs = new ByteArrayInputStream(encryptedMessage);

		byte[] encryptedAesKey = new byte[ENCRYPTED_AES_KEY_SIZE_BYTE];

		if (bs.available() < ENCRYPTED_AES_KEY_SIZE_BYTE) {
			logger.debug("Avaliable data is less than encrypted AES key size");
			return null;
		}
		byte[] aesCipherText = new byte[bs.available()
				- ENCRYPTED_AES_KEY_SIZE_BYTE];

		try {
			bs.read(encryptedAesKey);
			bs.read(aesCipherText);
		} catch (IOException e) {
			logger.error("IOException while reading from ByteArrayInputStream");
			e.printStackTrace();
		}

		// Decrypt RSA encrypted AES key and decrypt encrypted data with AES key
		byte[] aesKey = rsaDecrypt(encryptedAesKey,
				privKey.getQblEncPrivateKey());
		if (aesKey != null) {
			logger.debug("Message is OK!");
			return new String(decryptSymmetric(aesCipherText, aesKey));
		}
		return null;
	}

	/**
	 * Calculates HMAC of input.
	 * 
	 * @param text
	 *            input text
	 * @param key
	 *            key for HMAC calculation
	 * @return HMAC of text under key
	 */
	public byte[] calcHmac(byte[] text, byte[] key) {
		byte[] result = null;
		try {
			hmac.get().init(new SecretKeySpec(key, HMAC_ALGORITHM));
			result = hmac.get().doFinal(text);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Simple verification of HMAC
	 * 
	 * @param text
	 *            original input text
	 * @param hmac
	 *            HMAC which will be verified
	 * @param key
	 *            key for HMAC calculation
	 * @return result of verification i.e. true/false
	 */
	public boolean validateHmac(byte[] text, byte[] hmac, byte[] key) {
		boolean validation = MessageDigest.isEqual(hmac, calcHmac(text, key));
		if (!validation) {
			logger.debug("HMAC is invalid!");
		}
		return validation;
	}

	/**
	 * Encryptes plaintext in GCM Mode to get an authenticated encryption. It's
	 * an alternative to encrypt-then-(H)MAC in CTR mode. A random value is used
	 * for the nonce.
	 * 
	 * @param plainText
	 *            Plaintext which will be encrypted
	 * @param key
	 *            Symmetric key which will be used for encryption and
	 *            authentication
	 * @return Ciphertext, in format: IV|enc(plaintext)|authentication tag
	 */
	public byte[] encryptAuthenticatedSymmetric(byte[] plainText,
			byte[] key) {
		return encryptAuthenticatedSymmetric(plainText, key, null);
	}

	/**
	 * Encryptes plaintext in GCM Mode to get an authenticated encryption. It's
	 * an alternative to encrypt-then-(H)MAC in CTR mode. It will be tested and
	 * reviewed which AE will be used.
	 * 
	 * @param plainText
	 *            Plaintext which will be encrypted
	 * @param key
	 *            Symmetric key which will be used for encryption and
	 *            authentication
	 * @param nonce
	 *            random input that is concatenated to a counter
	 * @return Ciphertext, in format: IV|enc(plaintext)|authentication tag
	 */
	public byte[] encryptAuthenticatedSymmetric(byte[] plainText,
			byte[] key, byte[] nonce) {
		SecretKeySpec symmetricKey;
		IvParameterSpec iv;
		ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

		if (nonce == null || nonce.length != SYMM_NONCE_SIZE_BIT / 8) {
			nonce = getRandomBytes(SYMM_NONCE_SIZE_BIT / 8);
		}

		try {
			cipherText.write(nonce);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		iv = new IvParameterSpec(nonce);
		symmetricKey = new SecretKeySpec(key, SYMM_KEY_ALGORITHM);

		try {
			gcmCipher.get().init(Cipher.ENCRYPT_MODE, symmetricKey, iv);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			cipherText.write(gcmCipher.get().doFinal(plainText));
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cipherText.toByteArray();
	}

	/**
	 * Decryptes ciphertext in GCM Mode and verifies the integrity and
	 * authentication. As well as encryptAuthenticatedSymmetric() it will be
	 * tested which AE will be used.
	 * 
	 * @param cipherText
	 *            Ciphertext which will be decrypted
	 * @param key
	 *            Symmetric key which will be used for decryption and
	 *            verification
	 * @return Plaintext or null if validation of authentication tag fails
	 */
	public byte[] decryptAuthenticatedSymmetricAndValidateTag(
			byte[] cipherText, byte[] key) {
		ByteArrayInputStream bi = new ByteArrayInputStream(cipherText);
		byte[] nonce = new byte[SYMM_NONCE_SIZE_BIT / 8];
		byte[] encryptedPlainText = new byte[cipherText.length
				- SYMM_NONCE_SIZE_BIT / 8];
		byte[] plainText = null;
		IvParameterSpec iv;
		SecretKeySpec symmetricKey;

		try {
			bi.read(nonce);
			bi.read(encryptedPlainText);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		iv = new IvParameterSpec(nonce);
		symmetricKey = new SecretKeySpec(key, SYMM_KEY_ALGORITHM);

		try {
			gcmCipher.get().init(Cipher.DECRYPT_MODE, symmetricKey, iv);
			plainText = gcmCipher.get().doFinal(encryptedPlainText);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO this exception is thrown if ciphertext or authentication tag
			// was modified
			logger.debug("Authentication tag is invalid!");
			return null;
		}
		return plainText;
	}
}
