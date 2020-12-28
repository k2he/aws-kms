package com.demo.awskmss3.service;

import lombok.extern.slf4j.Slf4j;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
public class KmsServiceSTS2 {
    final static String keyArn = "arn:aws:kms:us-east-2:779352748365:key/4cefe317-592f-4bd1-a5fe-d081640209f9";
    final static String roleArn = "arn:aws:iam::779352748365:role/kai-test-ec2";
    
    private static final byte[] EXAMPLE_DATA = "Hello World 11".getBytes(StandardCharsets.UTF_8);

    private static final String ROLE_SESSION_NAME = "demo-runner";
    
    public StsClient stsClient;
    public KmsClient kmsClient;
    
    public KmsServiceSTS2() {
        log.info("---- Start init KmsServiceSTS 2 ---");
        
        AwsSessionCredentials kmsAwsCreds = assumeRole(roleArn, Duration.ofSeconds(900));
        
        kmsClient = KmsClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(kmsAwsCreds))
                .region(Region.US_EAST_2).build();
        encryptAndDecrypt();
        //
    }

    public void encryptAndDecrypt() {
        SdkBytes blob = SdkBytes.fromByteArray(EXAMPLE_DATA);
        EncryptRequest encryptRequest = EncryptRequest.builder()
                .keyId(keyArn).plaintext(blob).build();

        EncryptResponse resp = kmsClient.encrypt(encryptRequest);
        System.out.println("Encryption Text: " + resp.ciphertextBlob());

        DecryptRequest decryptRequest = DecryptRequest.builder().ciphertextBlob(resp.ciphertextBlob()).build();
        DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);

        var byteResponse = decryptResponse.plaintext().asByteArray();
        String s = new String(byteResponse, StandardCharsets.UTF_8);
        System.out.println("Decryption Text: " + s);
    }
    
    
    /**
     * Assume role using an ARN of the other role.
     */
    public static AwsSessionCredentials assumeRole(final String roleArn, final Duration duration) {
      final StsClient stsClient = StsClient.builder().region(Region.US_EAST_2).build();
      final AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
    		  .roleArn(roleArn)
          .roleSessionName(ROLE_SESSION_NAME)
          .durationSeconds((int) duration.getSeconds()).build();
      final AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(roleRequest);
      return AwsSessionCredentials.create(
    		  assumeRoleResponse.credentials().accessKeyId(), assumeRoleResponse.credentials().secretAccessKey(),
    		  assumeRoleResponse.credentials().sessionToken());
    }
}
