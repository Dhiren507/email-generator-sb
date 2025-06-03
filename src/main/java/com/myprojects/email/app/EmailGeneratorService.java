package com.myprojects.email.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myprojects.email.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper; // Create once, use many times

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper(); // Initialize here
    }

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String generateEmailReply(EmailRequest emailRequest) {

        // Simple validation - prevent crashes
        if (emailRequest == null || emailRequest.getEmailContent() == null ||
                emailRequest.getEmailContent().trim().isEmpty()) {
            return "Error: Please provide email content";
        }

        try {
            String prompt = buildPrompt(emailRequest);

            // Request body for the API call of gemini
            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    }
            );

            // DO REQUEST AND GET RESPONSE FROM GEMINI API
            String response = webClient.post()
                    .uri(geminiApiUrl) // Fixed: Don't add API key to URL
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey) // Better: API key in header
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30)) // Prevent hanging forever
                    .block();

            // Return response from the API
            return extractEmailContent(response);

        } catch (Exception e) {
            // Better error message for users
            System.out.println("API Error: " + e.getMessage()); // For debugging
            return "Sorry, I couldn't generate a reply right now. Please try again.";
        }
    }

    String extractEmailContent(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);

            // Check if response is valid before accessing
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isEmpty()) {
                return "No reply could be generated. Please try again.";
            }

            String emailContent = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Check if we got actual content
            if (emailContent == null || emailContent.trim().isEmpty()) {
                return "No reply could be generated. Please try again.";
            }

            return emailContent;

        } catch (Exception e) {
            System.out.println("Response parsing error: " + e.getMessage()); // For debugging
            return "Error processing the response. Please try again.";
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a Professional email reply for the following email content and Please dont Generate the subject line and ");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            // Simple security check - only allow safe tones
            String tone = emailRequest.getTone().toLowerCase().trim();
            if (tone.equals("professional") || tone.equals("friendly") ||
                    tone.equals("casual") || tone.equals("formal")) {
                prompt.append(" Use a ").append(tone).append(" tone");
            }
        }

        prompt.append("\nOriginal email content : \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
