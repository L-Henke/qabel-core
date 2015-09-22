package de.qabel.core.crypto;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CryptoUtilsTest {
	
	private final static String SYMM_KEY_ALGORITHM = "AES";

	final CryptoUtils cu = new CryptoUtils();
	String testFileName = "src/test/java/de/qabel/core/crypto/testFile";

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void fileDecryptionTest() throws IOException, InvalidKeyException {
		SecretKeySpec key = new SecretKeySpec(Hex.decode("feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308"), SYMM_KEY_ALGORITHM);
		byte[] nonce = Hex.decode("cafebabefacedbaddecaf888");
		File testFileEnc = new File(testFileName + ".enc");
		File testFileDec = new File(testFileName + ".dec");
		
		// create encrypted file for decryption test
		cu.encryptFileAuthenticatedSymmetric(new File(testFileName), new FileOutputStream(testFileEnc), key, nonce);

		FileInputStream cipherStream = new FileInputStream(testFileEnc);
		
		cu.decryptFileAuthenticatedSymmetricAndValidateTag(cipherStream, testFileDec, key);

		try {
			assertEquals(Hex.toHexString(Files.readAllBytes(Paths.get(testFileName))),
					Hex.toHexString(Files.readAllBytes(testFileDec.toPath())));
		} finally {
			testFileEnc.delete();
			testFileDec.delete();
		}
	}

	/**
	 * Test data from "Cryptography in NaCl" paper (http://cr.yp.to/highspeed/naclcrypto-20090310.pdf)
	 */
	@Test
	public void ecPointConstructionTest() {
		byte[] randomDataForPrivateKey = Hex.decode("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a");
		byte[] expectedPublicKey = Hex.decode("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a");

		QblECKeyPair testKey = new QblECKeyPair(randomDataForPrivateKey);
		assertArrayEquals(expectedPublicKey, testKey.getPub().getKey());
	}

	/**
	 * Test data from "Cryptography in NaCl" paper (http://cr.yp.to/highspeed/naclcrypto-20090310.pdf)
	 */
	@Test
	public void dhKeyAgreementTestNaClVector() {
		QblECKeyPair aliceKey = new QblECKeyPair(Hex.decode("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"));
		QblECKeyPair bobKey = new QblECKeyPair(Hex.decode("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb"));
		byte[] expectedSharedSecret = Hex.decode("4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742");
		byte[] sharedSecAlice = aliceKey.ECDH(bobKey.getPub());
		byte[] sharedSecBob = bobKey.ECDH(aliceKey.getPub());

		assertArrayEquals(expectedSharedSecret, sharedSecAlice);
		assertArrayEquals(expectedSharedSecret, sharedSecBob);
	}

	@Test
	public void dhKeyAgreementTest() {
		QblECKeyPair aliceKey = new QblECKeyPair(Hex.decode("5AC99F33632E5A768DE7E81BF854C27C46E3FBF2ABBACD29EC4AFF517369C660"));
		QblECKeyPair bobKey = new QblECKeyPair(Hex.decode("47DC3D214174820E1154B49BC6CDB2ABD45EE95817055D255AA35831B70D3260"));
		byte[] sharedSecAlice = aliceKey.ECDH(bobKey.getPub());
		byte[] sharedSecBob = bobKey.ECDH(aliceKey.getPub());

		assertArrayEquals(sharedSecAlice, sharedSecBob);
	}

	@Test
	public void dhKeyAgreementTestRandomKeys() {
		QblECKeyPair aliceKey = new QblECKeyPair();
		QblECKeyPair bobKey = new QblECKeyPair();
		byte[] sharedSecAlice = aliceKey.ECDH(bobKey.getPub());
		byte[] sharedSecBob = bobKey.ECDH(aliceKey.getPub());

		assertArrayEquals(sharedSecAlice, sharedSecBob);
	}

	@Test
	public void noiseTest() throws InvalidKeyException, InvalidCipherTextException {
		CryptoUtils cu = new CryptoUtils();
		QblECKeyPair aliceKey = new QblECKeyPair();
		QblECKeyPair bobKey = new QblECKeyPair();
		byte[] ciphertext = cu.createBox(aliceKey, bobKey.getPub(), "n0i$e".getBytes(), 0);
		DecryptedPlaintext plaintext = cu.readBox(bobKey, ciphertext);
		assertEquals("n0i$e", new String(plaintext.getPlaintext()));
	}

	@Test
	public void noiseTestWithNullData() throws InvalidKeyException, InvalidCipherTextException {
		CryptoUtils cu = new CryptoUtils();
		QblECKeyPair aliceKey = new QblECKeyPair();
		QblECKeyPair bobKey = new QblECKeyPair();
		byte[] ciphertext = cu.createBox(aliceKey, bobKey.getPub(), null, 0);
		DecryptedPlaintext plaintext = cu.readBox(bobKey, ciphertext);
		assertEquals("", new String(plaintext.getPlaintext()));
	}

	@Test
	public void noiseTestNegativePadLength() throws InvalidKeyException, InvalidCipherTextException {
		CryptoUtils cu = new CryptoUtils();
		QblECKeyPair aliceKey = new QblECKeyPair();
		QblECKeyPair bobKey = new QblECKeyPair();
		byte[] ciphertext = cu.createBox(aliceKey, bobKey.getPub(), "n0i$e".getBytes(), -1);
		DecryptedPlaintext plaintext = cu.readBox(bobKey, ciphertext);
		assertEquals("n0i$e", new String(plaintext.getPlaintext()));
	}

	@Test
	public void noiseBoxFromGoImplementation() throws InvalidCipherTextException, InvalidKeyException {
		CryptoUtils cu = new CryptoUtils();
		String expectedPlainText = "yellow submarines";
		byte[] box = Hex.decode("539edb6df8541fb8e56c97c6a8cd061fe1c6c874a374d8501f8a285ed5ec092244178f74e77071918e3f2c3e3d2a256916c33a85f409844bbd1b749719b2f2e71e210f763928d856479e7078cb0413e1e25f3e6685caaee9d10b2a0756d7c1769ccad1ee13bcbaf1186cec727a94b01e2be042da07");
		byte[] bobKey = Hex.decode("782e3b1ea317f7f808e1156d1282b4e7d0e60e4b7c0f205a5ce804f0a1a3a155");
		QblECKeyPair qblBobKey = new QblECKeyPair(bobKey);
		DecryptedPlaintext plaintext = cu.readBox(qblBobKey, box);
		assertEquals(expectedPlainText, new String(plaintext.getPlaintext()));
	}

	@Test
	public void noiseBoxFromGoImplementationWithPadding() throws InvalidCipherTextException, InvalidKeyException {
		CryptoUtils cu = new CryptoUtils();
		String expectedPlainText = "orange submarine";
		byte[] box = Hex.decode("a63794c4f7033b9c769023f28c12390a7b89296452a4695e35a952625839ae2d9d19715ba2130a6ae49aaf0ea5ab3eacededbb7676724618abb1fe648328086ed253a75d9672540c319114c4891cc6a1356ae7a8f3c9866c704b145efaa0313c9e52f609a4f6c41070ad4741c3ef637e7b7e0a7a7b03a0261607a9");
		byte[] bobKey = Hex.decode("a0c2b2bcb68bbe50b01181bfbcbff28ee00f37e44103d3a591dbae6cd5fb9f6a");
		QblECKeyPair qblBobKey = new QblECKeyPair(bobKey);
		DecryptedPlaintext plaintext = cu.readBox(qblBobKey, box);
		assertEquals(expectedPlainText, new String(plaintext.getPlaintext()));
	}
}
