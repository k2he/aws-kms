package com.demo.awskmss3.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.http.HttpStatus;

import com.demo.awskmss3.exception.MediaException;
import static com.demo.awskmss3.util.S3ErrorCode.CLIENT_ERROR;
import static com.demo.awskmss3.util.S3ErrorCode.FORBIDDEN;
import static com.demo.awskmss3.util.S3ErrorCode.MOVED_PERMANENTLY;
import static com.demo.awskmss3.util.S3ErrorCode.REJECTED;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@Slf4j
final class S3v2 {

	private static final long S3_MAX_UPLOAD_SIZE = 5000000000L;
	private static final int S3_MAX_CONNECTIONS = 100;

	final static String roleArn = "arn:aws:iam::779352748365:role/kai-test-ec2";

	private final S3Client s3Client;
	private final S3Utilities util;
	private final S3Presigner s3Presigner;

	private static final String ROLE_SESSION_NAME = "demo-s3-runner";

	/**
	 * Create S3 based on given AWS region.
	 */
	public S3v2(final String region) {
		final Region awsRegion = Region.of(region);
		this.s3Client = S3Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(kmsSessionCredentials(Region.US_EAST_2)))
				.region(Region.US_EAST_2).build();

		this.util = S3Utilities.builder().region(awsRegion).build();
		this.s3Presigner = S3Presigner.builder().region(awsRegion).build();
	}

	/**
	 * Retrieve the STS token stored along with the user's authentication.
	 */
	private AwsSessionCredentials kmsSessionCredentials(Region region) {
		Duration duration = Duration.ofSeconds(900);
		final StsClient stsClient = StsClient.builder().region(region).build();
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

	/**
	 * Clean up s3 objects.
	 */
	@PreDestroy
	public void destroy() {
		s3Client.close();
		s3Presigner.close();
	}

	/**
	 * Copy object to a specified bucket.
	 */
	public URL copy(final String source, final String target, final String key) {
		try {
			s3Client.copyObject(CopyObjectRequest.builder().copySource(source).destinationBucket(target).destinationKey(key)
					.build());
			return getUrl(target, key);
		} catch (final SdkServiceException ase) {
			throw new MediaException("Error copying file", ase);
		}
	}

	/**
	 * Create a pre-signed upload url for a given bucket and key combination.
	 */
	public URL generatePreSignedUrl(final String bucket, final String keyName, final String md5, final Instant expiry) {
		try {
			final PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucket).key(keyName).contentMD5(md5)
					.build();
			final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
					.putObjectRequest(putRequest).signatureDuration(Duration.between(Instant.now(), expiry)).build();
			return s3Presigner.presignPutObject(presignRequest).url();
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Upload a file to s3 with given keyname.
	 *
	 * @param file File to upload.
	 * @param key  Keyname of file to upload.
	 * @return S3Resource with bucket / keyname uploaded to.
	 */
	public URL upload(final File file, final String bucket, final String key) {
		try {
			final long fileSize = Files.size(file.toPath());
			if (fileSize < S3_MAX_UPLOAD_SIZE) {
				final PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).build();
				return executePutObjectRequest(bucket, key, request, file);
			} else {
				throw new MediaException(String.format("S3v2 cannot upload file of size %s", fileSize));
			}
		} catch (final IOException e) {
			throw new MediaException("Error trying to retrieve file's size", e);
		}
	}

	/**
	 * Upload a file to s3 with given keyname.
	 *
	 * @param content     a byte array to use as content of the objct.
	 * @param contentType the content type
	 * @param bucket      target bucket
	 * @param keyName     Keyname of file to upload.
	 * @return S3Resource with bucket / keyname uploaded to.
	 */
	public URL upload(final byte[] content, final String contentType, final String bucket, final String keyName,
			final ObjectCannedACL cannedAcl) {
		try {
			PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(keyName)
					.contentType(contentType).contentLength((long) content.length);
			if (cannedAcl != null) {
				putObjectRequest = putObjectRequest.acl(cannedAcl);
			}
			final RequestBody requestBody = RequestBody.fromBytes(content);
			s3Client.putObject(putObjectRequest.build(), requestBody);
			return getUrl(bucket, keyName);
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Retrieve a file from S3.
	 *
	 * @param bucket  Bucket file is located in.
	 * @param keyName Keyname of file.
	 * @return S3Object of object requested.
	 */
	public ResponseInputStream<GetObjectResponse> getObject(final String bucket, final String keyName) {
		try {
			return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(keyName).build());
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			log.info("{}", ace.getCause(), ace);
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Get an object stream for specific bytes of an object.
	 */
	protected ResponseInputStream<GetObjectResponse> getObject(final String bucket, final String key, final long start,
			final long end) {
		try {
			final GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key)
					.range(String.format("bytes=%s-%s", start, end)).build();
			return s3Client.getObject(request);
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			log.info("client exception", ace);
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Retrieve a metadata from S3.
	 *
	 * @param bucket  Bucket file is located in.
	 * @param keyName Keyname of file.
	 * @return ObjectMetadata of object requested.
	 */
	public Map<String, String> getObjectMetadata(final String bucket, final String keyName) {
		try {
			return s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(keyName).build()).metadata();
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Check if an object exists on S3.
	 *
	 * @param bucket  Bucket object is located in.
	 * @param keyName Keyname of object.
	 * @return True if object exists, false if not.
	 */
	public boolean objectExists(final String bucket, final String keyName) {
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(keyName).build());
			return true;
		} catch (final SdkServiceException ase) {
			if (ase.statusCode() == HttpStatus.SC_NOT_FOUND) {
				return false;
			}
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Retrieve a URL for a requested resource.
	 *
	 * @param bucket  Bucket resource is in.
	 * @param keyName Keyname of resource.
	 * @return URL for requested resource.
	 */
	public URL getUrl(final String bucket, final String keyName) {
		try {
			return util.getUrl(GetUrlRequest.builder().bucket(bucket).key(keyName).build());
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	/**
	 * Note: This function is very misleading as there is no such thing as a
	 * directory in s3. If the key ends with "/" we will get true for an empty or
	 * non-empty folder and we'll get false for a non existent or regular file.
	 */
	public boolean isDirectory(final String bucketName, final String key) {
		if (null == bucketName || bucketName.isBlank() || bucketName.isEmpty()) {
			log.warn("Bucketname is empty");
			return false;
		}
		if (null == key || key.isEmpty() || key.isBlank()) {
			log.warn("Key prefix is empty");
			return false;
		}
		// There's no concept of a directory hence it doesn't exist on s3, and we want
		// to check the object doesn't exist,
		// because if it does it's not a parent henceforth.
		if (objectExists(bucketName, key)) {
			log.info("Object exists, hence not a directory");
			return false;
		}

		// Note : This request will give 403 error code instead of 404 if s3:ListBucket
		// permission isn't there for Bucket.
		final ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).prefix(key)
				.delimiter("/").maxKeys(1).build(); // We only want to peek and see if there's any object under.

		final ListObjectsV2Response objectList = getObjectList(listObjectsRequest);
		// A directory in S3 is an object which doesn't exist, and any real object under
		// that if found shall be non-zero
		return objectList.keyCount() > 0;
	}

	/**
	 * List objects for a given bucket / key combination.
	 */
	public ListObjectsV2Response getObjectList(final String bucketName, final String key) {
		final ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).prefix(key)
				.build();
		return getObjectList(listObjectsRequest);
	}

	private ListObjectsV2Response getObjectList(final ListObjectsV2Request listObjectsRequest) {
		try {
			return s3Client.listObjectsV2(listObjectsRequest);
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		}
	}

	/**
	 * Delete an object on S3.
	 */
	public void delete(final String bucket, final String key) {
		try {
			if (objectExists(bucket, key)) {
				s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
			}
		} catch (final SdkClientException e) {
			throw new MediaException("Couldn't delete object on S3", e);
		}
	}

	/**
	 * Get content length of an object.
	 */
	long getContentLength(final String bucket, final String keyName) {
		try {
			return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(keyName).build()).response()
					.contentLength();
		} catch (final SdkServiceException ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	private URL executePutObjectRequest(final String bucket, final String keyName,
			final PutObjectRequest putObjectRequest, final File file) {
		try {
			s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
			return getUrl(bucket, keyName);
		} catch (final S3Exception ase) {
			logAmazonException(ase);
			throw new MediaException(ase.getMessage(), ase);
		} catch (final SdkClientException ace) {
			throw new MediaException(CLIENT_ERROR.getLogMessage(), ace);
		}
	}

	private static void logAmazonException(final SdkServiceException ase) {
		final int statusCode = ase.statusCode();
		log.info(REJECTED, statusCode);
		if (statusCode == HttpStatus.SC_FORBIDDEN) {
			log.error(FORBIDDEN.getLogMessage(), ase);
		} else if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY) {
			log.error(MOVED_PERMANENTLY.getLogMessage(), ase);
		}
	}
}
