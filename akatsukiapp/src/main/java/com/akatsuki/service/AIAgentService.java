package com.akatsuki.service;

import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.util.*;

/**
 * AI Agent that interprets natural language requests and maps them
 * to per-part question type configurations.
 *
 * Example user input: "Part 1 là true/false, Part 2 là matching 10 câu"
 * Output: List of PartConfig objects ready for question generation.
 */
public class AIAgentService {

    public static class PartConfig {
        public final int partNumber;
        public final String questionType;
        public final String questionTypeLabel;
        public final int count;

        public PartConfig(int partNumber, String questionType, String questionTypeLabel, int count) {
            this.partNumber = partNumber;
            this.questionType = questionType;
            this.questionTypeLabel = questionTypeLabel;
            this.count = count;
        }

        @Override
        public String toString() {
            return "Part " + partNumber + " → " + questionTypeLabel + " (" + count + " câu)";
        }
    }

    public static class AgentResponse {
        public final String message;
        public final List<PartConfig> configs;
        public final boolean isReady;

        public AgentResponse(String message, List<PartConfig> configs, boolean isReady) {
            this.message = message;
            this.configs = configs;
            this.isReady = isReady;
        }
    }

    private static final String AGENT_SYSTEM_PROMPT = """
        You are an AI Agent for AkatsukiApp — an English listening test generation platform.
        Your job: understand the user's natural language request and map it to a structured question configuration.

        AVAILABLE QUESTION TYPES (use these exact type codes):
        - mcq = Multiple Choice (trắc nghiệm)
        - true_false = True/False (đúng/sai)
        - true_false_ng = True/False/Not Given
        - fill_blank = Fill in the Blank (điền từ)
        - sentence_completion = Sentence Completion (hoàn thành câu)
        - short_answer = Short Answer (trả lời ngắn)
        - matching = Matching (nối cột)
        - table_completion = Table Completion (hoàn thành bảng)
        - note_completion = Note Completion (hoàn thành ghi chú)

        USER CONTEXT:
        - The audio transcript has been split into PARTs (Part 1, Part 2, Part 3, Part 4, etc.)
        - The user wants to assign SPECIFIC question types to SPECIFIC parts.
        - Default count per part is 5 questions unless the user says otherwise.

        YOUR RESPONSE FORMAT — you MUST return ONLY valid JSON:
        {
          "message": "A friendly Vietnamese confirmation message summarizing what you understood",
          "configs": [
            { "part": 1, "type": "true_false", "type_label": "True/False", "count": 5 },
            { "part": 2, "type": "matching", "type_label": "Matching", "count": 5 }
          ],
          "ready": true
        }

        RULES:
        1. ALWAYS respond in Vietnamese.
        2. BE DECISIVE — ACT IMMEDIATELY. The user wants you to execute their request, NOT to interrogate them.
           As long as you can extract ANY reasonable intent, set "ready": true and produce configs right away.
           DO NOT ask the user to confirm, repeat, or clarify things you can reasonably infer.
        3. FILL IN SENSIBLE DEFAULTS instead of asking:
           - No count given → use 5 câu per part.
           - No part specified but a type is given → apply that type to ALL available parts.
           - Type slightly ambiguous → pick the closest matching type code and proceed.
           - User says something vague like "tạo câu hỏi đi", "làm giúp tôi", "tùy bạn" → choose a balanced
             mix (e.g. mcq for the parts) with 5 câu each across all available parts and set "ready": true.
           In your "message", briefly state the assumption you made (e.g. "Mình mặc định 5 câu mỗi phần nhé").
        4. ONLY set "ready": false when the request is GENUINELY impossible to interpret as a question
           configuration at all (e.g. completely off-topic or empty). Even then, keep the clarifying
           question to ONE short sentence. Never ask more than one question, never ask trivial confirmations.
        5. The user may say things like:
           - "Part 1 true false, part 2 matching" → map directly
           - "Phần 1 trắc nghiệm, phần 2 điền từ" → map Vietnamese names to type codes
           - "3 phần, mỗi phần 10 câu MCQ" → all 3 parts get mcq with count 10
           - "Tôi muốn part 1 là đúng sai, part 2 nối, part 3 điền khuyết" → map accordingly
        6. Be smart about Vietnamese aliases: "trắc nghiệm" = mcq, "đúng sai" = true_false, "nối" or "nối cột" = matching, "điền" or "điền từ" or "điền khuyết" = fill_blank, etc.
        7. Return ONLY the JSON object. No markdown, no code fences.
        """;

    public static AgentResponse processRequest(String userMessage, int availableParts) throws Exception {
        GeminiService gemini = GeminiService.getInstance();
        if (!gemini.isConfigured()) {
            return new AgentResponse("Gemini API chưa được cấu hình. Vui lòng kiểm tra API key.", List.of(), false);
        }

        String contextNote = "The transcript has " + availableParts + " part(s) available.";

        JsonObject request = new JsonObject();
        JsonArray contents = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "user");
        JsonArray systemParts = new JsonArray();
        JsonObject systemText = new JsonObject();
        systemText.addProperty("text", AGENT_SYSTEM_PROMPT + "\n\n" + contextNote);
        systemParts.add(systemText);
        systemMsg.add("parts", systemParts);
        contents.add(systemMsg);

        JsonObject modelAck = new JsonObject();
        modelAck.addProperty("role", "model");
        JsonArray ackParts = new JsonArray();
        JsonObject ackText = new JsonObject();
        ackText.addProperty("text", "{\"message\": \"Tôi đã sẵn sàng. Bạn muốn cấu hình câu hỏi như thế nào?\", \"configs\": [], \"ready\": false}");
        ackParts.add(ackText);
        modelAck.add("parts", ackParts);
        contents.add(modelAck);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userText = new JsonObject();
        userText.addProperty("text", userMessage);
        userParts.add(userText);
        userMsg.add("parts", userParts);
        contents.add(userMsg);

        request.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        request.add("generationConfig", generationConfig);

        String apiKey = getApiKey();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofMinutes(1))
                .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return new AgentResponse("Lỗi API: " + response.statusCode() + ". Vui lòng thử lại.", List.of(), false);
        }

        return parseAgentResponse(response.body());
    }

    private static AgentResponse parseAgentResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return new AgentResponse("Không nhận được phản hồi từ AI. Thử lại nhé.", List.of(), false);
            }

            String text = candidates.get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();

            JsonObject parsed = JsonParser.parseString(text).getAsJsonObject();
            String message = parsed.has("message") ? parsed.get("message").getAsString() : "";
            boolean ready = parsed.has("ready") && parsed.get("ready").getAsBoolean();

            List<PartConfig> configs = new ArrayList<>();
            if (parsed.has("configs")) {
                for (JsonElement el : parsed.getAsJsonArray("configs")) {
                    JsonObject cfg = el.getAsJsonObject();
                    int part = cfg.get("part").getAsInt();
                    String type = cfg.get("type").getAsString();
                    String typeLabel = cfg.has("type_label") ? cfg.get("type_label").getAsString() : type;
                    int count = cfg.has("count") ? cfg.get("count").getAsInt() : 5;
                    configs.add(new PartConfig(part, type, typeLabel, count));
                }
            }

            return new AgentResponse(message, configs, ready);
        } catch (Exception e) {
            return new AgentResponse("Không thể phân tích phản hồi AI: " + e.getMessage(), List.of(), false);
        }
    }

    private static String getApiKey() {
        return GeminiService.getInstance().getApiKey();
    }
}
