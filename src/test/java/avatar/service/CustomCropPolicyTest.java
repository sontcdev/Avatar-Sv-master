package avatar.service;

import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class CustomCropPolicyTest {
    static Properties testConfig() {
        Properties p = new Properties();
        p.setProperty("avatar.customCropV1", "true");
        p.setProperty("avatar.customCropV1.environment", "isolated-staging");
        p.setProperty("avatar.customCropV1.authorization", "custom-crop-v1-approved");
        p.setProperty("avatar.customCropV1.seedId", "2");
        p.setProperty("avatar.customCropV1.productId", "3");
        return p;
    }
    @Test public void defaultIsDisabled() {
        assertNull(CustomCropPolicy.load(new Properties()));
    }

    @Test public void enabledRequiresStagingAuthorizationAndMappings() {
        Properties p = new Properties();
        p.setProperty("avatar.customCropV1", "true");
        assertThrows(IllegalArgumentException.class, () -> CustomCropPolicy.load(p));
        p.setProperty("avatar.customCropV1.environment", "isolated-staging");
        p.setProperty("avatar.customCropV1.authorization", "custom-crop-v1-approved");
        assertThrows(IllegalArgumentException.class, () -> CustomCropPolicy.load(p));
        p.setProperty("avatar.customCropV1.seedId", "-1");
        p.setProperty("avatar.customCropV1.productId", "0");
        assertThrows(IllegalArgumentException.class, () -> CustomCropPolicy.load(p));
        p.setProperty("avatar.customCropV1.seedId", "101");
        assertThrows(IllegalArgumentException.class, () -> CustomCropPolicy.load(p));
        p.setProperty("avatar.customCropV1.seedId", "2");
        p.setProperty("avatar.customCropV1.productId", "128");
        assertThrows(IllegalArgumentException.class, () -> CustomCropPolicy.load(p));
        p.setProperty("avatar.customCropV1.productId", "3");
        CustomCropPolicy policy = CustomCropPolicy.load(p);
        assertEquals(2, policy.seedId);
        assertEquals(3, policy.productId);
        assertEquals(14400000L, CustomCropPolicy.GROWTH_MS);
    }
    @Test public void runtimeCannotEnableUnverifiedMappings() {
        String old = System.getProperty("avatar.customCropV1");
        try {
            System.clearProperty("avatar.customCropV1"); assertNull(CustomCropPolicy.runtime());
            System.setProperty("avatar.customCropV1", "true");
            assertThrows(IllegalStateException.class, CustomCropPolicy::runtime);
        } finally {
            if (old == null) System.clearProperty("avatar.customCropV1"); else System.setProperty("avatar.customCropV1", old);
        }
    }
}
