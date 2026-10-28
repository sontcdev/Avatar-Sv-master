package avatar.network;

/** Explicit opt-in, restricted to the isolated load-test deployment. Never logs payloads. */
final class SessionTelemetry {
    private static final boolean ENABLED = allowed(Boolean.getBoolean("avatar.staging.telemetry"),
            System.getProperty("user.dir"));

    static boolean allowed(boolean requested, String directory) {
        return requested && "/home/sontc/avatar2d-gameplay-stage".equals(directory);
    }

    static boolean enabled() {
        return ENABLED;
    }

    static void dispatch(int session, int user, int command, long started, boolean completed) {
        if (ENABLED) System.out.println("STAGING_DISPATCH epoch_ms=" + System.currentTimeMillis()
                + " session=" + session + " user=" + user + " cmd=" + command
                + " elapsed_ns=" + (System.nanoTime() - started) + " completed=" + completed);
    }

    static void failure(int session, int user, String phase, Exception failure) {
        if (ENABLED) System.out.println("STAGING_DISCONNECT epoch_ms=" + System.currentTimeMillis()
                + " session=" + session + " user=" + user + " phase=" + phase
                + " exception=" + failure.getClass().getSimpleName());
    }
}
