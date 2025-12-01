import cv2
import sys
print(f"OpenCV version: {cv2.__version__}", flush=True)

cap = cv2.VideoCapture(0)
if cap.isOpened():
    print("Camera opened successfully", flush=True)
    cap.release()
else:
    print("Failed to open camera", flush=True)

# Keep alive
import time
while True:
    line = sys.stdin.readline()
    if line and "EXIT" in line:
        break
    time.sleep(1)
