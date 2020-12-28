package com.demo.awskmss3.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.demo.awskmss3.service.KmsServiceSTS2;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/kms")
public class KmsController {

//    @NonNull
//    private final KmsService kmsService;

//    @NonNull
//    private final KmsServiceSTS kmsServiceSTS;
    
    @NonNull
    private final KmsServiceSTS2 kmsServiceSTS2;
    
    @RequestMapping(value = "/encrypt", method = RequestMethod.GET)
    public String getActionByApplicationNumber() {

        return "Finished";
    }
}
