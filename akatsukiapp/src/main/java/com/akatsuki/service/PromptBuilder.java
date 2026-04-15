package com.akatsuki.service;

import java.util.*;

public class PromptBuilder {

    private static final Map<String, String> TYPE_INSTRUCTIONS = new LinkedHashMap<>();
    private static final Map<String, String> EXAM_CONTEXTS = new LinkedHashMap<>();

    static {
        TYPE_INSTRUCTIONS.put("mcq", """
            ### MULTIPLE CHOICE (type: "mcq")
            QUESTION DESIGN PROCESS — follow these steps for EACH question:
            1. Identify a specific comprehension point in the transcript.
            2. Write a clear question stem that tests understanding of that point.
            3. Write the CORRECT answer — it may PARAPHRASE the transcript.
            4. Write 3 DISTRACTORS following the rules below.
            DISTRACTOR RULES:
            - At least 3 of the 4 options MUST use information that IS mentioned in the transcript.
            - WRONG options take REAL details from the transcript but apply them to the WRONG context.
            - NEVER use "All of the above" or "None of the above".
            FORMAT:
            - options: ["A. ...", "B. ...", "C. ...", "D. ..."]
            - answer: JUST the letter, e.g. "B"
            """);

        TYPE_INSTRUCTIONS.put("true_false", """
            ### TRUE / FALSE (type: "true_false")
            STATEMENT DESIGN PROCESS:
            1. Identify specific claims, facts, numbers, or descriptions in the transcript.
            2. For each question, write ONE clear declarative statement.
            STATEMENT RULES:
            - TRUE statements: PARAPHRASE something directly stated in the transcript.
            - FALSE statements: Take a REAL detail and CHANGE one key element to make it incorrect.
            - Target distribution: approximately 50% True, 50% False.
            FORMAT:
            - options: ["True", "False"] (ALWAYS exactly these two strings)
            - answer: "True" or "False"
            """);

        TYPE_INSTRUCTIONS.put("true_false_ng", """
            ### TRUE / FALSE / NOT GIVEN (type: "true_false_ng")
            CATEGORY DEFINITIONS:
            - TRUE: The transcript directly SUPPORTS this statement.
            - FALSE: The transcript directly CONTRADICTS this statement.
            - NOT GIVEN: The SPECIFIC claim is neither confirmed nor denied in the transcript.
            - Target distribution: roughly 1/3 True, 1/3 False, 1/3 Not Given.
            FORMAT:
            - options: ["True", "False", "Not Given"]
            - answer: "True", "False", or "Not Given"
            """);

        TYPE_INSTRUCTIONS.put("fill_blank", """
            ### FILL IN THE BLANK (type: "fill_blank")
            DESIGN RULES:
            - The answer MUST be 1-3 words taken DIRECTLY from the transcript.
            - The blank must replace MEANINGFUL information.
            - Good targets: proper nouns, numbers, dates, specific terms.
            FORMAT:
            - options: [] (empty array)
            - answer: Exact 1-3 words from the transcript
            """);

        TYPE_INSTRUCTIONS.put("sentence_completion", """
            ### SENTENCE COMPLETION (type: "sentence_completion")
            DESIGN RULES:
            - The sentence beginning must clearly direct toward ONE specific answer.
            - The answer is 1-4 words that naturally complete the sentence.
            FORMAT:
            - options: [] (empty array)
            - answer: 1-4 words that complete the sentence
            """);

        TYPE_INSTRUCTIONS.put("short_answer", """
            ### SHORT ANSWER (type: "short_answer")
            DESIGN RULES:
            - Use WH-questions: What / Where / When / Who / How many / How much / Why / How.
            - Each question must have ONE clear, unambiguous correct answer.
            - The answer should be 1-5 words.
            FORMAT:
            - options: [] (empty array)
            - answer: 1-5 word factual answer
            """);

        TYPE_INSTRUCTIONS.put("matching", """
            ### MATCHING (type: "matching")
            MATCHING DESIGN RULES:
            - ALL items and ALL options MUST come from the transcript.
            - EVERY question MUST use the EXACT SAME options array (shared options list).
            - Include at least 2 MORE options than questions.
            FORMAT:
            - All questions share identical options: ["A. ...", "B. ...", "C. ...", "D. ...", "E. ...", ...]
            - answer: The correct letter, e.g. "C"
            """);

        TYPE_INSTRUCTIONS.put("table_completion", """
            ### TABLE / FORM COMPLETION (type: "table_completion")
            DESIGN RULES:
            - Each question = ONE blank cell in a table, form, or structured document.
            - Format the text as: "[Table/Form Title] — [Row/Column label]: _____"
            - The answer MUST be 1-3 words taken DIRECTLY from the transcript.
            FORMAT:
            - options: [] (empty array)
            - answer: Exact 1-3 words from the transcript
            """);

        TYPE_INSTRUCTIONS.put("note_completion", """
            ### NOTE / OUTLINE COMPLETION (type: "note_completion")
            DESIGN RULES:
            - Each question = ONE blank in organized notes or a summary outline.
            - Format: "[Topic/Section heading] — [bullet or sub-heading]: _____"
            - The answer MUST be 1-3 words from the transcript.
            FORMAT:
            - options: [] (empty array)
            - answer: Exact 1-3 words from the transcript
            """);

        EXAM_CONTEXTS.put("IELTS", """
            You are a senior IELTS examiner creating an official IELTS Listening practice test.
            IELTS LISTENING STANDARDS:
            - Questions MUST progress chronologically following the transcript
            - Language register: formal, precise, matching actual Cambridge IELTS papers
            - Fill-in-blank / note / table answers: NO MORE THAN THREE WORDS AND/OR A NUMBER
            - Multiple choice: exactly 4 options (A-D)
            """);

        EXAM_CONTEXTS.put("TOEIC", """
            You are an ETS-certified TOEIC test developer creating an official TOEIC Listening practice test.
            TOEIC LISTENING STANDARDS:
            - Focus on business, workplace, and everyday life communication scenarios
            - Test practical English comprehension for professional and social contexts
            - Correct answers often PARAPHRASE the audio
            """);

        EXAM_CONTEXTS.put("VSTEP", """
            You are a VSTEP exam specialist creating a VSTEP Listening test.
            VSTEP LISTENING STANDARDS:
            - Target proficiency levels B1 to C1
            - All questions are multiple-choice with 4 options (A-D)
            """);

        EXAM_CONTEXTS.put("GENERAL", """
            You are a university English professor designing a listening comprehension exam.
            UNIVERSITY EXAM STANDARDS:
            - Cover multiple cognitive skills: factual recall, inference, analysis
            - Balanced difficulty: 30% straightforward, 50% moderate, 20% challenging
            """);
    }

    public static String buildSingleTypePrompt(String transcript, String examType, String questionType,
                                                int count, String partLabel, String customInstruction) {
        String typeRule = TYPE_INSTRUCTIONS.getOrDefault(questionType, "");
        String examContext = EXAM_CONTEXTS.getOrDefault(examType, EXAM_CONTEXTS.get("GENERAL"));

        String partNote = partLabel != null && !partLabel.isEmpty()
                ? "\nCRITICAL PART CONSTRAINT: This transcript is from **" + partLabel + "** of the listening test. Generate ALL questions based ONLY on this part's content.\n"
                : "";

        String customNote = customInstruction != null && !customInstruction.isEmpty()
                ? "\nUSER'S CUSTOM INSTRUCTIONS:\n\"\"\"\n" + customInstruction + "\n\"\"\"\n"
                : "";

        return examContext + partNote +
                "\nYOUR TASK: Generate EXACTLY " + count + " \"" + questionType + "\" questions based on the audio transcript below.\n" +
                "ALL " + count + " questions MUST be of type \"" + questionType + "\".\n" +
                customNote +
                "\nQUESTION TYPE RULES:\n\n" + typeRule +
                "\nCRITICAL OUTPUT RULES:\n" +
                "1. Every answer MUST be directly verifiable from the transcript.\n" +
                "2. Questions MUST follow the chronological order of the transcript.\n" +
                "3. Each question object MUST contain: number (1-" + count + "), text (string), type (\"" + questionType + "\"), options (string[]), answer (string), explanation (string), transcript_quote (string).\n" +
                "4. For mcq/matching: \"options\" = labeled choices [\"A. ...\", \"B. ...\", ...].\n" +
                "5. For fill_blank/sentence_completion/short_answer/table_completion/note_completion: \"options\" = [].\n" +
                "6. For true_false: \"options\" MUST be [\"True\", \"False\"]. For true_false_ng: [\"True\", \"False\", \"Not Given\"].\n" +
                "7. \"explanation\" MUST quote the relevant passage and explain why the answer is correct.\n" +
                "8. \"transcript_quote\" = EXACT sentence(s) from the transcript (5-50 words).\n" +
                "9. Do NOT repeat information across questions.\n" +
                "10. Questions must be INDISTINGUISHABLE from a real " + examType + " exam paper.\n\n" +
                "TRANSCRIPT:\n\"\"\"\n" + transcript + "\n\"\"\"\n\n" +
                "Return JSON: { \"questions\": [ ..." + count + " question objects... ] }";
    }

    public static List<Map<String, Object>> splitTranscriptByParts(String transcript) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (transcript == null || transcript.isBlank()) return parts;

        String[] patterns = {
                "(?:^|\\n)\\s*={2,}\\s*(?:PART|Part)\\s*(\\d+)\\s*={2,}\\s*",
                "(?:^|\\n)\\s*(?:PART|Part|SECTION|Section)\\s+(\\d+)\\s*(?:[:\\-—.])?\\s*",
                "(?:^|\\n)\\s*(?:Part|PART|Section|SECTION)\\s+(\\d+)\\s*$"
        };

        List<int[]> matches = new ArrayList<>();
        List<String> partNums = new ArrayList<>();

        for (String pattern : patterns) {
            var matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.MULTILINE).matcher(transcript);
            while (matcher.find()) {
                matches.add(new int[]{matcher.start(), matcher.end()});
                partNums.add(matcher.group(1));
            }
            if (matches.size() >= 2) break;
            matches.clear();
            partNums.clear();
        }

        if (matches.size() < 2) {
            Map<String, Object> single = new HashMap<>();
            single.put("label", "Full Transcript");
            single.put("text", transcript.trim());
            parts.add(single);
            return parts;
        }

        for (int i = 0; i < matches.size(); i++) {
            int start = matches.get(i)[1];
            int end = i + 1 < matches.size() ? matches.get(i + 1)[0] : transcript.length();
            String text = transcript.substring(start, end).trim();
            if (!text.isEmpty()) {
                Map<String, Object> part = new HashMap<>();
                part.put("label", "Part " + partNums.get(i));
                part.put("text", text);
                parts.add(part);
            }
        }

        return parts.isEmpty() ? List.of(Map.of("label", "Full Transcript", "text", transcript.trim())) : parts;
    }
}
