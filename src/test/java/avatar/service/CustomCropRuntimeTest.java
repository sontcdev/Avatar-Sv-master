package avatar.service;

import avatar.db.DbManager;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomCropRuntimeTest {
    @Test public void runtimeRequiresMatchingIndependentDatabaseIdentity() throws Exception {
        DbManager db = DbManager.getInstance();
        Field f = DbManager.class.getDeclaredField("hikariDataSource"); f.setAccessible(true);
        assertNull(f.get(db));
        HikariDataSource pool = new HikariDataSource();
        pool.setJdbcUrl("jdbc:h2:mem:avatar2d_customcrop_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        f.set(db, pool);
        Path identity = Files.createTempFile("crop-identity-", ".properties");
        Files.setPosixFilePermissions(identity, PosixFilePermissions.fromString("rw-------"));
        String token = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String oldFlag = System.getProperty("avatar.customCropV1"), oldPath = System.getProperty("avatar.customCropV1.identityFile");
        try {
            try (Connection c = db.getConnection()) {
                c.createStatement().execute("CREATE TABLE custom_crop_staging_identity (identity_token VARCHAR(64) NOT NULL)");
                c.createStatement().execute("INSERT INTO custom_crop_staging_identity VALUES ('" + token + "')");
            }
            Files.writeString(identity, "mode=synthetic-test\nidentity=" + token + "\nseedId=0\nproductId=3\n");
            System.setProperty("avatar.customCropV1", "true");
            System.setProperty("avatar.customCropV1.identityFile", identity.toString());
            CustomCropPolicy policy = CustomCropPolicy.runtime();
            assertEquals(0, policy.seedId); assertEquals(3, policy.productId);
            Files.writeString(identity, "mode=synthetic-test\nidentity=" + "f".repeat(64) + "\nseedId=0\nproductId=3\n");
            assertThrows(IllegalStateException.class, CustomCropPolicy::runtime);
            Files.writeString(identity, "mode=synthetic-test\nidentity=" + token + "\nseedId=0\nproductId=128\n");
            assertThrows(IllegalStateException.class, CustomCropPolicy::runtime);
            Files.writeString(identity, "mode=native\nidentity=" + token + "\nseedId=0\nproductId=3\n");
            assertThrows(IllegalStateException.class, CustomCropPolicy::runtime);
            Files.writeString(identity, "mode=synthetic-test\nidentity=" + token + "\nseedId=2\nproductId=3\n");
            assertThrows(IllegalStateException.class, CustomCropPolicy::runtime);
            Files.writeString(identity, "mode=synthetic-test\nidentity=" + token + "\nseedId=0\nproductId=3\n");
            Files.setPosixFilePermissions(identity, PosixFilePermissions.fromString("rw-r--r--"));
            assertThrows(IllegalStateException.class, CustomCropPolicy::runtime);
        } finally {
            if (oldFlag == null) System.clearProperty("avatar.customCropV1"); else System.setProperty("avatar.customCropV1", oldFlag);
            if (oldPath == null) System.clearProperty("avatar.customCropV1.identityFile"); else System.setProperty("avatar.customCropV1.identityFile", oldPath);
            f.set(db, null); pool.close(); Files.delete(identity);
        }
    }
}
