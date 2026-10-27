package avatar.service;

import avatar.Farm.*;
import avatar.db.DbManager;
import avatar.model.User;
import avatar.model.GameData;
import avatar.item.PartManager;
import avatar.message.HomeMsgHandler;
import avatar.constants.Cmd;
import avatar.network.Message;
import java.time.LocalDateTime;
import org.junit.*;
import java.net.*;
import java.sql.*;
import static org.junit.Assert.*;

/** Opt-in check against a disposable MariaDB, never the game database. */
public class FarmPersistenceTest {
    @Test public void harvestSurvivesReloadWithoutDuplicateCredit() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("avatar.test.isolatedFarmDb"));
        DbManager db = DbManager.getInstance(); db.start();
        try (Connection connection = db.getConnection()) {
            assertEquals("avatar2d_starfruit_test", connection.getCatalog());
            connection.createStatement().execute("CREATE TABLE farm_data (user_id INT PRIMARY KEY, land_data LONGTEXT, animal_data LONGTEXT, hatgiong LONGTEXT, phanbon LONGTEXT, nongsan LONGTEXT, nongsandacbiet LONGTEXT, persistence_revision BIGINT NOT NULL DEFAULT 0)");
            // Exercise the same additive migration as deploy, including repeat application.
            for (int i = 0; i < 2; i++) connection.createStatement().execute("ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS starfruit_quantity SMALLINT UNSIGNED NOT NULL DEFAULT 180");
            for (int i = 0; i < 2; i++) connection.createStatement().execute("ALTER TABLE farm_data ADD COLUMN IF NOT EXISTS starfruit_last_harvest_ms BIGINT NOT NULL DEFAULT 0");
            connection.createStatement().execute("CREATE TABLE farmitems (id INT PRIMARY KEY, name VARCHAR(40), time INT, quantity INT, sell INT)");
            connection.createStatement().execute("INSERT INTO farmitems VALUES (5, 'test crop', 5, 120, 20)");
            PartManager.getInstance().loadFarmItemData();
            GameData.getInstance().loadItemfarm();
            assertEquals(120, PartManager.getInstance().findFarmitemByID(5).getQuantity());
            assertEquals(1, GameData.getInstance().getFarmItems().size());
            try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                 Socket peer = new Socket(listener.getInetAddress(), listener.getLocalPort());
                 Socket accepted = listener.accept()) {
                StarfruitHarvestTest.CaptureSession session = new StarfruitHarvestTest.CaptureSession(accepted);
                User user = new User(); user.setId(7); user.setSession(session); session.user = user;
                user.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
                user.landItems.add(new LandItem(0, 5, 100, 0, false, false, true, LocalDateTime.now().minusMinutes(10)));
                user.PhanBon.add(new PhanBon(1, 2));
                user.NongSan.add(new NongSan(3, 4));
                assertEquals(120, user.harvestCrop(1));
                assertEquals(180, user.harvestStarfruit());
                User reloaded = new User(); reloaded.setId(7);
                reloaded.loadFarmData(7);
                assertNull(reloaded.landItems.get(0).getPlantedTime());
                assertEquals(user.getStarfruitLastHarvestMs(), reloaded.getStarfruitLastHarvestMs());
                assertEquals(0, reloaded.getStarfruitQuantity());
                assertEquals(180, reloaded.NongSanDacBiet.get(0).getSoluong());
                assertEquals(2, reloaded.PhanBon.get(0).getSoluong());
                assertEquals(4, reloaded.NongSan.get(0).getSoluong());
                assertEquals(120, reloaded.NongSan.get(1).getSoluong());
                assertEquals(-1, reloaded.landItems.get(1).getType());
                try { reloaded.harvestCrop(1); fail("Cleared crop must not credit again"); }
                catch (IllegalArgumentException expected) { }
                reloaded.setSession(session); session.user = reloaded;
                reloaded.setXu(5000);
                session.messages.clear();
                Message purchase = new Message(Cmd.BUY_ITEM_HOUSE);
                purchase.writer().writeShort(1); purchase.writer().writeByte(0); purchase.writer().writeByte(0); purchase.writer().writeByte(0);
                new HomeMsgHandler(session).onMessage(new Message(Cmd.BUY_ITEM_HOUSE, purchase.getData()));
                assertEquals(5000, reloaded.getXu());
                assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
                assertEquals(0, reloaded.harvestStarfruit());
                assertEquals(180, reloaded.NongSanDacBiet.get(0).getSoluong());
            }
        } finally { db.shutdown(); }
    }
}
