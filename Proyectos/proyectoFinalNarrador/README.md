# 🎮 Narrador IA

Sistema que analiza clips de videojuegos y genera comentarios automáticos en español, fusionando tres dominios de la IA: **Visión por Computadora**, **Procesamiento de Lenguaje Natural** y **Síntesis de Voz**.

---

## 🧠 Arquitectura

El sistema funciona como una *pipeline* secuencial. Cada módulo tiene una responsabilidad única y se comunica con el siguiente vía texto plano.

```
┌────────────┐   frames    ┌────────────┐   texto     ┌─────────────┐
│  1. CAPTURA│ ──────────► │ 2. VISIÓN  │ ──────────► │  5. CONTROL │
│  (OpenCV)  │             │   (BLIP)   │             │ (similitud) │
└────────────┘             └────────────┘             └──────┬──────┘
                                                             │ ¿escena nueva?
                                                             ▼
                            ┌────────────┐   wav        ┌─────────────┐
                  audio ◄── │  4. TTS    │ ◄────────────│  3. NLP     │
                            │ (MMS-TTS)  │   comentario │   (LLM)     │
                            └────────────┘              └─────────────┘
```

### Por qué este orden

1. **Captura primero, vista después**: extraer frames es barato; pasarlos por BLIP no lo es. Por eso muestreamos a 1 frame cada N segundos en vez de procesar los 30 fps completos.
2. **Visión antes del control**: necesitamos texto para poder comparar semánticamente dos escenas. Es más confiable que comparar imágenes a nivel de píxel.
3. **Control antes del LLM**: el LLM es la pieza más cara del pipeline. Cortar acá ahorra el 60-80% del cómputo.

---

## 🤖 Modelos de Hugging Face usados

### 1. Visión — `Salesforce/blip-image-captioning-base`
- **Tarea**: Image Captioning (describir una imagen en texto).
- **Tamaño**: ~990 MB.
- **Por qué este**: BLIP-base es un balance ideal entre calidad y velocidad. Corre en CPU en ~1-2 s por frame y genera descripciones coherentes de escenas genéricas. Modelos más nuevos (BLIP-2, LLaVA) son más precisos pero pesan 10x más.

### 2. NLP — `Qwen/Qwen2.5-1.5B-Instruct`
- **Tarea**: Generación de texto con instrucciones (chat).
- **Tamaño**: ~3 GB.
- **Por qué este**:
  - Soporte nativo de español (Qwen está entrenado multilingüe desde el principio).
  - Sigue bien las instrucciones del *system prompt* (clave para mantener el rol de "comentarista").
  - Modelo abierto sin gating (a diferencia de Llama o Gemma, que requieren aprobación).
  - 1.5B parámetros = corre en CPU con latencia tolerable (~5-10 s por generación).
  
### 3. TTS — `facebook/mms-tts-spa`
- **Tarea**: Text-to-Speech para español.
- **Tamaño**: ~150 MB.
- **Por qué este**: Parte del proyecto **MMS** (Massively Multilingual Speech) de Meta, entrenado en 1000+ idiomas. No necesita voz de referencia (vs. XTTS), es liviano y produce voz natural en español.

### Auxiliar — `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`
- **Tarea**: Generar embeddings de texto para comparar similitud semántica.
- **Tamaño**: ~120 MB.
- Lo usamos en el módulo de control para decidir si dos descripciones consecutivas son "semánticamente la misma escena".

---

## ⚙️ Lógica de control (módulo 5)

Para evitar que el bot comente cada frame:

1. Guardamos la descripción del último frame **que sí fue comentado** (`prev_caption`).
2. En cada frame nuevo, codificamos `prev_caption` y `current_caption` a vectores (embeddings).
3. Calculamos su **similitud coseno**.
4. Si la similitud está por debajo de un umbral (configurable, default `0.75`) → la escena cambió → generamos comentario y actualizamos `prev_caption`.

> 💡 **Detalle importante**: `prev_caption` solo se actualiza cuando comentamos. Si lo actualizáramos cada frame, escenas que cambian gradualmente nunca dispararían un comentario.

---

## 🚀 Instalación y uso

```bash
# 1. Clonar y entrar al directorio
git clone <tu_repo> && cd narrador-ia

# 2. (Recomendado) crear entorno virtual
python -m venv .venv
source .venv/bin/activate         # Linux/Mac
# .venv\Scripts\activate          # Windows

# 3. Instalar dependencias
pip install -r requirements.txt

# 4. Ejecutar
streamlit run app.py
```

La primera ejecución descarga los modelos (~4-5 GB en total) al cache de Hugging Face (`~/.cache/huggingface/`). Las siguientes ejecuciones son instantáneas.

---

## 💻 Requisitos de hardware

| Componente | Mínimo (CPU)              | Recomendado (GPU)     |
| ---------- | ------------------------- | --------------------- |
| RAM        | 8 GB                      | 8 GB                  |
| VRAM       | —                         | 6 GB (CUDA)           |
| Disco      | 6 GB libres (modelos)     | 6 GB libres           |
| Velocidad  | ~15-20 s por frame nuevo  | ~1-2 s por frame      |

El código detecta automáticamente CUDA si está disponible.

---

## 📁 Estructura

```
.
├── app.py              # Aplicación completa (UI + 5 módulos)
├── requirements.txt    # Dependencias
└── README.md           # Este archivo
```

---

## 🔧 Parámetros ajustables (sidebar)

- **Intervalo de captura**: cada cuántos segundos se extrae un frame (1-5).
- **Umbral de cambio de escena**: cuán similares deben ser dos descripciones para considerarlas "la misma escena" (0.50-0.95).
- **Generar audio**: si está desactivado, se ahorra la carga del modelo TTS y la generación de .wav.

---

## 📌 Posibles mejoras

- Batching de frames en el módulo de visión para procesar varios en paralelo.
- Memoria conversacional en el LLM (que el comentarista recuerde lo que dijo antes).
- Reemplazar BLIP por un modelo entrenado específicamente en gameplay.
- Concatenar todos los audios en una sola pista sincronizada con el video original.
