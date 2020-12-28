package com.demo.awskmss3.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@Service
public class KmsCryptoImpl implements Crypto {

	final static String keyArn = "arn:aws:kms:us-east-2:779352748365:key/4cefe317-592f-4bd1-a5fe-d081640209f9";
	final static String roleArn = "arn:aws:iam::779352748365:role/kai-test-ec2";

	private static final byte[] EXAMPLE_DATA = "Hello World 11".getBytes(StandardCharsets.UTF_8);

	private static final String ROLE_SESSION_NAME = "demo-runner";

	@Override
	public byte[] encrypt(final byte[] toEncrypt) {
//		KmsClient kmsClient = KmsClient.builder()
//				.credentialsProvider(StaticCredentialsProvider.create(kmsSessionCredentials())).region(Region.US_EAST_2)
//				.build();
//
//		SdkBytes blob = SdkBytes.fromByteArray(EXAMPLE_DATA);
//		EncryptRequest encryptRequest = EncryptRequest.builder().keyId(keyArn).plaintext(blob).build();
//
//		EncryptResponse resp = kmsClient.encrypt(encryptRequest);
//		System.out.println("Encryption Text: " + resp.ciphertextBlob());

		final AwsCrypto crypto = AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
				.build();
		final KmsMasterKeyProvider keyProvider = KmsMasterKeyProvider.builder().buildStrict(keyArn);

		final Map<String, String> encryptionContext = Collections.singletonMap("ExampleContextKey",
				"ExampleContextValue");

		// 4. Encrypt the data
		final CryptoResult<byte[], KmsMasterKey> encryptResult = crypto.encryptData(keyProvider, EXAMPLE_DATA,
				encryptionContext);
		final byte[] ciphertext = encryptResult.getResult();

		String output = new String(encryptResult.getResult(), StandardCharsets.UTF_8);
        System.out.println("encryptResult: " + output);
        
        decrypt(ciphertext);
		return ciphertext;
	}

	@Override
	public void encrypt(final InputStream plainIn, final OutputStream cipherOut) {
		// TODO: implement stub.
	}

	@Override
	public byte[] decrypt(final byte[] cipherBytes) {
		final AwsCrypto crypto = AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
				.build();
		final KmsMasterKeyProvider keyProvider = KmsMasterKeyProvider.builder().buildStrict(keyArn);

		final Map<String, String> encryptionContext = Collections.singletonMap("ExampleContextKey",
				"ExampleContextValue");
		// 5. Decrypt the data
        final CryptoResult<byte[], KmsMasterKey> decryptResult = crypto.decryptData(keyProvider, cipherBytes);

        String output = new String(decryptResult.getResult(), StandardCharsets.UTF_8);
        System.out.println("decryptResult: " + output);
        return decryptResult.getResult();
//		
//		final AwsSessionCredentials sessionCredentials = kmsSessionCredentials();
//		final KmsMasterKeyProvider masterKeyProvider = KmsMasterKeyProvider.builder()
//				.withCredentials(sessionCredentials).withKeysForEncryption(kmsCustomerMasterKey()).build();
//		return crypto.decryptData(masterKeyProvider, cipherBytes).getResult();
	}

	@Override
	public void decrypt(final InputStream cipherIn, final OutputStream plainOut) {
		// TODO: implement stub.
	}

	/**
	 * Retrieve the STS token stored along with the user's authentication.
	 */
	private AwsSessionCredentials kmsSessionCredentials() {
		Duration duration = Duration.ofSeconds(900);
		final StsClient stsClient = StsClient.builder().region(Region.US_EAST_2).build();
		final AssumeRoleRequest roleRequest = AssumeRoleRequest.builder().roleArn(roleArn)
				.roleSessionName(ROLE_SESSION_NAME).durationSeconds((int) duration.getSeconds()).build();
		final AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(roleRequest);
//		final AuthenticationIdentityContext identityContext = currentUserIdentity();
//		if (identityContext == null) {
//			return null;
//		}
//		final StsToken stsToken = identityContext.getStsToken();
//		if (stsToken == null) {
//			return null;
//		}

		return AwsSessionCredentials.create(assumeRoleResponse.credentials().accessKeyId(),
				assumeRoleResponse.credentials().secretAccessKey(), assumeRoleResponse.credentials().sessionToken());
	}
}
