package avatar.service;

import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import avatar.db.DbManager;

/** Approved tomato rules; runtime permits only separately provisioned disposable test deployments. */
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
        if (!Boolean.getBoolean("avatar.customCropV1")) return null;
        try {
            String name = System.getProperty("avatar.customCropV1.identityFile");
            if (name == null) throw new IllegalArgumentException();
            Path file = Path.of(name);
            if (!file.isAbsolute() || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException();
            java.util.Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
            if (!permissions.contains(PosixFilePermission.OWNER_READ) || permissions.stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_")))
                throw new IllegalArgumentException();
            Properties identity = new Properties();
            try (java.io.Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { identity.load(reader); }
            String token = identity.getProperty("identity", "");
            if (!"synthetic-test".equals(identity.getProperty("mode")) || !token.matches("[0-9a-f]{64}")) throw new IllegalArgumentException();
            int seed = itemId(identity, "seedId", 100);
            int product = itemId(identity, "productId", 127);
            if (seed != CROP_ID) throw new IllegalArgumentException();
            try (Connection connection = DbManager.getInstance().getConnection()) {
                if (!"avatar2d_customcrop_test".equals(connection.getCatalog())) throw new IllegalArgumentException();
                try (java.sql.Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT identity_token FROM custom_crop_staging_identity")) {
                    if (!result.next()) throw new IllegalArgumentException();
                    String stored = result.getString(1);
                    if (stored == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.US_ASCII), stored.getBytes(StandardCharsets.US_ASCII)) || result.next())
                        throw new IllegalArgumentException();
                }
            }
            return new CustomCropPolicy(seed, product);
        } catch (Exception failure) {
            throw new IllegalStateException("Custom crop staging identity or mapping validation failed");
        }
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
            String value = properties.getProperty("avatar.customCropV1." + name, properties.getProperty(name));
            int id = Integer.parseInt(value);
            if (id < 0 || id > maximum) throw new IllegalArgumentException("Item ID outside native byte range");
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Missing or invalid custom crop " + name, e);
        }
    }
}
