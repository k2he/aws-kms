package com.demo.awskmss3.domain;

import java.util.HashSet;

public enum PatientSex {
  MALE,
  FEMALE,
  OTHER,
  UNKNOWN;
  
  private static final HashSet<String> values = new HashSet<String>();
  
  static {
    for (PatientSex sex : PatientSex.values()) {
      values.add(sex.name());
    }
  }
  
  public static boolean validateSex(String sex) {
    return values.contains(sex);
  }
}
