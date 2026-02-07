package com.cognito.virtual.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/config")
public class ConfigurationController {

    @Value("${ia.api.gemini.key}")
    private String geminiApiKey;

    @Value("${ia.api.youtube.key}")
    private String youtubeApiKey;

    @GetMapping("/keys")
    public Map<String, String> getAllApiKeys() {
        Map<String, String> keys = new HashMap<>();
        keys.put("geminiKey", geminiApiKey);
        keys.put("youtubeKey", youtubeApiKey);
        return keys;
    }

    @GetMapping("/gemini")
    public Map<String, String> getGeminiKey() {
        Map<String, String> response = new HashMap<>();
        response.put("geminiKey", geminiApiKey);
        return response;
    }

    @GetMapping("/youtube")
    public Map<String, String> getYoutubeKey() {
        Map<String, String> response = new HashMap<>();
        response.put("youtubeKey", youtubeApiKey);
        return response;
    }
}