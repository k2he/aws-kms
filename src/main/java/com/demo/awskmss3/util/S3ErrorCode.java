package com.demo.awskmss3.util;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Map error codes to error messages. Generic errors have no specific code.
 */
@Getter
@AllArgsConstructor
public enum S3ErrorCode {
  CLIENT_ERROR(-1, "Internal client error, unable to complete request"),
  SERVER_ERROR(-1, "Internal server error, unable to complete request"),
  FORBIDDEN(403, "Object access forbidden, IAM role permissions may need updating"),
  NOT_FOUND(404, "Object does not exist"),
  MOVED_PERMANENTLY(301, "Object does not exist in this region");

  private final int statusCode;
  private final String logMessage;

  public static final String REJECTED = "S3 request was rejected with status code {}.";
}
