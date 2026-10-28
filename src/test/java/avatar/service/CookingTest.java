package avatar.service;

import avatar.Farm.*;
import avatar.model.User;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;
import java.sql.SQLException;

public class CookingTest {
    private static final FarmCookingCatalog.Recipe RICE =
            new FarmCookingCatalog.Recipe(3,"Cơm",207,30,Map.of(6,1440));
    private static class MemoryUser extends User {
        boolean fail;
        @Override public synchronized void saveFarmData(int id) throws SQLException {
            if (fail) throw new SQLException("synthetic failure");
        }
    }
    @Test public void cookingConsumesOnceWaitsAndCreditsOneProduct() throws Exception {
        MemoryUser user=new MemoryUser(); user.NongSan.add(new NongSan(6,2880));
        user.startCooking(RICE,1000);
        assertEquals(1440,user.NongSan.get(0).getSoluong());
        assertEquals(1801000,user.getCookingReadyMs());
        try { user.startCooking(RICE,2000); fail(); } catch (IllegalArgumentException expected) { }
        try { user.collectCooking(1800999); fail(); } catch (IllegalArgumentException expected) { }
        assertTrue(user.NongSanDacBiet.isEmpty());
        assertEquals(207,user.collectCooking(1801000));
        assertEquals(1,user.NongSanDacBiet.get(0).getSoluong());
        assertEquals(0,user.getCookingRecipe());
        try { user.collectCooking(1801001); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(1,user.NongSanDacBiet.get(0).getSoluong());
    }
    @Test public void missingIngredientsFullStockAndFailedSaveLeaveStateUnchanged() throws Exception {
        MemoryUser user=new MemoryUser(); user.NongSan.add(new NongSan(6,1439));
        try { user.startCooking(RICE,1000); fail(); } catch (IllegalArgumentException expected) { }
        user.NongSan.get(0).setSoluong(1440); user.fail=true;
        try { user.startCooking(RICE,1000); fail(); } catch (SQLException expected) { }
        assertEquals(1440,user.NongSan.get(0).getSoluong()); assertEquals(0,user.getCookingRecipe());
        user.fail=false; user.startCooking(RICE,1000);
        user.NongSanDacBiet.add(new NongSanDacBiet(207,Short.MAX_VALUE));
        try { user.collectCooking(1801000); fail(); } catch (IllegalArgumentException expected) { }
        user.NongSanDacBiet.get(0).setSoluong(0); user.fail=true;
        try { user.collectCooking(1801000); fail(); } catch (SQLException expected) { }
        assertEquals(3,user.getCookingRecipe()); assertEquals(0,user.NongSanDacBiet.get(0).getSoluong());
    }
    @Test public void malformedOrDuplicateIngredientsCannotBeConsumed() throws Exception {
        MemoryUser user=new MemoryUser();user.NongSan.add(new NongSan(6,1440));user.NongSan.add(new NongSan(6,1440));
        try { user.startCooking(RICE,1000); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(0,user.getCookingRecipe());
    }
}
