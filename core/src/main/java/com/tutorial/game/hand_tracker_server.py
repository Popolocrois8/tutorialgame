import cv2
import mediapipe as mp
import numpy as np
import json
import socket
import threading
import time

class HandTrackerServer:
    def __init__(self, host='localhost', port=12345):
        self.host = host
        self.port = port
        self.running = True

        # MediaPipe setup
        self.mp_hands = mp.solutions.hands
        self.hands = self.mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            min_detection_confidence=0.7,
            min_tracking_confidence=0.7
        )
        self.mp_drawing = mp.solutions.drawing_utils

        # Socket setup
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(1)

        print(f"Hand tracking server started on {self.host}:{self.port}")

    def handle_client(self, client_socket):
        cap = cv2.VideoCapture(0)

        try:
            while self.running:
                ret, frame = cap.read()
                if not ret:
                    break

                # Flip and process frame
                frame = cv2.flip(frame, 1)
                rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

                # MediaPipe processing
                result = self.hands.process(rgb_frame)

                hand_data = {
                    "landmarks": [],
                    "gesture": "none",
                    "index_tip": {"x": 0, "y": 0},
                    "pinch": False
                }

                if result.multi_hand_landmarks:
                    landmarks = result.multi_hand_landmarks[0].landmark

                    # Convert landmarks to pixel coordinates
                    h, w, _ = frame.shape
                    landmarks_px = []
                    for landmark in landmarks:
                        landmarks_px.append({
                            "x": landmark.x * w,
                            "y": landmark.y * h
                        })

                    hand_data["landmarks"] = landmarks_px

                    # Get index fingertip and thumb tip
                    index_tip = landmarks[8]
                    thumb_tip = landmarks[4]

                    hand_data["index_tip"] = {
                        "x": index_tip.x * w,
                        "y": index_tip.y * h
                    }

                    # Check for pinch gesture
                    distance = np.sqrt(
                        (index_tip.x - thumb_tip.x)**2 +
                        (index_tip.y - thumb_tip.y)**2
                    )
                    hand_data["pinch"] = distance < 0.05  # Pinch threshold

                    # Simple gesture recognition
                    if hand_data["pinch"]:
                        hand_data["gesture"] = "pinch"
                    else:
                        hand_data["gesture"] = "open"

                # Send data to Java client
                data_str = json.dumps(hand_data)
                try:
                    client_socket.send((data_str + '\n').encode())
                except:
                    break

                # Small delay to prevent overwhelming the connection
                time.sleep(0.033)  # ~30 FPS

        except Exception as e:
            print(f"Error in client handler: {e}")
        finally:
            cap.release()
            client_socket.close()

    def start(self):
        print("Waiting for Java client to connect...")
        client_socket, addr = self.server_socket.accept()
        print(f"Java client connected from {addr}")

        client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
        client_thread.start()
        client_thread.join()

    def stop(self):
        self.running = False
        self.server_socket.close()
        self.hands.close()

if __name__ == "__main__":
    server = HandTrackerServer()
    try:
        server.start()
    except KeyboardInterrupt:
        server.stop()
