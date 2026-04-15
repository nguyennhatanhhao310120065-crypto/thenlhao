package com.akatsuki.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscriptLine {
    private double startTimeSec;
    private double endTimeSec;
    private String speaker;
    private String text;
    private String rawLine;
    private boolean isPartHeader;
    private boolean isNarrator;

    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("^\\[?(\\d{1,2}):(\\d{2})\\]?\\s*");

    private static final Pattern NARRATOR_PATTERN =
            Pattern.compile("^(?:Narrator|NARRATOR|narrator)\\s*:\\s*", Pattern.CASE_INSENSITIVE);

    private static final Pattern SPEAKER_PATTERN =
            Pattern.compile("^(Speaker\\s*\\d+)\\s*:\\s*", Pattern.CASE_INSENSITIVE);

    // After timestamp: also allow proper names like "John:", "Mary Smith:"
    private static final Pattern SPEAKER_NAME_PATTERN =
            Pattern.compile("^([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)\\s*:\\s*");

    private static final Pattern PART_HEADER_PATTERN =
            Pattern.compile("^={2,}\\s*(?:PART|Part|part)\\s*\\d+\\s*={2,}$");

    public TranscriptLine() {}

    public TranscriptLine(double startTimeSec, String speaker, String text, String rawLine) {
        this.startTimeSec = startTimeSec;
        this.speaker = speaker;
        this.text = text;
        this.rawLine = rawLine;
    }

    public double getStartTimeSec() { return startTimeSec; }
    public void setStartTimeSec(double startTimeSec) { this.startTimeSec = startTimeSec; }
    public double getEndTimeSec() { return endTimeSec; }
    public void setEndTimeSec(double endTimeSec) { this.endTimeSec = endTimeSec; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
    public boolean isPartHeader() { return isPartHeader; }
    public void setPartHeader(boolean partHeader) { isPartHeader = partHeader; }
    public boolean isNarrator() { return isNarrator; }
    public void setNarrator(boolean narrator) { isNarrator = narrator; }

    public boolean hasTimestamp() { return startTimeSec >= 0; }

    public String getFormattedTime() {
        if (startTimeSec < 0) return "";
        int mins = (int) (startTimeSec / 60);
        int secs = (int) (startTimeSec % 60);
        return String.format("%d:%02d", mins, secs);
    }

    public static List<TranscriptLine> parse(String transcript) {
        List<TranscriptLine> lines = new ArrayList<>();
        if (transcript == null || transcript.isBlank()) return lines;

        String[] rawLines = transcript.split("\\n");
        String lastText = null;

        for (String raw : rawLines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;

            // Part headers: === PART X ===
            if (PART_HEADER_PATTERN.matcher(trimmed).matches()) {
                TranscriptLine line = new TranscriptLine(-1, null, trimmed, raw);
                line.setPartHeader(true);
                lines.add(line);
                lastText = null;
                continue;
            }

            // Also catch simpler part headers like "PART 1" or "Part 2:"
            if (trimmed.matches("^(?:PART|Part|SECTION|Section)\\s+\\d+\\s*[:\\-—.]?\\s*$")) {
                TranscriptLine line = new TranscriptLine(-1, null, trimmed, raw);
                line.setPartHeader(true);
                lines.add(line);
                lastText = null;
                continue;
            }

            double timeSec = -1;
            String remaining = trimmed;

            // Extract timestamp
            Matcher tMatcher = TIMESTAMP_PATTERN.matcher(remaining);
            if (tMatcher.find()) {
                int mins = Integer.parseInt(tMatcher.group(1));
                int secs = Integer.parseInt(tMatcher.group(2));
                timeSec = mins * 60.0 + secs;
                remaining = remaining.substring(tMatcher.end());
            }

            String speaker = null;
            boolean narrator = false;

            // Check for Narrator label first
            Matcher nMatcher = NARRATOR_PATTERN.matcher(remaining);
            if (nMatcher.find()) {
                speaker = "Narrator";
                narrator = true;
                remaining = remaining.substring(nMatcher.end()).trim();
            } else {
                // Check for "Speaker N:" pattern
                Matcher sMatcher = SPEAKER_PATTERN.matcher(remaining);
                if (sMatcher.find()) {
                    speaker = sMatcher.group(1).trim();
                    remaining = remaining.substring(sMatcher.end()).trim();
                } else if (timeSec >= 0) {
                    // Only after a timestamp, try proper name pattern
                    Matcher nameMatcher = SPEAKER_NAME_PATTERN.matcher(remaining);
                    if (nameMatcher.find()) {
                        String candidate = nameMatcher.group(1);
                        // Reject common English words that start sentences
                        if (!isCommonSentenceStart(candidate)) {
                            speaker = candidate;
                            remaining = remaining.substring(nameMatcher.end()).trim();
                        }
                    }
                }
            }

            if (remaining.isEmpty()) continue;

            // Deduplication: skip if the text is identical to the previous line
            String normText = remaining.replaceAll("\\s+", " ").trim();
            if (lastText != null && lastText.equals(normText)) continue;
            lastText = normText;

            TranscriptLine line = new TranscriptLine(timeSec, speaker, remaining, raw);
            line.setNarrator(narrator);
            lines.add(line);
        }

        // Compute end times from next line's start time
        for (int i = 0; i < lines.size(); i++) {
            TranscriptLine line = lines.get(i);
            if (line.hasTimestamp()) {
                double endTime = -1;
                for (int j = i + 1; j < lines.size(); j++) {
                    if (lines.get(j).hasTimestamp()) {
                        endTime = lines.get(j).getStartTimeSec();
                        break;
                    }
                }
                line.setEndTimeSec(endTime > 0 ? endTime : line.getStartTimeSec() + 30);
            }
        }

        return lines;
    }

    private static boolean isCommonSentenceStart(String word) {
        return switch (word.toLowerCase()) {
            case "the", "this", "that", "these", "those", "there", "here",
                 "now", "then", "next", "first", "second", "third",
                 "today", "tomorrow", "yesterday", "before", "after",
                 "welcome", "hello", "hi", "good", "well", "yes", "no", "ok", "okay",
                 "please", "thank", "thanks", "so", "but", "and", "or", "because",
                 "however", "also", "all", "write", "listen", "look", "read",
                 "at", "in", "on", "for", "with", "from", "to", "by",
                 "very", "quite", "really", "oh", "um", "uh" -> true;
            default -> false;
        };
    }

    public static TranscriptLine findLineAtTime(List<TranscriptLine> lines, double timeSec) {
        TranscriptLine best = null;
        for (TranscriptLine line : lines) {
            if (!line.hasTimestamp()) continue;
            if (line.getStartTimeSec() <= timeSec && timeSec < line.getEndTimeSec()) {
                return line;
            }
            if (line.getStartTimeSec() <= timeSec) {
                best = line;
            }
        }
        return best;
    }

    public static TranscriptLine findLineContainingQuote(List<TranscriptLine> lines, String quote) {
        if (quote == null || quote.isEmpty()) return null;
        String normQuote = quote.toLowerCase().replaceAll("\\s+", " ").trim();

        // 1. Exact or Prefix containment on single line 
        for (TranscriptLine line : lines) {
            if (line.getText() == null || line.isPartHeader()) continue;
            String normText = line.getText().toLowerCase().replaceAll("\\s+", " ").trim();
            // If the line contains the whole quote or a good prefix of it
            if (normText.contains(normQuote)) return line;
            if (normQuote.length() > 20 && normText.contains(normQuote.substring(0, 20))) return line;
            // Only allow reverse constraint if the text is substantial
            if (normText.length() > 15 && normQuote.contains(normText)) return line;
        }

        // 2. Continuous text mapping (robust across line breaks)
        StringBuilder fullText = new StringBuilder();
        List<Integer> lineStartIndices = new ArrayList<>();
        List<TranscriptLine> validLines = new ArrayList<>();

        for (TranscriptLine l : lines) {
            if (l.getText() != null && !l.isPartHeader()) {
                lineStartIndices.add(fullText.length());
                validLines.add(l);
                fullText.append(l.getText().toLowerCase().replaceAll("\\s+", " ").trim()).append(" ");
            }
        }
        
        String full = fullText.toString();
        int idx = full.indexOf(normQuote);
        if (idx < 0) {
            // Try matching prefix of quote
            String pref = normQuote.substring(0, Math.min(30, normQuote.length()));
            idx = full.indexOf(pref);
        }

        if (idx >= 0) {
            // Find which line corresponds to this idx
            for (int i = validLines.size() - 1; i >= 0; i--) {
                if (idx >= lineStartIndices.get(i)) {
                    return validLines.get(i);
                }
            }
        }

        // 3. Fallback: Word overlap scoring (if Gemini paraphrased heavily)
        String[] quoteWords = normQuote.split("\\s+");
        TranscriptLine bestLine = null;
        int bestScore = 0;
        for (TranscriptLine line : lines) {
            if (line.getText() == null || line.isPartHeader()) continue;
            String normText = line.getText().toLowerCase().replaceAll("\\s+", " ").trim();
            int score = 0;
            for (String word : quoteWords) {
                if (word.length() > 3 && normText.contains(word)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                bestLine = line;
            }
        }
        if (bestLine != null && bestScore >= Math.max(2, quoteWords.length * 0.3)) {
            return bestLine;
        }

        return null;
    }

    public static double estimateTimeForQuote(List<TranscriptLine> lines, String quote, double totalDurationSec) {
        TranscriptLine line = findLineContainingQuote(lines, quote);
        if (line != null && line.hasTimestamp()) {
            return line.getStartTimeSec();
        }
        return 0; // Better safe than randomly forwarding the audio to extreme proportions
    }

    public static double estimateEndTimeForQuote(List<TranscriptLine> lines, String quote, double totalDurationSec) {
        TranscriptLine line = findLineContainingQuote(lines, quote);
        if (line != null && line.hasTimestamp()) {
            return line.getEndTimeSec();
        }
        double startSec = estimateTimeForQuote(lines, quote, totalDurationSec);
        return startSec + Math.max(3.0, quote.length() * 0.06) + 1.0;
    }
}
