#!/data/data/com.termux/files/usr/bin/bash

# ==============================================================================
#  🌸 Razr Mini-OS PRoot & Storage I/O Tuning 🧸
# ==============================================================================
# Installed to ~/.termux/tune-storage.sh by RazrGuard install-services.sh.
# Enables SQLite WAL mode, trims old log archives, and optimizes I/O overhead.

C_RESET="\033[0m"
C_PINK="\033[38;2;242;167;191m"
C_LAVENDER="\033[38;2;199;164;245m"
C_MINT="\033[38;2;134;232;186m"

# 1. Enable SQLite WAL mode on host databases (e.g. filebrowser)
if command -v sqlite3 >/dev/null 2>&1; then
    find "$HOME/.config" "$HOME" -maxdepth 3 -type f \( -name "*.db" -o -name "*.sqlite3" \) 2>/dev/null | while read -r db; do
        sqlite3 "$db" "PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;" >/dev/null 2>&1 || true
    done
fi

# 2. Enable SQLite WAL mode on proot databases (Vaultwarden, Jellyfin, Suwayomi)
PROOT_DEBIAN="$PREFIX/var/lib/proot-distro/installed-rootfs/debian"
if [ -d "$PROOT_DEBIAN" ] && command -v sqlite3 >/dev/null 2>&1; then
    find "$PROOT_DEBIAN/root" "$PROOT_DEBIAN/var/lib" "$PROOT_DEBIAN/home" -maxdepth 4 -type f \( -name "*.db" -o -name "*.sqlite" -o -name "*.sqlite3" \) 2>/dev/null | while read -r db; do
        sqlite3 "$db" "PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;" >/dev/null 2>&1 || true
    done
fi

# 3. Trim rotated svlogd log files older than 7 days
find "$PREFIX/var/log" -type f \( -name "*.u" -o -name "*.s" \) -mtime +7 -delete 2>/dev/null || true

# 4. Clean ephemeral cache/temp files
rm -rf "$PREFIX/tmp/*" "$HOME/.cache/thumbnails" 2>/dev/null || true
