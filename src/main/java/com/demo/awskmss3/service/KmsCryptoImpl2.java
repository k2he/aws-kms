package com.demo.awskmss3.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@Service
public class KmsCryptoImpl2 implements Crypto {

	final static String keyArn = "arn:aws:kms:us-east-2:779352748365:key/4cefe317-592f-4bd1-a5fe-d081640209f9";
	final static String roleArn = "arn:aws:iam::779352748365:role/kai-test-ec2";

	private static final String ROLE_SESSION_NAME = "demo-runner";

	public KmsClient kmsClient;
	
	
	@Override
	public byte[] encrypt(final byte[] toEncrypt) {
		KmsClient kmsClient = KmsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(kmsSessionCredentials())).region(Region.US_EAST_2)
				.build();

		SdkBytes blob = SdkBytes.fromByteArray(toEncrypt);
		EncryptRequest encryptRequest = EncryptRequest.builder().keyId(keyArn).plaintext(blob).build();

		EncryptResponse resp = kmsClient.encrypt(encryptRequest);
		System.out.println("Encryption Text: " + resp.ciphertextBlob());

		SdkBytes byteResult = resp.ciphertextBlob();
		return byteResult.asByteArray();
	}

	@Override
	public void encrypt(final InputStream plainIn, final OutputStream cipherOut) {
		// TODO: implement stub.
	}

	@Override
	public byte[] decrypt(final byte[] cipherBytes) {
		KmsClient kmsClient = KmsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(kmsSessionCredentials())).region(Region.US_EAST_2)
				.build();
		
		SdkBytes blob = SdkBytes.fromByteArray(cipherBytes);
		
		DecryptRequest decryptRequest = DecryptRequest.builder().ciphertextBlob(blob).build();
        DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);

        var byteResponse = decryptResponse.plaintext().asByteArray();
        String s = new String(byteResponse, StandardCharsets.UTF_8);
        System.out.println("Decryption Text: " + s);
        return byteResponse;
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
