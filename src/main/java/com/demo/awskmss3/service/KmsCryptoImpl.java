package com.demo.awskmss3.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@Service
public class KmsCryptoImpl implements Crypto {

	final static String keyArn = "arn:aws:kms:us-east-2:779352748365:key/4cefe317-592f-4bd1-a5fe-d081640209f9";
	final static String roleArn = "arn:aws:iam::779352748365:role/kai-test-ec2";

	private static final String ROLE_SESSION_NAME = "demo-runner";

	@Override
	public byte[] encrypt(final byte[] toEncrypt) {
		final AwsCrypto crypto = AwsCrypto.builder().build();
		final BasicSessionCredentials awsBasicCredentials = kmsSessionCredentials();
		final KmsMasterKeyProvider keyProvider = KmsMasterKeyProvider.builder()
				.withCredentials(awsBasicCredentials)
				.buildStrict(kmsCustomerMasterKey());
		return crypto.encryptData(keyProvider, toEncrypt).getResult();
	}

	@Override
	public void encrypt(final InputStream plainIn, final OutputStream cipherOut) {
		// TODO: implement stub.
	}

	@Override
	public byte[] decrypt(final byte[] cipherBytes) {
		final AwsCrypto crypto = AwsCrypto.builder().build();
		final BasicSessionCredentials sessionCredentials = kmsSessionCredentials();
		final KmsMasterKeyProvider masterKeyProvider = KmsMasterKeyProvider.builder()
				.withCredentials(sessionCredentials)
				.buildStrict(kmsCustomerMasterKey());
		return crypto.decryptData(masterKeyProvider, cipherBytes).getResult();
	}

	@Override
	public void decrypt(final InputStream cipherIn, final OutputStream plainOut) {
		// TODO: implement stub.
	}

	/**
	 * Retrieve the STS token stored along with the user's authentication.
	 */
	private BasicSessionCredentials kmsSessionCredentials() {
		Duration duration = Duration.ofSeconds(900); // 15 mins
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

		return new BasicSessionCredentials(assumeRoleResponse.credentials().accessKeyId(),
				assumeRoleResponse.credentials().secretAccessKey(), assumeRoleResponse.credentials().sessionToken());
	}

	/**
	 * Retrieve the Customer Master Key from user's authentication.
	 */
	private String kmsCustomerMasterKey() {
//		final AuthenticationIdentityContext identityContext = currentUserIdentity();
//		if (identityContext == null) {
//			return null;
//		}
//		return identityContext.getCustomerMasterKey();
		return keyArn;
	}
}
