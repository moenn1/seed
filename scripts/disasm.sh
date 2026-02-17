#!/usr/bin/env bash
set -euo pipefail
if [ $# -lt 1 ]; then
  echo "Usage: $0 <binary>"
  exit 1
fi
BIN="$1"

# Try LLVM objdump first; if it fails (e.g., due to unexpected default flags), fall back to otool
status=1
if command -v /opt/homebrew/opt/llvm/bin/llvm-objdump >/dev/null 2>&1; then
  /opt/homebrew/opt/llvm/bin/llvm-objdump -d -arch arm64 "$BIN" || status=$?
  if [ "$status" -eq 0 ]; then exit 0; fi
  echo "llvm-objdump (Homebrew) failed (status=$status). Falling back..." >&2
fi

if command -v llvm-objdump >/dev/null 2>&1; then
  status=1
  llvm-objdump -d -arch arm64 "$BIN" || status=$?
  if [ "$status" -eq 0 ]; then exit 0; fi
  echo "llvm-objdump in PATH failed (status=$status). Falling back..." >&2
fi

# macOS fallback: otool (no GNU-style flags used)
if command -v otool >/dev/null 2>&1; then
  echo "(Fallback to otool) Disassembling text sections"
  otool -tvV "$BIN"
  exit 0
fi

echo "No working disassembler found. Install LLVM (brew install llvm) to get llvm-objdump, or use otool."
exit 1