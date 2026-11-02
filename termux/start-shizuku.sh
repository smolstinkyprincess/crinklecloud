#!/data/data/com.termux/files/usr/bin/bash
# Starts the Shizuku server over local ADB. Installed to ~/.termux/start-shizuku.sh by
# RazrGuard install-services.sh. Exits 0 only if Shizuku actually answers afterwards.

export RISH_APPLICATION_ID="com.termux"
SHIZUKU_PKG="moe.shizuku.privileged.api"

# Asking Shizuku directly is the only reliable check: Android hides other apps'
# processes from Termux, so pgrep never sees Shizuku even when it's running.
shizuku_up() {
    timeout 10 rish -c true >/dev/null 2>&1
}

if shizuku_up; then
    echo "[*] Shizuku is already running."
    exit 0
fi

# RazrGuard's app keeps adbd on :5555 (AdbTcpEnabler), so try that before scanning for
# the Wireless Debugging port, which changes on every reboot.
echo "[*] Looking for local ADB..."
TARGET=""
if adb connect 127.0.0.1:5555 2>/dev/null | grep -q "connected to"; then
    TARGET="127.0.0.1:5555"
else
    PORT=$(nmap -p 30000-45000 127.0.0.1 --open 2>/dev/null | awk -F'/' '/tcp open/ {print $1; exit}')
    if [ -n "$PORT" ] && adb connect "127.0.0.1:$PORT" 2>/dev/null | grep -q "connected to"; then
        TARGET="127.0.0.1:$PORT"
    fi
fi

if [ -z "$TARGET" ]; then
    echo "[!] No local ADB found. Is Wireless Debugging enabled?"
    exit 1
fi
echo "[*] Connected to ADB on $TARGET"
sleep 1

# Shizuku 13+ starts from libshizuku.so inside its installed APK; older versions use
# start.sh in its external data dir. (Its /data/user_de dir isn't readable by the adb
# shell, which is why the old path failed with "Permission denied".)
echo "[*] Starting Shizuku server as ADB shell..."
adb -s "$TARGET" shell '
    dir=$(pm path '"$SHIZUKU_PKG"' | sed -n "s/^package://; s/base\.apk$//p" | head -n1)
    lib="${dir}lib/arm64/libshizuku.so"
    if [ -n "$dir" ] && [ -f "$lib" ]; then
        "$lib"
    else
        sh /storage/emulated/0/Android/data/'"$SHIZUKU_PKG"'/start.sh
    fi
'

adb disconnect "$TARGET" >/dev/null 2>&1

# Give the server a moment to come up, then report what really happened
for _ in 1 2 3 4 5; do
    sleep 2
    if shizuku_up; then
        echo "[+] Shizuku started successfully!"
        exit 0
    fi
done
echo "[!] Shizuku did not respond after starting. Open the Shizuku app to check."
exit 1
