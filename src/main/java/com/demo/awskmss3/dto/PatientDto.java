package com.demo.awskmss3.dto;

import java.util.Date;

import com.demo.awskmss3.domain.PatientSex;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDto {

	private String name;
	
	private String medicalRecordNumber;
	
	private String ethnicity;
	
	private Date dateOfBirth;
	
	private PatientSex sex;
	
}
