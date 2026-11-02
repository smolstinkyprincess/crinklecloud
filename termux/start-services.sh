#!/data/data/com.termux/files/usr/bin/bash

# ==============================================================================
#  🌸 Razr Mini-OS Boot & Service Sequence 🧸
# ==============================================================================
# Installed to ~/.termux/boot/start-services.sh by RazrGuard install-services.sh.
# Termux:Boot runs it at boot; GuardService re-runs it when the stack looks dead, so
# every step must be idempotent. The services themselves live in runit
# ($PREFIX/var/service): runsvdir starts every enabled one and restarts any that die.

# Palette & Helpers
C_RESET="\033[0m"
C_PINK="\033[38;2;242;167;191m"
C_LAVENDER="\033[38;2;199;164;245m"
C_MINT="\033[38;2;134;232;186m"

log_step() {
    echo -e "${C_LAVENDER}$1${C_RESET} ${C_PINK}$2${C_RESET}"
}

echo -e "${C_PINK}╭──────────────────────────────────────────────╮${C_RESET}"
echo -e "${C_PINK}│  🌸 Razr Mini-OS Boot & Service Sequence 🧸  │${C_RESET}"
echo -e "${C_PINK}╰──────────────────────────────────────────────╯${C_RESET}"

# --- 1. Shizuku Server via Local ADB Loopback ---
log_step "✨ [1/6]" "Checking Shizuku privileged daemon..."
export RISH_APPLICATION_ID="com.termux"
# start-shizuku.sh checks with rish first and exits early if Shizuku already answers
shizuku_out=$(bash "$HOME/.termux/start-shizuku.sh" 2>&1)
shizuku_rc=$?
sed 's/^/       /' <<<"$shizuku_out"
if [ "$shizuku_rc" -ne 0 ]; then
    echo -e "       ${C_PINK}Shizuku is not responding; screen tucking below will fail.${C_RESET}"
fi

# --- 2. Wake-lock ---
log_step "💤 [2/6]" "Holding cozy wake-lock (no sleepy CPU)..."
termux-wake-lock

# --- 3. Tailscale Daemon ---
log_step "🌐 [3/6]" "Waking up Tailscale network daemon..."
am start-foreground-service com.tailscale.ipn/.IPNService 2>/dev/null || \
am start -n com.tailscale.ipn/.MainActivity >/dev/null 2>&1

# --- 4. Screen & Digitizer Tucking ---
log_step "✨ [4/6]" "Calming screen digitizer & tucking display into sleep..."
# Disabled: CrinkleCloud uses the display now
#rish -c "wm size 1x1" 2>/dev/null
#rish -c "input keyevent 223" 2>/dev/null

# --- 5. Storage Directories ---
log_step "📂 [5/6]" "Checking anime storage folder..."
mkdir -p "$HOME/storage/shared/Download/Anime"

# --- 6. runit Supervisor ---
# No 15s route-settle wait needed anymore: a service that fails because the network
# isn't up yet just gets restarted by runit.
log_step "🧸 [6/6]" "Waking the runit supervisor (it starts & babysits every service)..."
export SVDIR="$PREFIX/var/service"
export LOGDIR="$PREFIX/var/log"
if ! pgrep -f '^[^ ]*/runsvdir '  > /dev/null; then
    service-daemon start
fi

echo -e "${C_PINK}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${C_RESET}"
echo -e "${C_MINT}✨ Supervisor is up! Check services with: sv status \$SVDIR/* 💖${C_RESET}"
echo -e "${C_PINK}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${C_RESET}"
