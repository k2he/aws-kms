package com.demo.awskmss3.controller;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.awskmss3.domain.Patient;
import com.demo.awskmss3.dto.PatientDto;
import com.demo.awskmss3.service.KmsS3Service;
import com.demo.awskmss3.service.PatientService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.Bucket;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kms")
public class KmsController {

    @NonNull
    private final PatientService patientService;
    
    @NonNull
    private final KmsS3Service kmsS3Service;
    
    @PostMapping(value = "/encryptAndSave")
    public Patient encrypt(@RequestBody PatientDto patientDto) {

    	Patient patient = Patient.builder()
    			.name(patientDto.getName())
    			.medicalRecordNumber(patientDto.getMedicalRecordNumber())
    			.ethnicity(patientDto.getEthnicity())
    			.dateOfBirth(patientDto.getDateOfBirth())
    			.sex(patientDto.getSex())
    			.build();
    	
    	Patient patientResult = patientService.save(patient);
    	
    	return patientResult;
    	
    }
    
    @GetMapping(value = "/patients/{patientId}")
    public Patient getPatientById(@PathVariable("patientId") Long patientId) {
    	Patient patient = patientService.getPatientById(patientId);
    	
    	return patient;
    }
    
    @GetMapping(value = "/patients")
    public List<Patient> getAllPatient() {
    	return patientService.getAllPatients();
    }
    
    @GetMapping(value = "/checkBucket")
    public String checkBucketName() {
    	Bucket bucket = kmsS3Service.getBucket("kai-tester-bucket");
    	return bucket.name();
    }
    
    @GetMapping(value = "/putToBucket")
    public Boolean uploadToS3() throws FileNotFoundException {
    	return kmsS3Service.addToBucket("kai-tester-bucket");
    }

    @GetMapping(value = "/listBucketObject")
    public List<String> listBucketObjects() throws FileNotFoundException {
        return kmsS3Service.listBucketObjects("kai-tester-bucket");
    }
    
    @GetMapping(value = "/getObject")
    public void getObject() throws FileNotFoundException {
        kmsS3Service.getObject("kai-tester-bucket");
    }
}
