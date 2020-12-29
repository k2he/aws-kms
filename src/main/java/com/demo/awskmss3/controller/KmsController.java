package com.demo.awskmss3.controller;

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
import com.demo.awskmss3.service.PatientService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kms")
public class KmsController {

	private static final byte[] EXAMPLE_DATA = "Hello World 11".getBytes(StandardCharsets.UTF_8);

//    private final KmsService kmsService;

//    @NonNull
//    private final KmsServiceSTS kmsServiceSTS;
    
    @NonNull
    private final PatientService patientService;
    
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
}
