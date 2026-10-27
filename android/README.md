# Avatar Homelab Android

Standalone signed APK: https://avatar.memora.io.vn/Avatar-Homelab.apk

Build 103 hides the virtual keypad by default. Tap **Phím** at the top to show it, or **Ẩn phím** to hide it; the choice persists across app restarts. Hidden keys do not intercept touches.

The current APK starts in landscape and fills the available display, retaining the camera cutout safe area. Updating preserves the saved game account and resets only the emulator display/key layout once. The legacy MIDlet cannot reliably resize during a session, so this build stays in landscape. Hold the arrow keys to move; **F** confirms, **L/R** activate the left/right game commands. A menu tap may focus an item first; then press **Chọn/F**.

Install directly on Android (allow installing this downloaded APK). No J2ME Loader import or Tailscale app is required. Choose **Đổi tài khoản**, enter the dedicated non-admin account, choose **Chơi tiếp** and a city. All socket selections in this standalone build go to the same fixed Avatar service through `wss://avatar.memora.io.vn/game`; city names are inherited from the bundled client.

## Build

Requires Java 17, Android SDK 34, NDK 28.2.13676358 and Gradle 8.14. J2ME Loader is pinned at `7e978c5ddee51a13d78ac59fa059d773d9de6b48` (1.8.2), Apache 2.0; license is included here. The upstream engine includes separately licensed native dependencies; retain its notices when redistributing.

1. Prepare `artifacts/Avatar258-private.jar` with `scripts/prepare-client.py`.
2. Keep your signing keystore and `storeFile`, `storePassword`, `keyPassword`, `keyAlias` in `.secrets/avatar-signing.properties` (mode 600). Preserve this key for upgrades; do not publish it.
3. Run `android/build-apk.sh`. Optionally set `AVATAR_GRADLE` to an installed Gradle 8.14 executable. On macOS set `JAVA_HOME` to Java 17. Output: `artifacts/Avatar-Homelab.apk`.

The build applies the checked-in patch and WebSocket transport, instruments the original bytecode with upstream AndroidProducer, and packages classes/resources in the upstream `midlet` flavor. Resource shrinking is disabled because the legacy game uses reflective/resource lookups. Legacy stdout is suppressed because the original login code prints credentials.

## Public deployment

`avatar.memora.io.vn` is a proxied CNAME to existing tunnel `c8003976-0c39-415f-9d31-eed5d325f024`. Only its new hostname route points to `http://127.0.0.1:18728`; existing API/upload routes are retained.

User service `avatar2d-public` runs `scripts/avatar-ws-bridge.py` under `/home/sontc/avatar2d-public`, Python 3.12 + websockets 15.0.1. `/game` accepts binary WebSocket streams and connects only to `100.112.0.62:19128`. Limits: 6 concurrent sessions total, 2 per public IP, 64 KiB per incoming message. Shared mobile/home NATs count as one IP. `/healthz` checks upstream reachability; `/` and `/Avatar-Homelab.apk` download the APK. Listener is loopback only; no router port forwarding.

Publish APK by copying to `/home/sontc/avatar2d-public/Avatar-Homelab.apk.new` then renaming atomically. Restart only this user service when changing bridge code. Rollback public access by removing only the Avatar DNS/ingress entry and stopping `avatar2d-public`; keep the private game service running.

Verification commands: `PYTHONPATH=artifacts/ws-test-lib python3 scripts/test-avatar-ws-bridge.py` locally, and the bridge venv Python running `scripts/public-protocol-smoke.py` on homelab. The latter checks the complete actual XOR-key handshake over public WSS, rather than only the first frame.

## Known limits

Android 17 emulator displays a 16 KiB page-size compatibility warning for inherited native libraries, then runs in compatibility mode. Physical phones, all Android versions and sustained multi-player load have not been tested. This build is for sideloading; it is not a Play Store release. Original JAR resources/game behavior are retained.
