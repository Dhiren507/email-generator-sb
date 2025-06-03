package com.myprojects.email.app;

import com.myprojects.email.EmailRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/email")
@CrossOrigin(origins = {"http://localhost:5173", "https://emailreplygenratorr.netlify.app"})
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;
    public EmailGeneratorController(EmailGeneratorService emailGeneratorService) {
        this.emailGeneratorService = emailGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestBody EmailRequest emailRequest) {
        String generatedEmail = emailGeneratorService.generateEmailReply(emailRequest);

        return ResponseEntity.ok(generatedEmail);
    }

    @GetMapping("/health")
    public String health() {
        return "Email Generator Service is running!";
    }
}
