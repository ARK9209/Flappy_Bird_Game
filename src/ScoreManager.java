import java.sql.*;
import java.util.*;

/** Owns score SQL and retains an in-memory best when MySQL is unavailable. */
public class ScoreManager {
    private int localBest;
    private String lastError = "";
    public void record(int score) { localBest = Math.max(localBest, score); }
    public SaveResult saveScore(String playerName, int score) {
        String insertSql = "INSERT INTO scores (player_name, score) VALUES (?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, playerName); insert.setInt(2, score); insert.executeUpdate();
            record(score); lastError = ""; return SaveResult.SAVED;
        } catch (SQLException e) { setDatabaseError("save score", e); return SaveResult.DATABASE_UNAVAILABLE; }
    }
    public List<DatabaseScore> getTopScores(int limit) {
        if (limit <= 0) return Collections.emptyList();
        List<DatabaseScore> result = new ArrayList<>();
        String sql = "SELECT player_name, score, created_at FROM scores ORDER BY score DESC, created_at ASC LIMIT ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(new DatabaseScore(rows.getString("player_name"), rows.getInt("score"), rows.getTimestamp("created_at"))); }
            lastError = "";
        } catch (SQLException e) { setDatabaseError("load leaderboard", e); }
        return result;
    }
    public int getHighestScore() {
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(MAX(score), 0) AS highest_score FROM scores"); ResultSet rows = statement.executeQuery()) {
            int highest = rows.next() ? rows.getInt("highest_score") : 0; localBest = Math.max(localBest, highest); lastError = ""; return highest;
        } catch (SQLException e) { setDatabaseError("load highest score", e); return localBest; }
    }
    public void refreshBest() { getHighestScore(); }
    public int getBest() { return localBest; }
    public String getLastError() { return lastError; }
    private void setDatabaseError(String action, SQLException error) { lastError = "Database unavailable; " + action + " was skipped."; System.err.println(lastError + " " + error.getMessage()); }
    public enum SaveResult { SAVED, DATABASE_UNAVAILABLE }
    public static final class DatabaseScore {
        private final String playerName; private final int score; private final Timestamp createdAt;
        public DatabaseScore(String playerName, int score, Timestamp createdAt) { this.playerName = playerName; this.score = score; this.createdAt = createdAt; }
        public String getPlayerName() { return playerName; } public int getScore() { return score; } public Timestamp getCreatedAt() { return createdAt; }
    }
}
