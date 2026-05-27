import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.applications.mobilenet_v2 import preprocess_input
import json

DATASET = "dataset_procesado"
TAMAÑO = 224
BATCH_SIZE = 32
EPOCAS_INICIALES = 30  
EPOCAS_FINETUNING = 40   
MODELO_SALIDA = "modelo_transfer.keras"
CLASES_SALIDA = "clases.json"

train_ds = tf.keras.utils.image_dataset_from_directory(
    DATASET, validation_split=0.2, subset="training", seed=123,
    image_size=(TAMAÑO, TAMAÑO), batch_size=BATCH_SIZE
)
val_ds = tf.keras.utils.image_dataset_from_directory(
    DATASET, validation_split=0.2, subset="validation", seed=123,
    image_size=(TAMAÑO, TAMAÑO), batch_size=BATCH_SIZE
)

clases = train_ds.class_names
num_clases = len(clases)
print(f"Clases detectadas ({num_clases}): {clases}")

with open(CLASES_SALIDA, "w") as f:
    json.dump(clases, f)

AUTOTUNE = tf.data.AUTOTUNE
train_ds = train_ds.cache().shuffle(1000).prefetch(AUTOTUNE)
val_ds = val_ds.cache().prefetch(AUTOTUNE)

data_augmentation = tf.keras.Sequential([
    layers.RandomFlip("horizontal"),
    layers.RandomRotation(0.1),
    layers.RandomZoom(0.1),
])

base_model = MobileNetV2(
    input_shape=(TAMAÑO, TAMAÑO, 3),
    include_top=False, 
    weights="imagenet"  
)

base_model.trainable = False

inputs = layers.Input(shape=(TAMAÑO, TAMAÑO, 3))
x = data_augmentation(inputs)

x = preprocess_input(x)

x = base_model(x, training=False)

x = layers.GlobalAveragePooling2D()(x)
x = layers.Dropout(0.3)(x)
outputs = layers.Dense(num_clases, activation="softmax")(x)

modelo = models.Model(inputs, outputs)
modelo.summary()

modelo.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

print("\n=== FASE 1: Entrenando solo la cabeza ===")
modelo.fit(
    train_ds,
    validation_data=val_ds,
    epochs=EPOCAS_INICIALES
)

base_model.trainable = True

for capa in base_model.layers[:-30]:
    capa.trainable = False

modelo.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=1e-5),
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

callbacks = [
    tf.keras.callbacks.EarlyStopping(
        monitor="val_accuracy",
        patience=15,
        restore_best_weights=True
    ),
    tf.keras.callbacks.ModelCheckpoint(
        MODELO_SALIDA,
        monitor="val_accuracy",
        save_best_only=True
    )
]

print("\n=== FASE 2: Fine-tuning ===")
modelo.fit(
    train_ds,
    validation_data=val_ds,
    epochs=EPOCAS_FINETUNING,
    callbacks=callbacks
)

print(f"\n✓ Modelo guardado en: {MODELO_SALIDA}")
print(f"✓ Clases guardadas en: {CLASES_SALIDA}")