package com.demo.awskmss3.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@Service
public class KmsS3Service {

    final static String keyArn = "arn:aws:kms:us-east-2:779352748365:key/4cefe317-592f-4bd1-a5fe-d081640209f9";
    final static String roleArn = "arn:aws:iam::779352748365:role/kai-test-ec2";

    private static final String ROLE_SESSION_NAME = "demo-s3-runner";
    private static final String fileName = "observation.json";
    
    S3Client s3Client;
//    Regions clientRegion = Regions.DEFAULT_REGION;
//    String bucketName = "kai-tester-bucket";
//    String stringObjKeyName = "*** String object key name ***";
//    String fileObjKeyName = "*** File object key name ***";
//    String fileName = "*** Path to file to upload ***";
//    
//    public boolean uploadToS3() {
//    	
//        PutObjectRequest putRequest = new PutObjectRequest(bucketName,
//                keyName, file).withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams());
//        
//        try {
//            //This code expects that you have AWS credentials set up per:
//            // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html
//            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
//                    .withRegion(clientRegion)
//                    .build();
//
//            // Upload a text string as a new object.
//            s3Client.putObject(bucketName, stringObjKeyName, "Uploaded String Object");
//
//            // Upload a file as a new object with ContentType and title specified.
//            PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, new File(fileName));
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentType("plain/text");
//            metadata.addUserMetadata("title", "someTitle");
//            request.setMetadata(metadata);
//            s3Client.putObject(request);
//        } catch (AmazonServiceException e) {
//            // The call was transmitted successfully, but Amazon S3 couldn't process 
//            // it, so it returned an error response.
//            e.printStackTrace();
//        } catch (SdkClientException e) {
//            // Amazon S3 couldn't be contacted for a response, or the client
//            // couldn't parse the response from Amazon S3.
//            e.printStackTrace();
//        }
//    }
    
    
//    /**
//     * This client is useful when dealing with encrypted data.
//     *
//     * @param kmsKeyArn Customer Master KMS Key ARN.
//     * @param stsToken  Valid STS Token.
//     * @param bucket    Bucket.
//     * @return A client which can deal with encrypted data.
//     */
//    private AmazonS3Encryption getAmazonS3ClientWithKmsKeyAndStsToken(final String kmsKeyArn, final StsToken stsToken,
//        final String bucket) {
//      final BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(stsToken.getAwsAccessKey(),
//          stsToken.getAwsSecretKey(), stsToken.getSessionToken());
//      final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(basicSessionCredentials);
//      // We need to embed a kms client because of https://github.com/aws/aws-sdk-java/issues/1386
//      final AWSKMS kms = AWSKMSClientBuilder.standard().withCredentials(credentials).build();
//      final CryptoConfiguration cryptoConfiguration = new CryptoConfiguration(CryptoMode.AuthenticatedEncryption);
//      return AmazonS3EncryptionClientBuilder
//          .standard()
//          // Will require s3:GetBucketLocation permission, has to be enabled on each service ec2 role
//          .withRegion(s3NonEncrypted.getBucketLocation(bucket))
//          .withCryptoConfiguration(cryptoConfiguration)
//          .withEncryptionMaterials(new KMSEncryptionMaterialsProvider(kmsKeyArn))
//          .withCredentials(credentials)
//          .withKmsClient(kms)
//          .withClientConfiguration(new ClientConfiguration().withMaxConnections(S3_MAX_CONNECTIONS))
//          .build();
//    }
    
    public Bucket getBucket(String bucket_name) {
        s3Client = S3Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(kmsSessionCredentials())).region(Region.US_EAST_2)
				.build();
        
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        List<Bucket> buckets = listBucketsResponse.buckets();
        for (Bucket b : buckets) {
            if (b.name().equals(bucket_name)) {
                return b;
            }
        }
        return null;
    }

    public boolean addToBucket(String bucketName) throws FileNotFoundException {
    	s3Client = S3Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(kmsSessionCredentials())).region(Region.US_EAST_2)
				.build();
    	
    	File file = ResourceUtils.getFile("classpath:observation.json");
    	
    	PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .ssekmsKeyId(keyArn)
                .serverSideEncryption("aws:kms")
                .build();
    	
    	s3Client.putObject(objectRequest, RequestBody.fromFile(file));
    	return true;
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