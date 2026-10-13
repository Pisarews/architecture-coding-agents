#!/usr/bin/env bash
# Start once, any time of day. Waits for the night window, runs the queue, pauses when the window
# closes or the daily API limit is hit, and resumes the next night until the plan is complete.
#   harness/run_night.sh plans/pilot.yaml data/pilot 22-7
# The API key comes from the environment or from ~/.config/archagents/env (chmod 600, never in the repo).
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/.." && pwd)
PLAN=${1:?plan yaml}; OUT=${2:?output dir}; WINDOW=${3:-22-7}
if [[ -z "${OPENROUTER_API_KEY:-}" && -f "$HOME/.config/archagents/env" ]]; then
  set -a; source "$HOME/.config/archagents/env"; set +a
fi
: "${OPENROUTER_API_KEY:?set OPENROUTER_API_KEY or create ~/.config/archagents/env}"
START=${WINDOW%-*}; END=${WINDOW#*-}
in_window() {
  local h=$((10#$(date +%H)))
  if (( START < END )); then (( h >= START && h < END )); else (( h >= START || h < END )); fi
}
mkdir -p "$ROOT/$OUT"; cd "$ROOT"
LOG="$ROOT/$OUT/queue.log"
echo "$(date '+%F %T') run_night started (plan=$PLAN window=$WINDOW)" >> "$LOG"
while true; do
  if ! in_window; then sleep 300; continue; fi
  echo "$(date '+%F %T') window open, queue starts" >> "$LOG"
  caffeinate -dimsu "$ROOT/../.venv/bin/python" "$HERE/run_queue.py" --plan "$HERE/$PLAN" --out "$OUT" --window "$WINDOW" >> "$LOG" 2>&1
  rc=$?
  if (( rc == 0 )); then echo "$(date '+%F %T') plan complete" >> "$LOG"; exit 0; fi
  if (( rc == 5 )); then
    echo "$(date '+%F %T') API limit reached; waiting for the next night" >> "$LOG"
    while in_window; do sleep 300; done
  fi
  sleep 60
done
