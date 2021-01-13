package com.demo.awskmss3.exception;

public class MediaException extends RuntimeException {

  private static final long serialVersionUID = 7039505672934422138L;

  public MediaException(final String message) {
    super(message);
  }
  
  public MediaException(final String message, Exception e) {
    super(message, e);
  }

}
