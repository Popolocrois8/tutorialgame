import cv2
import mediapipe as mp
import socket
import select

# --- CONFIGURATION ---
JAVA_IP = "127.0.0.1"
JAVA_PORT = 5005       # Port to SEND gestures TO Java
PYTHON_PORT = 5006     # Port to LISTEN for commands FROM Java
# ---------------------

# 1. Setup UDP Socket for sending (Gestures)
sock_send = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# 2. Setup UDP Socket for listening (Commands like START/STOP)
sock_listen = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock_listen.bind((JAVA_IP, PYTHON_PORT))
sock_listen.setblocking(False) # Non-blocking so we don't freeze

# Initialize MediaPipe
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(max_num_hands=1, min_detection_confidence=0.7)
mp_draw = mp.solutions.drawing_utils

cap = None
is_active = False

print(f"💤 Hand Engine Waiting... (Listening on Port {PYTHON_PORT})")

def count_fingers(landmarks):
    fingers = []
    # Thumb (Check x-axis for right hand/mirroring)
    if landmarks[4].x < landmarks[3].x:
        fingers.append(1)
    else:
        fingers.append(0)
    # 4 Fingers (Check y-axis)
    for id in [8, 12, 16, 20]:
        if landmarks[id].y < landmarks[id - 2].y:
            fingers.append(1)
        else:
            fingers.append(0)
    return fingers

while True:
    # --- CHECK FOR COMMANDS FROM JAVA ---
    try:
        ready = select.select([sock_listen], [], [], 0.01)
        if ready[0]:
            data, addr = sock_listen.recvfrom(1024)
            command = data.decode().strip()

            if command == "START":
                if not is_active:
                    print("🟢 Java said START. Opening Camera...")
                    cap = cv2.VideoCapture(0)
                    is_active = True

            elif command == "STOP":
                if is_active:
                    print("🔴 Java said STOP. Releasing Camera...")
                    is_active = False
                    if cap:
                        cap.release()
                        cv2.destroyAllWindows()

    except Exception as e:
        print(f"Socket Error: {e}")

    # --- PROCESS CAMERA ONLY IF ACTIVE ---
    if is_active and cap is not None and cap.isOpened():
        success, img = cap.read()
        if success:
            img = cv2.flip(img, 1)
            img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
            results = hands.process(img_rgb)

            current_sign = "NONE"

            if results.multi_hand_landmarks:
                for hand_lms in results.multi_hand_landmarks:
                    mp_draw.draw_landmarks(img, hand_lms, mp_hands.HAND_CONNECTIONS)
                    fingers = count_fingers(hand_lms.landmark)
                    total_fingers = fingers.count(1)

                    if total_fingers == 0: current_sign = "down"
                    elif fingers[1] == 1 and fingers[2] == 1 and total_fingers == 2: current_sign = "up"
                    elif total_fingers == 5: current_sign = "left"
                    elif fingers[0] == 1 and total_fingers == 1: current_sign = "right"

            # Send result to Java
            if current_sign != "NONE":
                sock_send.sendto(current_sign.encode(), (JAVA_IP, JAVA_PORT))
                cv2.putText(img, f"SENT: {current_sign.upper()}", (10, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

            cv2.imshow("Hand Engine (Active)", img)
            cv2.waitKey(1)

    # If not active, just wait a bit to save CPU
    else:
        cv2.waitKey(100)
