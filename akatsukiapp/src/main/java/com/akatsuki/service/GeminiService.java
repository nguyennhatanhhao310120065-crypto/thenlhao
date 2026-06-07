package com.akatsuki.service;

import com.akatsuki.model.Question;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class GeminiService {
    private static GeminiService instance;
    private String apiKey;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    private GeminiService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        loadApiKey();
    }

    public static synchronized GeminiService getInstance() {
        if (instance == null) instance = new GeminiService();
        return instance;
    }

    private void loadApiKey() {
        try {
            Path envPath = Path.of(System.getProperty("user.dir"), ".env");
            if (!Files.exists(envPath)) {
                envPath = Path.of(System.getProperty("user.dir"), "src", "main", "resources", ".env");
            }
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq == -1) continue;
                    String key = trimmed.substring(0, eq).trim();
                    String val = trimmed.substring(eq + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'")))
                        val = val.substring(1, val.length() - 1);
                    if ("GEMINI_API_KEY".equals(key)) apiKey = val;
                }
            }
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getenv("GEMINI_API_KEY");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String getApiKey() {
        return apiKey;
    }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    public String transcribeAudio(File audioFile) throws Exception {
        if (!isConfigured()) throw new Exception("GEMINI_API_KEY is not configured");

        String mimeType = getMimeType(audioFile.getName());
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

        String transcribePrompt = """
            Transcribe this English listening test audio accurately and completely.
            
            CRITICAL RULES — READ ALL BEFORE STARTING:
            
            1. ROLE IDENTIFICATION:
               - "Narrator" = the voice that reads TEST INSTRUCTIONS (e.g., "You will hear...", "Now look at Part 1",
                 "Before you hear the rest of the conversation, you have some time to look at questions 4 to 10",
                 "Now listen and answer questions 1 to 3"). These are NOT part of the conversation.
               - "Speaker 1", "Speaker 2" = actual people in the CONVERSATION or LECTURE.
               - NEVER label test instructions as Speaker. They are ALWAYS "Narrator".
            
            2. PART STRUCTURE:
               - This audio likely contains multiple PARTS (Part 1, Part 2, Part 3, Part 4).
               - Mark each part with: === PART X === (on its own line, NO timestamp, NO speaker label).
               - IMPORTANT: When a new part begins, RESET speaker numbers back to Speaker 1 and Speaker 2.
                 Each part has its OWN set of speakers. Do NOT continue numbering from the previous part.
               - Part 1 might have Speaker 1 and Speaker 2. Part 2 has different people, but still call them Speaker 1 and Speaker 2.
            
            3. TIMESTAMP FORMAT:
               - EVERY spoken line MUST start with [MM:SS] timestamp.
               - Part headers (=== PART X ===) do NOT get timestamps.
               - Be accurate with timestamps. Do NOT repeat the same content at different timestamps.
               - Each sentence should appear EXACTLY ONCE. Never duplicate lines.
            
            4. LINE FORMAT:
               - Narrator lines: [MM:SS] Narrator: instruction text here
               - Speaker lines:  [MM:SS] Speaker 1: spoken dialogue here
               - Part headers:   === PART 1 ===
            
            5. SPEAKER COUNT PER PART:
               - Most parts have exactly 2 speakers in conversation, or 1 speaker in a monologue/lecture.
               - Do NOT create more than 3 speakers per part unless absolutely certain.
            
            6. ACCURACY:
               - Capture every word. Use proper punctuation.
               - Start a new line when speaker changes or every 10-15 seconds.
               - Output ONLY the transcript. No commentary.
            
            EXAMPLE OUTPUT:
            [0:00] Narrator: You will hear a number of different recordings and you will have to answer questions on what you hear.
            [0:08] Narrator: Now look at Part 1.
            === PART 1 ===
            [0:12] Narrator: You will hear two friends discussing their travel plans. Listen and answer questions 1 to 5.
            [0:20] Speaker 1: Hi Sarah, have you booked your flight yet?
            [0:23] Speaker 2: Yes, I booked it yesterday. I'm flying out on Friday.
            [0:28] Speaker 1: Great, me too. What time is your flight?
            [1:45] Narrator: Before you hear the rest of the conversation, you have some time to look at questions 4 to 5.
            [1:55] Narrator: Now listen and answer questions 4 to 5.
            [2:00] Speaker 1: So where are you staying?
            === PART 2 ===
            [3:00] Narrator: Now look at Part 2. You will hear a student talking to a librarian.
            [3:10] Speaker 1: Good morning. Can I help you?
            [3:12] Speaker 2: Yes, I would like to check in for flight FA492.
            """;

        JsonObject request = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();

        JsonObject audioPart = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", mimeType);
        inlineData.addProperty("data", base64Audio);
        audioPart.add("inlineData", inlineData);
        parts.add(audioPart);

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", transcribePrompt);
        parts.add(textPart);

        content.add("parts", parts);
        contents.add(content);
        request.add("contents", contents);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();

        HttpResponse<String> response = sendWithRetry(httpRequest);
        return extractTextFromResponse(response.body());
    }

    public List<Question> generateQuestions(String transcript, String examType, String questionType,
                                            int count, String partLabel, String customInstruction) throws Exception {
        if (!isConfigured()) throw new Exception("GEMINI_API_KEY is not configured");

        String prompt = PromptBuilder.buildSingleTypePrompt(transcript, examType, questionType, count, partLabel, customInstruction);

        JsonObject request = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);
        content.add("parts", parts);
        contents.add(content);
        request.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        request.add("generationConfig", generationConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofMinutes(3))
                .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();

        HttpResponse<String> response = sendWithRetry(httpRequest);
        String jsonText = extractTextFromResponse(response.body());
        return parseQuestions(jsonText, questionType);
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return response;

                if (response.statusCode() == 503 || response.statusCode() == 429) {
                    System.out.println("[Gemini] Attempt " + attempt + "/" + MAX_RETRIES + " got " + response.statusCode() + ", retrying in " + (RETRY_DELAY_MS / 1000) + "s...");
                    lastException = new Exception("Gemini API quá tải (503). Server đang bận, vui lòng thử lại sau vài giây.");
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                        continue;
                    }
                } else {
                    throw new Exception("Gemini API error (HTTP " + response.statusCode() + "): " + response.body());
                }
            } catch (java.io.IOException e) {
                System.out.println("[Gemini] Attempt " + attempt + "/" + MAX_RETRIES + " IO error: " + e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }
        throw lastException != null ? lastException : new Exception("Gemini API request failed after " + MAX_RETRIES + " retries");
    }

    public List<Question> generateAllQuestions(String transcript, String examType, List<String> questionTypes,
                                               int questionsPerType, Map<String, String> customInstructions) throws Exception {
        var parts = PromptBuilder.splitTranscriptByParts(transcript);
        boolean hasParts = parts.size() > 1;

        List<CompletableFuture<List<Question>>> futures = new ArrayList<>();

        if (hasParts) {
            if (questionTypes.size() <= parts.size()) {
                int ppt = parts.size() / questionTypes.size();
                int rem = parts.size() % questionTypes.size();
                int pi = 0;
                for (int i = 0; i < questionTypes.size(); i++) {
                    int n = ppt + (i < rem ? 1 : 0);
                    StringBuilder merged = new StringBuilder();
                    StringBuilder labels = new StringBuilder();
                    for (int j = pi; j < pi + n; j++) {
                        if (j > pi) { merged.append("\n\n"); labels.append(" + "); }
                        merged.append(parts.get(j).get("text"));
                        labels.append(parts.get(j).get("label"));
                    }
                    pi += n;
                    String type = questionTypes.get(i);
                    String ci = customInstructions != null ? customInstructions.getOrDefault(type, customInstructions.getOrDefault("_global", null)) : null;
                    String mergedStr = merged.toString();
                    String labelsStr = labels.toString();
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        try { return generateQuestions(mergedStr, examType, type, questionsPerType, labelsStr, ci); }
                        catch (Exception e) { System.err.println("Generate failed for " + type + ": " + e.getMessage()); return List.of(); }
                    }));
                }
            } else {
                int tpp = questionTypes.size() / parts.size();
                int rem = questionTypes.size() % parts.size();
                int ti = 0;
                for (int i = 0; i < parts.size(); i++) {
                    int n = tpp + (i < rem ? 1 : 0);
                    for (int j = 0; j < n; j++) {
                        String type = questionTypes.get(ti++);
                        String text = (String) parts.get(i).get("text");
                        String label = (String) parts.get(i).get("label");
                        String ci = customInstructions != null ? customInstructions.getOrDefault(type, customInstructions.getOrDefault("_global", null)) : null;
                        futures.add(CompletableFuture.supplyAsync(() -> {
                            try { return generateQuestions(text, examType, type, questionsPerType, label, ci); }
                            catch (Exception e) { System.err.println("Generate failed for " + type + ": " + e.getMessage()); return List.of(); }
                        }));
                    }
                }
            }
        } else {
            for (String type : questionTypes) {
                String ci = customInstructions != null ? customInstructions.getOrDefault(type, customInstructions.getOrDefault("_global", null)) : null;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try { return generateQuestions(transcript, examType, type, questionsPerType, null, ci); }
                    catch (Exception e) { System.err.println("Generate failed for " + type + ": " + e.getMessage()); return List.of(); }
                }));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Question> allQuestions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int num = 1;
        for (var future : futures) {
            List<Question> qs = future.get();
            for (Question q : qs) {
                q.setQuestionNumber(num++);
                allQuestions.add(q);
            }
        }

        if (allQuestions.isEmpty()) {
            throw new Exception("Tất cả yêu cầu đều thất bại. Gemini API có thể đang quá tải (503). Vui lòng đợi 30 giây rồi thử lại.");
        }
        return allQuestions;
    }

    private String extractTextFromResponse(String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray candidates = json.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) return "";
        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        JsonObject contentObj = firstCandidate.getAsJsonObject("content");
        if (contentObj == null) return "";
        JsonArray partsList = contentObj.getAsJsonArray("parts");
        if (partsList == null || partsList.isEmpty()) return "";
        return partsList.get(0).getAsJsonObject().get("text").getAsString();
    }

    private List<Question> parseQuestions(String jsonText, String questionType) {
        List<Question> questions = new ArrayList<>();
        try {
            JsonObject parsed = JsonParser.parseString(jsonText).getAsJsonObject();
            JsonArray qArray = parsed.getAsJsonArray("questions");
            if (qArray == null) return questions;
            for (JsonElement el : qArray) {
                JsonObject qObj = el.getAsJsonObject();
                Question q = new Question();
                q.setQuestionNumber(qObj.has("number") ? qObj.get("number").getAsInt() : 0);
                q.setQuestionText(qObj.has("text") ? qObj.get("text").getAsString() : "");
                q.setCorrectAnswer(qObj.has("answer") ? qObj.get("answer").getAsString() : "");
                q.setExplanation(qObj.has("explanation") ? qObj.get("explanation").getAsString() : "");
                q.setQuestionType(questionType);
                q.setTranscriptQuote(qObj.has("transcript_quote") ? qObj.get("transcript_quote").getAsString() : "");
                if (qObj.has("options")) {
                    List<String> opts = gson.fromJson(qObj.getAsJsonArray("options"), new TypeToken<List<String>>() {}.getType());
                    q.setOptions(opts);
                }
                questions.add(q);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse questions JSON: " + e.getMessage());
        }
        return questions;
    }

    /**
     * Generate questions using AI Agent per-part config.
     * Each PartConfig maps a specific question type to a specific transcript part.
     */
    public List<Question> generateWithAgentConfig(String transcript, String examType,
                                                   List<AIAgentService.PartConfig> configs) throws Exception {
        var parts = PromptBuilder.splitTranscriptByParts(transcript);
        List<CompletableFuture<List<Question>>> futures = new ArrayList<>();

        for (AIAgentService.PartConfig cfg : configs) {
            int partIdx = cfg.partNumber - 1;
            String partText;
            String partLabel;
            if (partIdx >= 0 && partIdx < parts.size()) {
                partText = (String) parts.get(partIdx).get("text");
                partLabel = (String) parts.get(partIdx).get("label");
            } else {
                partText = transcript;
                partLabel = "Full Transcript";
            }

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return generateQuestions(partText, examType, cfg.questionType, cfg.count, partLabel, null);
                } catch (Exception e) {
                    System.err.println("Agent generate failed for Part " + cfg.partNumber + " " + cfg.questionType + ": " + e.getMessage());
                    return List.of();
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Question> allQuestions = new ArrayList<>();
        int num = 1;
        for (var future : futures) {
            for (Question q : future.get()) {
                q.setQuestionNumber(num++);
                allQuestions.add(q);
            }
        }

        if (allQuestions.isEmpty()) {
            throw new Exception("AI Agent: Tất cả yêu cầu đều thất bại. Vui lòng thử lại.");
        }
        return allQuestions;
    }

    private String getMimeType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        return switch (ext) {
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".ogg" -> "audio/ogg";
            case ".m4a" -> "audio/mp4";
            case ".flac" -> "audio/flac";
            case ".webm" -> "audio/webm";
            default -> "audio/mpeg";
        };
    }
}
