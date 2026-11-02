#!/usr/bin/env bash

set -e

PKG="one.crinklecod.cc"
TARGET_PACKAGES=("$PKG" "com.termux" "com.termux.boot" "com.tailscale.ipn" "moe.shizuku.privileged.api")

echo "============================================="
echo "  Provisioning RazrGuard & Termux Environment"
echo "============================================="

# 1. Verify ADB connection
echo "[*] Checking ADB connection..."
if ! adb get-state 1>/dev/null 2>&1; then
    echo "[!] Error: No authorized ADB device detected. Connect the phone and run 'adb devices'."
    exit 1
fi
echo "[+] Device connected."

# 2. Grant App Permissions
echo "[*] Granting RazrGuard permissions..."
adb shell pm grant "$PKG" com.termux.permission.RUN_COMMAND 2>/dev/null || true
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow 2>/dev/null || true

# 3. Exempt all core packages from Doze and background execution limits
echo "[*] Whitelisting from Doze and setting active standby buckets..."
for p in "${TARGET_PACKAGES[@]}"; do
    if ! adb shell pm path "$p" >/dev/null 2>&1; then
        echo "    -> Skipping $p (not installed)"
        continue
    fi
    echo "    -> Configuring $p"
    adb shell dumpsys deviceidle whitelist +"$p" 1>/dev/null
    adb shell cmd appops set "$p" RUN_ANY_IN_BACKGROUND allow 1>/dev/null
    adb shell cmd appops set "$p" RUN_IN_BACKGROUND allow 1>/dev/null
    adb shell am set-standby-bucket "$p" active 1>/dev/null
done

# 4. Disable Android 12+ Phantom Process Killer & Tune Low Memory Killer
echo "[*] Disabling phantom process killer (prevents killing darkhttpd/child tasks)..."
adb shell device_config set_sync_disabled_for_tests persistent 1>/dev/null 2>&1 || true
adb shell device_config put activity_manager max_phantom_processes 2147483647 1>/dev/null 2>&1 || true
adb shell settings put global settings_enable_monitor_phantom_procs false 1>/dev/null 2>&1 || true

# 5. Disable Wi-Fi Power Save & Protect PIDs with OOM Score -1000
echo "[*] Disabling Wi-Fi power save & setting OOM immunity for background daemons..."
adb shell iw dev wlan0 set power_save off 2>/dev/null || true
for p in "${TARGET_PACKAGES[@]}"; do
    adb shell "for pid in \$(pidof $p 2>/dev/null); do echo -1000 > /proc/\$pid/oom_score_adj 2>/dev/null || true; done" || true
done

# 6. Start GuardService for the first time
echo "[*] Starting RazrGuard foreground service..."
adb shell am start-foreground-service -n "$PKG/.GuardService"

echo "============================================="
echo "  Setup Complete! Watching logs (Ctrl+C to exit):"
echo "============================================="
adb logcat -s RazrGuard RazrGuard/Shizuku
