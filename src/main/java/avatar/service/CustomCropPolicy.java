package avatar.service;

import java.util.Properties;

/** Approved tomato rules; runtime activation stays disabled until native mappings are verified. */
public final class CustomCropPolicy {
    public static final String VERSION = "custom-crop-v1";
    public static final int CROP_ID = 0;
    public static final long GROWTH_MS = 14_400_000L;
    public static final int HEALTH = 100;
    public static final int SEED_COST = 1;
    public static final int HARVEST_QUANTITY = 2;
    public final int seedId;
    public final int productId;

    private CustomCropPolicy(int seedId, int productId) {
        this.seedId = seedId;
        this.productId = productId;
    }

    public static CustomCropPolicy runtime() {
        if (Boolean.getBoolean("avatar.customCropV1"))
            throw new IllegalStateException("Custom crop runtime activation disabled: native mappings and isolated deployment are unverified");
        return null;
    }

    static CustomCropPolicy load(Properties properties) {
        if (!"true".equals(properties.getProperty("avatar.customCropV1"))) return null;
        if (!"isolated-staging".equals(properties.getProperty("avatar.customCropV1.environment"))
                || !"custom-crop-v1-approved".equals(properties.getProperty("avatar.customCropV1.authorization"))) {
            throw new IllegalArgumentException("Custom crop requires explicit isolated staging authorization");
        }
        return new CustomCropPolicy(itemId(properties, "seedId", 100), itemId(properties, "productId", 127));
    }

    private static int itemId(Properties properties, String name, int maximum) {
        try {
            int id = Integer.parseInt(properties.getProperty("avatar.customCropV1." + name));
            if (id < 0 || id > maximum) throw new IllegalArgumentException("Item ID outside native byte range");
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Missing or invalid custom crop " + name, e);
        }
    }
}
