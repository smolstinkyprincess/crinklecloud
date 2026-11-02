#!/data/data/com.termux/files/usr/bin/bash
# RazrGuard monitor: watches runit services and HTTP endpoints, pushes ntfy alerts on
# state changes, and pings healthchecks.io as a dead-man's switch. Runs as the
# razrguard-monitor runit service; installed to ~/.termux/razrguard-monitor.sh.
#
# All state is kept in memory; nothing is written to disk except log lines on changes.

CONF="$HOME/.config/razrguard/monitor.conf"
SVDIR="${SVDIR:-$PREFIX/var/service}"

# Defaults; override in $CONF
NTFY_SERVER="https://ntfy.sh"
NTFY_TOPIC=""
HC_URL=""
INTERVAL=60          # seconds between checks
HEARTBEAT_EVERY=5    # checks between healthchecks.io pings
FAIL_THRESHOLD=2     # consecutive failed checks before alerting
RESTART_LIMIT=3      # restarts within RESTART_WINDOW counts as crash-looping
RESTART_WINDOW=900
HTTP_CHECKS=()       # "name url" pairs; any response below 500 counts as up
TS_ACTIONS=0         # 0 = dry run (alert what it would do), 1 = really act
TS_PEER=""           # optional always-on tailnet IP to ping, proves traffic flows
TS_STEP_MINUTES=(2 5 10 30)   # minutes down before each recovery step
TS_REBOOT_COOLDOWN=21600      # at most one reboot per 6 h
WAN_URL="http://connectivitycheck.gstatic.com/generate_204"

# shellcheck source=/dev/null
[ -f "$CONF" ] && . "$CONF"

if [ -z "$NTFY_TOPIC" ]; then
    echo "NTFY_TOPIC is not set in $CONF"
    sleep 300   # don't spin; runit restarts us after this
    exit 1
fi

declare -A fails=() alerted=() lastpid=() restarts=()
pending=()
SEP=$'\x1f'

log() { echo "$*"; }

notify() { # priority tags title message
    # Bounded so a long offline spell can't grow memory forever
    ((${#pending[@]} >= 20)) && pending=("${pending[@]:1}")
    pending+=("$1$SEP$2$SEP$3$SEP$4")
}

# Sends queued notifications; anything that fails (e.g. offline) is retried next tick
flush() {
    local keep=() item p t title msg
    for item in "${pending[@]}"; do
        IFS="$SEP" read -r p t title msg <<<"$item"
        if ! curl -fsS -m 10 -o /dev/null -H "Priority: $p" -H "Tags: $t" \
            -H "Title: $title" -d "$msg" "$NTFY_SERVER/$NTFY_TOPIC"; then
            keep+=("$item")
        fi
    done
    pending=("${keep[@]}")
}

# Alerts once when a check has failed FAIL_THRESHOLD times in a row, and once on recovery
check() { # key ok message
    local key=$1 ok=$2 msg=$3
    if ((ok)); then
        fails[$key]=0
        if [ -n "${alerted[$key]}" ]; then
            unset "alerted[$key]"
            log "RECOVERED $key"
            notify default white_check_mark "Recovered: $key" "$key is healthy again"
        fi
    else
        fails[$key]=$((${fails[$key]:-0} + 1))
        if ((fails[$key] >= FAIL_THRESHOLD)) && [ -z "${alerted[$key]}" ]; then
            alerted[$key]=1
            log "PROBLEM $key: $msg"
            notify high warning "Problem: $key" "$msg"
        fi
    fi
}

check_services() {
    local d name stat pid now=$EPOCHSECONDS recent t
    for d in "$SVDIR"/*/; do
        [ -d "$d" ] || continue                       # glob matched nothing
        name=$(basename "$d")
        [ -e "$d/down" ] && continue                  # disabled
        stat=$(cat "$d/supervise/stat" 2>/dev/null)
        [ "$stat" = "down" ] && continue              # stopped on purpose with `sv down`
        pid=$(cat "$d/supervise/pid" 2>/dev/null)

        # Count restarts: a new pid on an already-seen service
        if [ -n "$pid" ] && [ -n "${lastpid[$name]}" ] && [ "$pid" != "${lastpid[$name]}" ]; then
            restarts[$name]="${restarts[$name]} $now"
        fi
        [ -n "$pid" ] && lastpid[$name]=$pid
        recent=""
        for t in ${restarts[$name]}; do
            ((now - t < RESTART_WINDOW)) && recent="$recent $t"
        done
        restarts[$name]=$recent

        if [[ "$stat" != run* ]]; then
            check "$name" 0 "$name is not running (runit: ${stat:-unknown})"
        else
            local n
            n=$(wc -w <<<"$recent")
            if ((n >= RESTART_LIMIT)); then
                check "$name" 0 "$name restarted $n times in $((RESTART_WINDOW / 60)) min (crash loop?)"
            else
                check "$name" 1 ""
            fi
        fi
    done
}

check_http() {
    local entry name url code
    for entry in "${HTTP_CHECKS[@]}"; do
        read -r name url <<<"$entry"
        # -k: local services use self-signed certs (e.g. Caddy on :8443)
        code=$(curl -sk -o /dev/null -m 10 -w '%{http_code}' "$url")
        if [ "$code" != "000" ] && ((code < 500)); then
            check "http:$name" 1 ""
        else
            check "http:$name" 0 "$name not responding at $url (HTTP $code)"
        fi
    done
}

# --- Tailscale recovery ---------------------------------------------------------
# Tailscale is up when a tun interface holds a 100.x address (and TS_PEER answers, if
# set). Only acts while the internet itself works: if Wi-Fi/WAN is down, restarting
# Tailscale can't help, so it toggles Wi-Fi a couple of times and otherwise waits.

STATE_DIR="$HOME/.local/state/razrguard"
ts_down_since=0 ts_step=0 wan_down_since=0 wan_step=0 ip_warned=0

privileged() {
    RISH_APPLICATION_ID=com.termux timeout 60 rish -c "$1" </dev/null >/dev/null 2>&1
}

recovery_action() { # what command
    if ((TS_ACTIONS)); then
        log "RECOVERY $1"
        notify high hammer_and_wrench "Network recovery" "$1"
        flush
        privileged "$2"
    else
        log "RECOVERY (dry run) would: $1"
        notify default eyes "Network recovery (dry run)" "Would: $1"
    fi
}

TS_CONNECT="am broadcast -a com.tailscale.ipn.CONNECT_VPN -n com.tailscale.ipn/.IPNReceiver"
WIFI_TOGGLE="svc wifi disable; sleep 5; svc wifi enable"

tailscale_step() { # step number, minutes down
    case $1 in
        0) recovery_action "Tailscale down $2 min: asking it to reconnect" "$TS_CONNECT" ;;
        1) recovery_action "Tailscale down $2 min: restarting the Tailscale app" \
            "am force-stop com.tailscale.ipn; sleep 2; am start -n com.tailscale.ipn/.MainActivity; sleep 5; $TS_CONNECT; input keyevent 223" ;;
        2) recovery_action "Tailscale down $2 min: toggling Wi-Fi" "$WIFI_TOGGLE" ;;
        3)
            local last=0
            last=$(cat "$STATE_DIR/last-reboot" 2>/dev/null || echo 0)
            if ((EPOCHSECONDS - last < TS_REBOOT_COOLDOWN)); then
                log "RECOVERY reboot skipped (last one $(((EPOCHSECONDS - last) / 60)) min ago)"
                notify high warning "Network recovery" "Tailscale still down after $2 min; reboot skipped (already rebooted within 6 h)"
                return
            fi
            ((TS_ACTIONS)) && { mkdir -p "$STATE_DIR"; echo "$EPOCHSECONDS" >"$STATE_DIR/last-reboot"; }
            recovery_action "Tailscale down $2 min: rebooting the phone" "svc power reboot"
            ;;
    esac
}

# Prints "N: IFACE inet ADDR/..." lines like `ip -4 -o addr show`. Android 10+ blocks
# netlink for apps, so `ip` usually fails in Termux; ifconfig (ioctl-based) still works
# without privileges. rish (shell uid) is the last resort since it spawns a JVM.
# rish output is read with 2>&1: this rish build splits lines across stdout and stderr.
read_addrs() {
    local out
    out=$(ip -4 -o addr show 2>/dev/null)
    grep -q ' inet ' <<<"$out" && { echo "$out"; return; }
    out=$(ifconfig 2>/dev/null | awk '/^[^ \t]/ {sub(":", "", $1); i=$1} $1 == "inet" {n++; print n": "i"    inet "$2"/32"}')
    grep -q ' inet ' <<<"$out" && { echo "$out"; return; }
    RISH_APPLICATION_ID=com.termux timeout 30 rish -c "ip -4 -o addr show" </dev/null 2>&1
}

check_tailscale() {
    local addrs now=$EPOCHSECONDS mins
    addrs=$(read_addrs)
    if ! grep -q ' inet ' <<<"$addrs"; then
        # Can't read interfaces at all: never act on a blind guess. Retried every tick
        # (at boot Shizuku/rish may simply not be up yet).
        ((ip_warned)) || { log "cannot read network interfaces; skipping Tailscale checks until readable"; ip_warned=1; }
        return
    fi
    ((ip_warned)) && { log "network interfaces readable again; Tailscale checks resumed"; ip_warned=0; }

    if grep -Eq '^[0-9]+: tun[0-9]+ +inet 100\.' <<<"$addrs" &&
        { [ -z "$TS_PEER" ] || ping -c2 -W3 "$TS_PEER" >/dev/null 2>&1; }; then
        check tailscale 1 ""
        ts_down_since=0 ts_step=0 wan_down_since=0 wan_step=0
        return
    fi
    check tailscale 0 "Tailscale is down"

    if [ "$(curl -s -o /dev/null -m 8 -w '%{http_code}' "$WAN_URL")" != 204 ]; then
        # No internet: pause the Tailscale ladder and just nudge Wi-Fi twice
        ts_down_since=0 ts_step=0
        ((wan_down_since)) || wan_down_since=$now
        mins=$(((now - wan_down_since) / 60))
        if { ((wan_step == 0)) && ((mins >= 5)); } || { ((wan_step == 1)) && ((mins >= 20)); }; then
            wan_step=$((wan_step + 1))
            recovery_action "No internet for $mins min: toggling Wi-Fi" "$WIFI_TOGGLE"
        fi
        return
    fi
    wan_down_since=0 wan_step=0

    ((ts_down_since)) || ts_down_since=$now
    mins=$(((now - ts_down_since) / 60))
    if ((ts_step < ${#TS_STEP_MINUTES[@]})) && ((mins >= TS_STEP_MINUTES[ts_step])); then
        tailscale_step "$ts_step" "$mins"
        ts_step=$((ts_step + 1))
    fi
}

heartbeat() {
    [ -n "$HC_URL" ] || return
    if ((${#alerted[@]} == 0)); then
        curl -fsS -m 10 --retry 2 -o /dev/null "$HC_URL"
    else
        # /fail makes healthchecks.io alert right away instead of waiting for the grace period
        curl -fsS -m 10 --retry 2 -o /dev/null --data-raw "Failing: ${!alerted[*]}" "$HC_URL/fail"
    fi
}

# Android 10+ hides /proc/uptime from apps; the shell uid (rish) can still read it
up=$(cut -d' ' -f1 /proc/uptime 2>/dev/null) ||
    up=$(RISH_APPLICATION_ID=com.termux timeout 30 rish -c "cut -d' ' -f1 /proc/uptime" </dev/null 2>&1)
up=${up%.*}
if [[ "$up" =~ ^[0-9]+$ ]]; then
    log "monitor started (phone uptime ${up}s)"
    notify low robot "RazrGuard monitor started" "Phone uptime: $((up / 60)) min"
else
    log "monitor started (phone uptime unknown)"
    notify low robot "RazrGuard monitor started" "Phone uptime unknown"
fi

tick=0
while :; do
    check_services
    check_http
    check_tailscale
    ((tick % HEARTBEAT_EVERY == 0)) && heartbeat
    ((${#pending[@]})) && flush
    tick=$((tick + 1))
    sleep "$INTERVAL"
done
