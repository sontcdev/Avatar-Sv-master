package avatar.Farm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reads the same farm-info catalog supplied to the bundled 2.5.8 client. */
public final class FarmCookingCatalog {
    public record Recipe(int id, String name, int outputId, int durationMinutes,
                         Map<Integer, Integer> ingredients) {
        public Recipe {
            if (id <= 0 || id > Short.MAX_VALUE || name == null || name.isBlank()
                    || outputId <= 0 || outputId > Short.MAX_VALUE
                    || durationMinutes <= 0 || durationMinutes > Short.MAX_VALUE
                    || ingredients == null || ingredients.isEmpty()) {
                throw new IllegalArgumentException("Invalid cooking recipe");
            }
            ingredients = Map.copyOf(ingredients);
            for (Map.Entry<Integer, Integer> ingredient : ingredients.entrySet()) {
                if (ingredient.getKey() < 0 || ingredient.getKey() > Short.MAX_VALUE
                        || ingredient.getValue() <= 0 || ingredient.getValue() > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Invalid cooking ingredient");
                }
            }
        }
    }

    private final Map<Integer, Recipe> recipes;

    private FarmCookingCatalog(Map<Integer, Recipe> recipes) {
        this.recipes = Map.copyOf(recipes);
    }

    public Recipe recipe(int id) {
        return recipes.get(id);
    }

    public static FarmCookingCatalog parse(byte[] data) throws IOException {
        if (data == null) throw new IOException("Missing farm catalog");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            Set<Integer> ingredientIds = new HashSet<>();
            int cropCount = count(in, false);
            for (int i = 0; i < cropCount; i++) {
                addId(ingredientIds, in.readByte());
                in.readUTF();
                skip(in, 2 + 13 * 2);
            }
            int prices = count(in, false);
            skip(in, prices * 3 + cropCount * 2 + prices * 2);
            int animals = count(in, false);
            for (int i = 0; i < animals; i++) {
                addId(ingredientIds, in.readByte()); in.readUTF(); in.readUTF();
                skip(in, 4 + 6 * 2 + 1 + 36 + 1 + 3 * 2);
            }
            int support = count(in, true);
            for (int i = 0; i < support; i++) {
                skip(in, 6); in.readUTF(); skip(in, 4);
            }
            int misc = count(in, true);
            for (int i = 0; i < misc; i++) {
                skip(in, 4); in.readUTF(); skip(in, 4);
            }
            int nextCrops = count(in, true);
            for (int i = 0; i < nextCrops; i++) {
                addId(ingredientIds, in.readShort()); in.readUTF(); skip(in, 5 * 2 + 1 + 8 * 2);
            }
            Map<Integer, Recipe> recipes = new HashMap<>();
            int recipeCount = count(in, false);
            for (int i = 0; i < recipeCount; i++) {
                int id = in.readShort();
                String name = in.readUTF();
                int output = in.readShort();
                int duration = in.readShort();
                int ingredientCount = count(in, false);
                Map<Integer, Integer> ingredients = new HashMap<>();
                for (int j = 0; j < ingredientCount; j++) {
                    int ingredient = in.readShort();
                    if (ingredients.put(ingredient, (int) in.readShort()) != null) {
                        throw new IOException("Duplicate cooking ingredient: " + ingredient);
                    }
                }
                Recipe recipe = new Recipe(id, name, output, duration, ingredients);
                if (recipes.put(id, recipe) != null) throw new IOException("Duplicate recipe: " + id);
            }
            Set<Integer> products = new HashSet<>();
            int productCount = count(in, true);
            for (int i = 0; i < productCount; i++) {
                int productId = in.readShort();
                if (productId <= 0) throw new IOException("Invalid cooking product ID");
                addId(products, productId); in.readShort(); in.readUTF(); skip(in, 8);
            }
            if (in.available() != 0) throw new IOException("Trailing farm catalog bytes");
            for (Recipe recipe : recipes.values()) {
                if (!products.contains(recipe.outputId())) throw new IOException("Unknown cooking output");
                for (int ingredient : recipe.ingredients().keySet()) {
                    if (!ingredientIds.contains(ingredient) && !products.contains(ingredient)) {
                        throw new IOException("Unknown cooking ingredient: " + ingredient);
                    }
                }
            }
            return new FarmCookingCatalog(recipes);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid farm cooking catalog", exception);
        }
    }

    private static int count(DataInputStream in, boolean byteCount) throws IOException {
        int value = byteCount ? in.readByte() : in.readShort();
        if (value < 0 || value > in.available()) throw new IOException("Invalid farm catalog count");
        return value;
    }

    private static void skip(DataInputStream in, int length) throws IOException {
        in.skipNBytes(length);
    }

    private static void addId(Set<Integer> ids, int id) throws IOException {
        if (id < 0 || !ids.add(id)) throw new IOException("Invalid or duplicate catalog ID: " + id);
    }
}
