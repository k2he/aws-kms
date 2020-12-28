package com.demo.awskmss3.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.demo.awskmss3.service.KmsCryptoImpl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kms")
public class KmsController {

	private static final byte[] EXAMPLE_DATA = "Hello World 11".getBytes(StandardCharsets.UTF_8);

//    @NonNull
//    private final KmsService kmsService;

//    @NonNull
//    private final KmsServiceSTS kmsServiceSTS;
    
    @NonNull
    private final KmsCryptoImpl kmsCryptoImpl;
    
    @RequestMapping(value = "/encrypt", method = RequestMethod.GET)
    public String encrypt() {

    	kmsCryptoImpl.encrypt(EXAMPLE_DATA);
    	
        return "Finished";
    }
    
    @RequestMapping(value = "/decrypt", method = RequestMethod.GET)
    public String decrypt() {

    	kmsCryptoImpl.encrypt(EXAMPLE_DATA);
    	
        return "Finished";
    }
}
