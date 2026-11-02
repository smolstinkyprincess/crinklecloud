#!/data/data/com.termux/files/usr/bin/bash

# ==============================================================================
#  🎀 Razr Mini-OS Stack Restart Routine 🌸
# ==============================================================================
# Installed to ~/restart-services.sh by RazrGuard install-services.sh.
#   restart-services.sh               restart every enabled service
#   restart-services.sh jellyfin ...  restart just these

C_RESET="\033[0m"
C_PINK="\033[38;2;242;167;191m"
C_LAVENDER="\033[38;2;199;164;245m"
C_MINT="\033[38;2;134;232;186m"

log_step() {
    echo -e "${C_LAVENDER}$1${C_RESET} ${C_PINK}$2${C_RESET}"
}

echo -e "${C_PINK}╭──────────────────────────────────────────────╮${C_RESET}"
echo -e "${C_PINK}│  🎀 Razr Mini-OS Stack Restart Routine 🌸    │${C_RESET}"
echo -e "${C_PINK}╰──────────────────────────────────────────────╯${C_RESET}"

export SVDIR="$PREFIX/var/service"

# Make sure the supervisor itself is alive first
bash "$HOME/.termux/boot/start-services.sh" > /dev/null

if (($#)); then
    services=("$@")
else
    # Enabled services only (disabled ones have a `down` file)
    services=()
    for d in "$SVDIR"/*/; do
        [[ -e "$d/down" ]] || services+=("$(basename "$d")")
    done
fi

log_step "🧹 [1/2]" "Gently restarting: ${services[*]}"
# -w 30: give Jellyfin & Vaultwarden time to shut down cleanly
sv -w 30 restart "${services[@]}"

log_step "✨ [2/2]" "Current status:"
sv status "${services[@]}"

echo -e "${C_MINT}💖 All done! 🧸🌸${C_RESET}"
