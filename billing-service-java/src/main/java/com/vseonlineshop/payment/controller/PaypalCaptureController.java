package com.vseonlineshop.payment.controller;


import com.vseonlineshop.payment.service.PaypalCaptureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/paypal")
@RequiredArgsConstructor
@Slf4j
public class PaypalCaptureController {

    private final PaypalCaptureService paypalCaptureService;

    @PostMapping("/capture/{token}")
    public ResponseEntity<Map<String,String>> captureOrder(
            @PathVariable String token
    ) {

        String result = paypalCaptureService.captureOrder(token);

        return ResponseEntity.ok(
                Map.of(
                        "message", result
                )
        );
    }
}
