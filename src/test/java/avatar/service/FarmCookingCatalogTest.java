package avatar.service;

import avatar.Farm.FarmCookingCatalog;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmCookingCatalogTest {
    @Test public void parsesVariableSectionsAndFreezesIngredients() throws Exception {
        FarmCookingCatalog catalog = FarmCookingCatalog.parse(fixture(3, 207, 6, 2, 1));
        assertEquals("Rice", catalog.recipe(3).name());
        assertEquals(30, catalog.recipe(3).durationMinutes());
        assertEquals(Integer.valueOf(2), catalog.recipe(3).ingredients().get(6));
        assertNull(catalog.recipe(999));
        assertThrows(UnsupportedOperationException.class, () -> catalog.recipe(3).ingredients().put(1, 2));
    }

    @Test public void rejectsTruncationCountsDuplicatesAndUnknownReferences() throws Exception {
        byte[] valid = fixture(3, 207, 6, 2, 1);
        for (int length = 0; length < valid.length; length++) {
            byte[] truncated = java.util.Arrays.copyOf(valid, length);
            assertThrows(IOException.class, () -> FarmCookingCatalog.parse(truncated));
        }
        byte[] negative = valid.clone(); negative[0] = -1;
        assertThrows(IOException.class, () -> FarmCookingCatalog.parse(negative));
        assertThrows(IOException.class, () -> FarmCookingCatalog.parse(fixture(3, 208, 6, 2, 1)));
        assertThrows(IOException.class, () -> FarmCookingCatalog.parse(fixture(3, 207, 99, 2, 1)));
        assertThrows(IOException.class, () -> FarmCookingCatalog.parse(fixture(3, 207, 6, 0, 1)));
        assertThrows(IOException.class, () -> FarmCookingCatalog.parse(fixture(3, 207, 6, 2, 2)));
    }

    @Test public void validatesPublicRecipe() {
        assertThrows(IllegalArgumentException.class, () -> new FarmCookingCatalog.Recipe(0,"Rice",207,30,Map.of(6,2)));
        assertThrows(IllegalArgumentException.class, () -> new FarmCookingCatalog.Recipe(3,"Rice",207,0,Map.of(6,2)));
        assertThrows(IllegalArgumentException.class, () -> new FarmCookingCatalog.Recipe(3,"Rice",207,30,Map.of()));
    }

    @Test public void readsAuthoritativeAssetWhenExplicitlyProvided() throws Exception {
        String path = System.getProperty("avatar.test.farmCookingCatalog");
        org.junit.Assume.assumeNotNull(path);
        FarmCookingCatalog catalog = FarmCookingCatalog.parse(Files.readAllBytes(Path.of(path)));
        assertEquals(207, catalog.recipe(3).outputId());
        assertEquals(30, catalog.recipe(3).durationMinutes());
        assertEquals(Map.of(6,1440), catalog.recipe(3).ingredients());
        assertEquals(Map.of(215,240), catalog.recipe(14).ingredients());
        assertEquals(Map.of(0,1000), catalog.recipe(30).ingredients());
    }

    private static byte[] fixture(int id, int output, int ingredient, int quantity, int recipes) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeShort(1); out.writeByte(6); out.writeUTF("Crop"); out.write(new byte[28]);
        out.writeShort(0); out.writeShort(1); // Prices, then crop's second price.
        out.writeShort(0); // Animals.
        out.writeByte(1); out.write(new byte[6]); out.writeUTF("Support"); out.writeInt(0);
        out.writeByte(1); out.writeInt(0); out.writeUTF("Misc"); out.writeInt(0);
        out.writeByte(1); out.writeShort(30); out.writeUTF("Next crop"); out.write(new byte[27]);
        out.writeShort(recipes);
        for (int i = 0; i < recipes; i++) {
            out.writeShort(id); out.writeUTF("Rice"); out.writeShort(output); out.writeShort(30);
            out.writeShort(1); out.writeShort(ingredient); out.writeShort(quantity);
        }
        out.writeByte(1); out.writeShort(207); out.writeShort(1); out.writeUTF("Rice product"); out.writeLong(0);
        return bytes.toByteArray();
    }
}
