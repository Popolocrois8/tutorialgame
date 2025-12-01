import sys
print("Python is working!", flush=True)
print("Python version:", sys.version, flush=True)

# Keep running until told to stop
import time
while True:
    line = sys.stdin.readline()
    if line and "EXIT" in line:
        break
    time.sleep(1)
    print("Still alive...", flush=True)
