import cv2
import os
from pathlib import Path

DATASET_ORIGINAL = "dataset"
DATASET_PROCESADO = "dataset_procesado"
TAMAÑO = 224

ruta_cascade = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
detector = cv2.CascadeClassifier(ruta_cascade)


def procesar_imagen(ruta_origen, ruta_destino):
    """Detecta, recorta y redimensiona la cara de una imagen."""
    img = cv2.imread(str(ruta_origen))
    if img is None:
        print(f"No se pudo leer: {ruta_origen}")
        return False

    gris = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    caras = detector.detectMultiScale(
        gris,
        scaleFactor=1.1,  
        minNeighbors=5, 
        minSize=(60, 60)   
    )

    if len(caras) == 0:
        print(f"Sin cara detectada: {ruta_origen}")
        return False

    x, y, w, h = max(caras, key=lambda c: c[2] * c[3])

    cara = img[y:y+h, x:x+w]

    cara = cv2.resize(cara, (TAMAÑO, TAMAÑO))

    cv2.imwrite(str(ruta_destino), cara)
    return True


def main():
    origen = Path(DATASET_ORIGINAL)
    destino = Path(DATASET_PROCESADO)

    total = 0
    exitosas = 0

    for carpeta_persona in origen.iterdir():
        if not carpeta_persona.is_dir():
            continue

        carpeta_destino = destino / carpeta_persona.name
        carpeta_destino.mkdir(parents=True, exist_ok=True)

        print(f"\nProcesando: {carpeta_persona.name}")

        for img_path in carpeta_persona.iterdir():
            if img_path.suffix.lower() not in [".jpg", ".jpeg", ".png"]:
                continue
            total += 1
            destino_img = carpeta_destino / img_path.name
            if procesar_imagen(img_path, destino_img):
                exitosas += 1

    print(f"\n✓ Procesadas {exitosas}/{total} imágenes correctamente.")


if __name__ == "__main__":
    main()