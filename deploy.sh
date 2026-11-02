#!/usr/bin/env bash
# Deploys RazrGuard's phone-side files from this repo over SSH, then restarts only the
# runit services whose files actually changed.
#
#   ./deploy.sh            # host "razr" from ~/.ssh/config
#   RAZR_HOST=foo ./deploy.sh
#   ./deploy.sh --dry-run  # show what would change, touch nothing
#
# Ships termux/ (public) and, if present, private/ (local-only repo; see private/README.md).
# Edit files here and deploy; don't edit them on the phone, or the next deploy reverts them.

set -euo pipefail
HOST="${RAZR_HOST:-razr}"
DRY=0
[[ "${1:-}" == "--dry-run" ]] && DRY=1
ROOT="$(cd "$(dirname "$0")" && pwd)"
dirs=(termux)
[[ -d "$ROOT/private" ]] && dirs+=(private)

echo "[*] Shipping ${dirs[*]} to $HOST"
tar -C "$ROOT" --exclude=.git -cf - "${dirs[@]}" |
    ssh "$HOST" 'S=$HOME/.razrguard-deploy; rm -rf "$S"; mkdir -p "$S"; tar -xf - -C "$S"'

# Remote half. DRY is passed as $1.
ssh "$HOST" bash ${RAZR_TRACE:+-x} -s -- "$DRY" <<'REMOTE'
set -euo pipefail
DRY=$1
S="$HOME/.razrguard-deploy"
export SVDIR="$PREFIX/var/service"
ROOTFS="$PREFIX/var/lib/proot-distro/containers/debian/rootfs"
restart=()

# put SRC DEST MODE [SERVICE]: copy if contents differ; queue SERVICE for restart
put() {
    local src=$1 dst=$2 mode=$3 svc=${4:-}
    [[ -f "$src" ]] || return 0
    cmp -s "$src" "$dst" 2>/dev/null && return 0
    echo "    changed: $dst${svc:+  (restart $svc)}"
    ((DRY)) && return 0
    install -m "$mode" "$src" "$dst"
    [[ -n "$svc" ]] && restart+=("$svc")
    return 0
}

echo "[*] Private files"
P="$S/private"
put "$P/termux/update-stats.sh"        "$HOME/update-stats.sh"        700 update-stats
put "$P/termux/trans-helper.py"        "$HOME/trans-helper.py"        600 trans-helper
put "$P/termux/on-torrent-complete.sh" "$HOME/on-torrent-complete.sh" 700
put "$P/termux/.ssh-motd.sh"           "$HOME/.ssh-motd.sh"           700
put "$P/proot/start-jellyfin.sh"       "$ROOTFS/usr/local/bin/start-jellyfin.sh" 755 jellyfin
put "$P/proot/start-suwayomi.sh"       "$ROOTFS/usr/local/bin/start-suwayomi.sh" 755 suwayomi
put "$P/proot/Caddyfile"               "$ROOTFS/etc/caddy/Caddyfile"  644 caddy

echo "[*] RazrGuard services (termux/)"
# The installer only rewrites files whose contents changed, so snapshot to diff afterwards
snap() { md5sum "$SVDIR"/*/run "$SVDIR"/*/cmd "$HOME/.termux/razrguard-monitor.sh" 2>/dev/null || true; }
if ((DRY)); then
    put "$S/termux/razrguard-monitor.sh" "$HOME/.termux/razrguard-monitor.sh" 755
    put "$S/termux/start-services.sh"    "$HOME/.termux/boot/start-services.sh" 755
    put "$S/termux/start-shizuku.sh"     "$HOME/.termux/start-shizuku.sh" 755
    put "$S/termux/restart-services.sh"  "$HOME/restart-services.sh" 755
    echo "    (service definitions not diffed in dry run)"
else
    before=$(snap)
    bash "$S/termux/install-services.sh" | sed 's/^/    /'
    after=$(snap)
    while read -r f; do
        case "$f" in
            */razrguard-monitor.sh) restart+=(razrguard-monitor) ;;
            "$SVDIR"/*) f=${f#"$SVDIR"/}; restart+=("${f%%/*}") ;;
        esac
    done < <(diff <(echo "$before") <(echo "$after") | awk '/^>/ {print $3}')
fi

rm -rf "$S"
if ((DRY)); then echo "[+] Dry run: nothing changed"; exit 0; fi
if ((${#restart[@]})); then
    mapfile -t uniq < <(printf '%s\n' "${restart[@]}" | sort -u)
    echo "[*] Restarting: ${uniq[*]}"
    sv restart "${uniq[@]}"
else
    echo "[+] No service changes; nothing restarted"
fi
REMOTE
