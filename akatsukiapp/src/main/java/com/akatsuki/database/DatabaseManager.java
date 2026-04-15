package com.akatsuki.database;

import com.akatsuki.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection conn;
    private final Gson gson = new Gson();

    private DatabaseManager(String dbPath) {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            conn.setAutoCommit(true);
            initTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static synchronized DatabaseManager getInstance(String dbPath) {
        if (instance == null) instance = new DatabaseManager(dbPath);
        return instance;
    }

    public static DatabaseManager getInstance() {
        if (instance == null) throw new IllegalStateException("DatabaseManager not initialized");
        return instance;
    }

    private void initTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE,
                    password_hash TEXT,
                    role INTEGER,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS question_banks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bank_name TEXT,
                    created_by INTEGER,
                    is_public BOOLEAN DEFAULT 0,
                    exam_type TEXT,
                    audio_url TEXT,
                    transcript TEXT,
                    start_time REAL DEFAULT 0,
                    end_time REAL DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(created_by) REFERENCES users(id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bank_id INTEGER,
                    section_number INTEGER,
                    instruction TEXT,
                    FOREIGN KEY(bank_id) REFERENCES question_banks(id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    section_id INTEGER,
                    question_number INTEGER,
                    question_text TEXT,
                    correct_answer TEXT,
                    explanation TEXT,
                    question_type TEXT,
                    options TEXT,
                    transcript_quote TEXT DEFAULT '',
                    FOREIGN KEY(section_id) REFERENCES sections(id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS student_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    bank_id INTEGER,
                    score REAL,
                    total_questions INTEGER DEFAULT 0,
                    correct_count INTEGER DEFAULT 0,
                    completed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id),
                    FOREIGN KEY(bank_id) REFERENCES question_banks(id)
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS saved_questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    question_id INTEGER,
                    saved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id),
                    FOREIGN KEY(question_id) REFERENCES questions(id),
                    UNIQUE(user_id, question_id)
                )""");
        }
    }

    // ---- Auth ----
    public User login(String username, String password) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            if (!rs.getString("password_hash").equals(password)) return null;
            return new User(rs.getInt("id"), rs.getString("username"), rs.getInt("role"));
        } catch (SQLException e) {
            return null;
        }
    }

    public User register(String username, String password, int role) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setInt(3, role);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return new User(keys.getInt(1), username, role);
            return null;
        }
    }

    // ---- Question Banks ----
    public List<QuestionBank> getBanks(int userId) {
        List<QuestionBank> banks = new ArrayList<>();
        String sql = """
            SELECT qb.*, u.username as creator_name,
                (SELECT COUNT(*) FROM questions q JOIN sections s ON q.section_id = s.id WHERE s.bank_id = qb.id) as question_count
            FROM question_banks qb
            JOIN users u ON qb.created_by = u.id
            WHERE qb.is_public = 1 OR qb.created_by = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QuestionBank b = new QuestionBank();
                b.setId(rs.getInt("id"));
                b.setBankName(rs.getString("bank_name"));
                b.setCreatedBy(rs.getInt("created_by"));
                b.setPublic(rs.getBoolean("is_public"));
                b.setExamType(rs.getString("exam_type"));
                b.setAudioUrl(rs.getString("audio_url"));
                b.setTranscript(rs.getString("transcript"));
                b.setStartTime(rs.getDouble("start_time"));
                b.setEndTime(rs.getDouble("end_time"));
                b.setCreatedAt(rs.getString("created_at"));
                b.setCreatorName(rs.getString("creator_name"));
                b.setQuestionCount(rs.getInt("question_count"));
                banks.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return banks;
    }

    public QuestionBank getBankById(int bankId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM question_banks WHERE id = ?")) {
            ps.setInt(1, bankId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            QuestionBank b = new QuestionBank();
            b.setId(rs.getInt("id"));
            b.setBankName(rs.getString("bank_name"));
            b.setCreatedBy(rs.getInt("created_by"));
            b.setPublic(rs.getBoolean("is_public"));
            b.setExamType(rs.getString("exam_type"));
            b.setAudioUrl(rs.getString("audio_url"));
            b.setTranscript(rs.getString("transcript"));
            b.setStartTime(rs.getDouble("start_time"));
            b.setEndTime(rs.getDouble("end_time"));
            b.setCreatedAt(rs.getString("created_at"));

            List<Section> sections = getSections(bankId);
            b.setSections(sections);
            return b;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Section> getSections(int bankId) throws SQLException {
        List<Section> sections = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sections WHERE bank_id = ?")) {
            ps.setInt(1, bankId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Section s = new Section();
                s.setId(rs.getInt("id"));
                s.setBankId(rs.getInt("bank_id"));
                s.setSectionNumber(rs.getInt("section_number"));
                s.setInstruction(rs.getString("instruction"));
                s.setQuestions(getQuestions(s.getId()));
                sections.add(s);
            }
        }
        return sections;
    }

    private List<Question> getQuestions(int sectionId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM questions WHERE section_id = ?")) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setSectionId(rs.getInt("section_id"));
                q.setQuestionNumber(rs.getInt("question_number"));
                q.setQuestionText(rs.getString("question_text"));
                q.setCorrectAnswer(rs.getString("correct_answer"));
                q.setExplanation(rs.getString("explanation"));
                q.setQuestionType(rs.getString("question_type"));
                String optJson = rs.getString("options");
                if (optJson != null && !optJson.isEmpty()) {
                    q.setOptions(gson.fromJson(optJson, new TypeToken<List<String>>() {}.getType()));
                }
                q.setTranscriptQuote(rs.getString("transcript_quote"));
                questions.add(q);
            }
        }
        return questions;
    }

    public int saveBank(String bankName, int createdBy, String examType, String audioUrl,
                        String transcript, double startTime, double endTime, List<Section> sections) {
        try {
            conn.setAutoCommit(false);
            int bankId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO question_banks (bank_name, created_by, exam_type, audio_url, transcript, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, bankName);
                ps.setInt(2, createdBy);
                ps.setString(3, examType);
                ps.setString(4, audioUrl);
                ps.setString(5, transcript);
                ps.setDouble(6, startTime);
                ps.setDouble(7, endTime);
                ps.executeUpdate();
                bankId = ps.getGeneratedKeys().getInt(1);
            }
            for (Section section : sections) {
                int sectionId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sections (bank_id, section_number, instruction) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, bankId);
                    ps.setInt(2, section.getSectionNumber());
                    ps.setString(3, section.getInstruction());
                    ps.executeUpdate();
                    sectionId = ps.getGeneratedKeys().getInt(1);
                }
                for (Question q : section.getQuestions()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO questions (section_id, question_number, question_text, correct_answer, explanation, question_type, options, transcript_quote) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, sectionId);
                        ps.setInt(2, q.getQuestionNumber());
                        ps.setString(3, q.getQuestionText());
                        ps.setString(4, q.getCorrectAnswer());
                        ps.setString(5, q.getExplanation() != null ? q.getExplanation() : "");
                        ps.setString(6, q.getQuestionType() != null ? q.getQuestionType() : "mcq");
                        ps.setString(7, gson.toJson(q.getOptions()));
                        ps.setString(8, q.getTranscriptQuote() != null ? q.getTranscriptQuote() : "");
                        ps.executeUpdate();
                    }
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            return bankId;
        } catch (SQLException e) {
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return -1;
        }
    }

    public boolean deleteBank(int bankId, int userId) {
        try (PreparedStatement check = conn.prepareStatement("SELECT created_by FROM question_banks WHERE id = ?")) {
            check.setInt(1, bankId);
            ResultSet rs = check.executeQuery();
            if (!rs.next() || rs.getInt("created_by") != userId) return false;
        } catch (SQLException e) {
            return false;
        }
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM sections WHERE bank_id = ?")) {
                ps.setInt(1, bankId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int sid = rs.getInt("id");
                    conn.createStatement().executeUpdate("DELETE FROM saved_questions WHERE question_id IN (SELECT id FROM questions WHERE section_id = " + sid + ")");
                    conn.createStatement().executeUpdate("DELETE FROM questions WHERE section_id = " + sid);
                }
            }
            conn.createStatement().executeUpdate("DELETE FROM sections WHERE bank_id = " + bankId);
            conn.createStatement().executeUpdate("DELETE FROM student_results WHERE bank_id = " + bankId);
            conn.createStatement().executeUpdate("DELETE FROM question_banks WHERE id = " + bankId);
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        }
    }

    public boolean toggleVisibility(int bankId, int userId, boolean isPublic) {
        try (PreparedStatement check = conn.prepareStatement("SELECT created_by FROM question_banks WHERE id = ?")) {
            check.setInt(1, bankId);
            ResultSet rs = check.executeQuery();
            if (!rs.next() || rs.getInt("created_by") != userId) return false;
        } catch (SQLException e) {
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE question_banks SET is_public = ? WHERE id = ?")) {
            ps.setInt(1, isPublic ? 1 : 0);
            ps.setInt(2, bankId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateTranscript(int bankId, String newTranscript) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE question_banks SET transcript = ? WHERE id = ?")) {
            ps.setString(1, newTranscript);
            ps.setInt(2, bankId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---- Results ----
    public boolean saveResult(int userId, int bankId, double score, int totalQuestions, int correctCount) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO student_results (user_id, bank_id, score, total_questions, correct_count) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, userId);
            if (bankId > 0) ps.setInt(2, bankId); else ps.setNull(2, Types.INTEGER);
            ps.setDouble(3, score);
            ps.setInt(4, totalQuestions);
            ps.setInt(5, correctCount);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<StudentResult> getResults(int userId) {
        List<StudentResult> results = new ArrayList<>();
        String sql = """
            SELECT sr.*, COALESCE(qb.bank_name, 'Practice Session') as bank_name,
                   COALESCE(qb.exam_type, 'Practice') as exam_type
            FROM student_results sr
            LEFT JOIN question_banks qb ON sr.bank_id = qb.id
            WHERE sr.user_id = ? ORDER BY sr.completed_at DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StudentResult r = new StudentResult();
                r.setId(rs.getInt("id"));
                r.setUserId(rs.getInt("user_id"));
                r.setBankId(rs.getInt("bank_id"));
                r.setScore(rs.getDouble("score"));
                r.setTotalQuestions(rs.getInt("total_questions"));
                r.setCorrectCount(rs.getInt("correct_count"));
                r.setCompletedAt(rs.getString("completed_at"));
                r.setBankName(rs.getString("bank_name"));
                r.setExamType(rs.getString("exam_type"));
                results.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // ---- Saved Questions ----
    public boolean saveQuestion(int userId, int questionId) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO saved_questions (user_id, question_id) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, questionId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean unsaveQuestion(int userId, int questionId) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM saved_questions WHERE user_id = ? AND question_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, questionId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Question> getSavedQuestions(int userId) {
        List<Question> questions = new ArrayList<>();
        String sql = """
            SELECT q.*, qb.bank_name, qb.exam_type, qb.audio_url FROM questions q
            JOIN saved_questions sq ON q.id = sq.question_id
            JOIN sections s ON q.section_id = s.id
            JOIN question_banks qb ON s.bank_id = qb.id
            WHERE sq.user_id = ? ORDER BY sq.saved_at DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setQuestionNumber(rs.getInt("question_number"));
                q.setQuestionText(rs.getString("question_text"));
                q.setCorrectAnswer(rs.getString("correct_answer"));
                q.setExplanation(rs.getString("explanation"));
                q.setQuestionType(rs.getString("question_type"));
                String optJson = rs.getString("options");
                if (optJson != null && !optJson.isEmpty()) {
                    q.setOptions(gson.fromJson(optJson, new TypeToken<List<String>>() {}.getType()));
                }
                q.setTranscriptQuote(rs.getString("transcript_quote"));
                q.setPartLabel(rs.getString("bank_name") + " | " + rs.getString("exam_type"));
                questions.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }

    public List<Integer> getSavedQuestionIds(int userId) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT question_id FROM saved_questions WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("question_id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
}
