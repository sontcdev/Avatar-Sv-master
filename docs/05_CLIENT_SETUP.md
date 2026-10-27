# Current standalone APK

Download https://avatar.memora.io.vn/Avatar-Homelab.apk and install directly. No separate loader or Tailscale is required. Choose **Đổi tài khoản**, enter the dedicated account, then select a city. Use **Chơi tiếp** for an existing account; **Chơi mới** requires unsupported guest registration. All city socket choices in this APK reach the same private game over public WSS. Current credentials: `ssh homelab cat /home/sontc/avatar2d-staging/test-account.txt` (run privately in your terminal). Build/operations: `../android/README.md`.

The following describes the earlier private JAR test and does not apply to the standalone APK.

# Client setup — emulator gameplay verified; physical phone pending

Original: resAndProjectUnityFileGameJar/Avatar258.jar, preserved unchanged.
Private test copy: artifacts/Avatar258-private.jar (not committed or published).

Manifest: MIDlet-1 `Avatar 258,/icon.png,main.GameMidlet`, vendor TeaMobi, version2.5.8, CLDC1.0, MIDP2.0. ZIP CRC checks pass. `javap -c -p main.GameMidlet` confirms embedded Vietnamese server addresses127.0.0.1, international address112.78.1.25, mixed ports14444/19128/18128. Vietnamese group1 entries0/1 use19128: `Xu So Than Tien` / `Thanh Pho Bao Binh`. Other entries retain incompatible ports and must not be selected.

`python3 scripts/prepare-client.py $(ssh homelab tailscale ip -4)` makes a new private copy. Script refuses overwriting original/output and signed artifacts. It parses class constant-pool UTF8 entries, changes only the two game address constants to current Tailscale IPv4 and four server-list URLs to a dead loopback endpoint to prevent public server-list replacement. Bytecode instructions, class version, stack maps and port arrays are unchanged. Result passes ZIP CRC, manifest and javap parsing. This is structural validation, not a real J2ME verifier or emulator run. Other official update/FAQ/SMS/payment features remain; do not use them. Modification/redistribution permission is not established; keep this test artifact private and obtain rights-holder approval before distribution.

## Android steps

1. Install Tailscale on Android, sign into the same authorized tailnet, and enable its VPN connection. Ensure tailnet policy permits this phone to100.112.0.62 TCP19128. Do not configure a public tunnel or router forwarding.
2. Install [J2ME Loader from its official project/release links](https://github.com/nikita36078/J2ME-Loader/releases). J2ME Loader1.8.2 successfully ran this JAR on the Android emulator.
3. Transfer artifacts/Avatar258-private.jar privately to the phone (USB/local transfer). Open/import it in J2ME Loader. Use a fresh app data profile so old RMS server lists cannot replace embedded private addresses. Allow socket networking. Start with240x320 display and virtual keypad; adjust for device usability.
4. Choose Vietnamese, then `Xu So Than Tien` or `Thanh Pho Bao Binh` (the entries confirmed to use19128). Do not choose international/default14444 entries. If the UI labels differ, capture the server-selection screen; do not assume the first entry is correct.
5. An administrator creates a non-admin account using scripts/create-test-account.py. Retrieve its root-only credential file locally; never paste it in reports. Registration in this build is unimplemented.
6. Login, follow character creation, choose a gender and enter a map. Move, send a chat, interact with an NPC/shop. Record exact errors or a screenshot. Logout/login and restart service to check persistence.

A physical Android client is unavailable to this agent. Login, rendering, input, actual selected endpoint, handshake and gameplay on that device remain pending. A protocol harness is supplementary evidence, never a substitute for these steps.

## Unity candidate

AvatarDKunitydecode.zip contains AuxiliaryFiles/GameAssemblies and ExportedProject/Assets with decompiled Assembly-CSharp sources and assets. ProjectSettings/ProjectVersion.txt says Unity5.6.4f1. GameMidlet.cs reports version2.5.8 and original public Teamobi server addresses. It is an exported reconstruction, not evidence of a clean editable original project or working APK. No Android build performed. Old Unity editor, Android SDK/JDK/Gradle compatibility, decompiled-source compilation and endpoint/network changes need separate verification. J2ME remains the smaller path; no substantial Unity rebuild attempted.

## Android emulator evidence (2026-10-27)

Installed official J2ME Loader 1.8.2 and imported private Avatar JAR in read-only Pixel_10_Pro AVD (Android17, arm64, 16KB pages). Loader reports MIDlet Avatar / TeaMobi / 2.5.8 successfully installed. Android requires page-size compatibility mode for Loader native libraries. Actual login, creation, park entry, movement and reconnect passed after server/password repairs.

Observed login blocker: the real MIDlet discarded punctuation from the generated URL-safe test password (20 intended /19 submitted), causing legitimate auth failure. Test-account generation now uses20 random ASCII letters/digits; dedicated-account correction applied successfully. Client also prints submitted login fields to Android logcat. Do not collect raw logcat or reuse a personal password; emulator log buffer cleared after diagnosis.

Latest actual selection: Vietnamese group `Xu So Than Tien`, then its first city entry `Xu So Than Tien` (19128). Login, create character, choose Công viên and use virtual arrows. Emulator screenshots and reconnect results: 06_GAMEPLAY_VERIFICATION.md.
