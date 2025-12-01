import sys

try:
    import mediapipe as mp
    print("MediaPipe imported successfully", flush=True)
except Exception as e:
    print(f"MediaPipe import failed: {e}", flush=True)

# Keep alive
import time
while True:
    line = sys.stdin.readline()
    if line and "EXIT" in line:
        break
    time.sleep(1)
