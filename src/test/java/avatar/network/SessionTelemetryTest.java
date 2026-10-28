package avatar.network;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionTelemetryTest {
    @Test
    public void telemetryRequiresBothExplicitOptInAndIsolatedDeployment() {
        String staging = "/home/sontc/avatar2d-gameplay-stage";
        assertTrue(SessionTelemetry.allowed(true, staging));
        assertFalse(SessionTelemetry.allowed(false, staging));
        assertFalse(SessionTelemetry.allowed(true, "/home/sontc/avatar2d"));
        assertFalse(SessionTelemetry.allowed(true, null));
        assertFalse(SessionTelemetry.allowed(true, staging + "-production"));
    }
}
