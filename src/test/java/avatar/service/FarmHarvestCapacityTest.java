package avatar.service;

import avatar.Farm.LandItem;
import avatar.Farm.NongSan;
import avatar.Farm.NongSanDacBiet;
import avatar.Farm.farmItem;
import avatar.item.PartManager;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class FarmHarvestCapacityTest {
    @Test public void starfruitDoesNotCreateAnUnrepresentableInventoryEntry() throws Exception {
        StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser();
        for (int i = 0; i < 127; i++) user.NongSanDacBiet.add(new NongSanDacBiet(i, 1));
        try { user.harvestStarfruit(); fail("Full inventory must reject harvest"); }
        catch (SQLException expected) { }
        assertEquals(127, user.NongSanDacBiet.size());
        assertEquals(180, user.getStarfruitQuantity());
        assertEquals(0, user.saves);
    }

    @Test public void fullInventoryStillAcceptsExistingStarfruit() throws Exception {
        StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser();
        for (int i = 0; i < 126; i++) user.NongSanDacBiet.add(new NongSanDacBiet(i, 1));
        user.NongSanDacBiet.add(new NongSanDacBiet(215, 1));
        assertEquals(180, user.harvestStarfruit());
        assertEquals(181, user.NongSanDacBiet.get(126).getSoluong());
        assertEquals(127, user.NongSanDacBiet.size());
    }

    @Test public void cropDoesNotCreateAnUnrepresentableInventoryEntry() throws Exception {
        StarfruitHarvestTest.SavedUser user = new StarfruitHarvestTest.SavedUser();
        for (int i = 0; i < 127; i++) user.NongSan.add(new NongSan(i, 1));
        farmItem crop = new farmItem(127, "Test crop", 5, 10, 20);
        user.landItems.add(new LandItem(0, 127, 100, 0, false, false, true, LocalDateTime.now().minusMinutes(10)));
        PartManager.getInstance().getFarmItems().add(crop);
        try {
            try { user.harvestCrop(0); fail("Full inventory must reject harvest"); }
            catch (IllegalArgumentException expected) { }
            assertEquals(127, user.NongSan.size());
            assertEquals(127, user.landItems.get(0).getType());
            assertEquals(0, user.saves);
        } finally { PartManager.getInstance().getFarmItems().remove(crop); }
    }
}
