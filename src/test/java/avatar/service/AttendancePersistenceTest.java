package avatar.service;

import avatar.db.DbManager;
import avatar.item.Part;
import avatar.item.PartManager;
import avatar.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Runs only against the disposable fixture DB. */
public class AttendancePersistenceTest {
    private User player(int id, int scores) {
        User user = new User(); user.setId(id); user.setScores(scores);
        user.setChests(new ArrayList<>()); user.setChestSlot((byte) 10); return user;
    }
    @Test public void dailyRewardIsAtomicAndReplaySafeAcrossReloadAndConcurrency() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedFarmDb"));
        DbManager db = DbManager.getInstance(); db.start();
        Part ticket = Part.builder().id(593).type((short)-2).build();
        PartManager.getInstance().getParts().add(ticket);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try (Connection connection = db.getConnection()) {
            assertEquals("avatar2d_starfruit_test", connection.getCatalog());
            connection.createStatement().execute("CREATE TABLE players (user_id INT PRIMARY KEY, chests LONGTEXT NOT NULL, scores INT CHECK (scores < 100))");
            for (int i = 0; i < 2; i++) connection.createStatement().execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS attendance_date DATE NULL DEFAULT NULL");
            connection.createStatement().execute("INSERT INTO players VALUES (71, '[]', 0, NULL), (92, '[]', 90, NULL)");
            User user = player(71, 0);
            Future<Boolean> first = workers.submit(user::claimDailyAttendance);
            Future<Boolean> second = workers.submit(user::claimDailyAttendance);
            assertTrue(first.get() ^ second.get());
            assertEquals(20, user.getScores()); assertEquals(1, user.chests.get(0).getQuantity());
            User reloaded = player(71, 20);
            assertFalse(reloaded.claimDailyAttendance());
            try (ResultSet row = connection.createStatement().executeQuery("SELECT scores,chests,attendance_date FROM players WHERE user_id=71")) {
                assertTrue(row.next()); assertEquals(20, row.getInt(1));
                assertTrue(row.getString(2).contains("593")); assertNotNull(row.getDate(3));
            }
            User failed = player(92, 90);
            try { failed.claimDailyAttendance(); fail("CHECK must reject entire reward"); }
            catch (SQLException expected) { }
            assertEquals(90, failed.getScores()); assertTrue(failed.chests.isEmpty());
            try (ResultSet row = connection.createStatement().executeQuery("SELECT chests,attendance_date FROM players WHERE user_id=92")) {
                assertTrue(row.next()); assertEquals("[]", row.getString(1)); assertNull(row.getDate(2));
            }
            connection.createStatement().execute("UPDATE players SET attendance_date='2000-01-01' WHERE user_id=71");
            assertTrue(user.claimDailyAttendance());
            assertEquals(40, user.getScores()); assertEquals(2, user.chests.get(0).getQuantity());
            connection.createStatement().execute("UPDATE players SET attendance_date='2099-01-01' WHERE user_id=71");
            assertFalse(user.claimDailyAttendance());
        } finally { workers.shutdownNow(); PartManager.getInstance().getParts().remove(ticket); db.shutdown(); }
    }
}
