import cv2
import numpy as np
import tensorflow as tf
import json
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELO = os.path.join(BASE_DIR, "modelo_transfer.keras")
CLASES_FILE = os.path.join(BASE_DIR, "clases.json")
TAMAÑO = 224
UMBRAL_CONFIANZA = 0.6

modelo = tf.keras.models.load_model(MODELO)

with open(CLASES_FILE, "r") as f:
    clases = json.load(f)

print(f"Modelo cargado. Clases: {clases}")

ruta_cascade = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
detector = cv2.CascadeClassifier(ruta_cascade)

cap = cv2.VideoCapture(0)

if not cap.isOpened():
    print("No se pudo abrir la camara")
    exit()

print("Presiona 'q' para salir")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    gris = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    caras = detector.detectMultiScale(gris, 1.1, 5, minSize=(60, 60))

    for (x, y, w, h) in caras:
        cara = frame[y:y+h, x:x+w]
        cara_resize = cv2.resize(cara, (TAMAÑO, TAMAÑO))

        cara_array = np.expand_dims(cara_resize, axis=0).astype(np.float32)

        predicciones = modelo.predict(cara_array, verbose=0)[0]
        idx = np.argmax(predicciones)
        confianza = predicciones[idx]

        if confianza >= UMBRAL_CONFIANZA:
            etiqueta = f"{clases[idx]} ({confianza*100:.1f}%)"
            color = (0, 255, 0)   # verde
        else:
            etiqueta = f"Desconocido ({confianza*100:.1f}%)"
            color = (0, 0, 255)   # rojo
            
        cv2.rectangle(frame, (x, y), (x+w, y+h), color, 2)
        cv2.putText(frame, etiqueta, (x, y-10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

    cv2.imshow("Reconocimiento Facial - 'q' para salir", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()