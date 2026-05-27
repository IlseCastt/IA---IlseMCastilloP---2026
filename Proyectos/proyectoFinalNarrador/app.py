import os
import tempfile

import cv2
import numpy as np
import scipy.io.wavfile
import streamlit as st
import torch
from PIL import Image
from sentence_transformers import SentenceTransformer, util
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    BlipForConditionalGeneration,
    BlipProcessor,
    VitsModel,
)

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
DTYPE = torch.float16 if DEVICE == "cuda" else torch.float32

VISION_MODEL_ID = "Salesforce/blip-image-captioning-base"
LLM_MODEL_ID = "Qwen/Qwen2.5-1.5B-Instruct"
TTS_MODEL_ID = "facebook/mms-tts-spa"
SIM_MODEL_ID = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"

def extract_frames(video_path: str, interval_seconds: int = 1):
    """
    Generador que extrae UN frame cada `interval_seconds` segundos.

    Usar `yield` (en lugar de devolver una lista) es CRÍTICO:
    procesamos el video en streaming sin cargarlo entero en memoria.

    Yields:
        (idx_extraido, timestamp_segundos, frame_rgb_numpy)
    """
    cap = cv2.VideoCapture(video_path)
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    step = max(1, int(fps * interval_seconds))

    idx_total = 0
    idx_out = 0
    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            if idx_total % step == 0:
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                yield idx_out, idx_total / fps, frame_rgb
                idx_out += 1
            idx_total += 1
    finally:
        cap.release()

@st.cache_resource(show_spinner="Cargando modelo de visión (BLIP)...")
def load_vision_model():
    """Carga BLIP una sola vez por sesión gracias al cache de Streamlit."""
    processor = BlipProcessor.from_pretrained(VISION_MODEL_ID)
    model = BlipForConditionalGeneration.from_pretrained(VISION_MODEL_ID).to(DEVICE)
    model.eval()
    return processor, model


def describe_frame(frame_rgb: np.ndarray, processor, model) -> str:
    """
    Recibe un frame (numpy RGB) y devuelve una descripción.
    BLIP devuelve texto en inglés — eso está bien, después el LLM
    se encarga de traducir/reinterpretar en español.
    """
    image = Image.fromarray(frame_rgb)
    inputs = processor(image, return_tensors="pt").to(DEVICE)
    with torch.no_grad():
        output_ids = model.generate(**inputs, max_new_tokens=40)
    return processor.decode(output_ids[0], skip_special_tokens=True)

@st.cache_resource(show_spinner="Cargando LLM comentarista (Qwen)...")
def load_llm():
    tokenizer = AutoTokenizer.from_pretrained(LLM_MODEL_ID)
    model = AutoModelForCausalLM.from_pretrained(
        LLM_MODEL_ID,
        torch_dtype=DTYPE,
        low_cpu_mem_usage=True,
    ).to(DEVICE)
    model.eval()
    return tokenizer, model


def generate_commentary(description_en: str, tokenizer, model) -> str:
    """
    Toma la descripción literal (de BLIP, normalmente en inglés) y la
    transforma en un comentario corto y dinámico en español.

    El "system prompt" es donde le damos la personalidad al modelo.
    """
    system_prompt = (
        "Eres un comentarista profesional de e-sports en español. "
        "Recibes una descripción literal de una escena de videojuego y debes "
        "convertirla en un comentario en ESPAÑOL, dinámico y breve "
        "(1 o 2 frases máximo), como si narraras en vivo. "
        "No expliques la imagen, NÁRRALA con energía. No uses emojis."
    )
    user_prompt = f"Descripción de la escena: {description_en}"

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ]
    prompt_text = tokenizer.apply_chat_template(
        messages, tokenize=False, add_generation_prompt=True
    )
    inputs = tokenizer(prompt_text, return_tensors="pt").to(DEVICE)

    with torch.no_grad():
        out = model.generate(
            **inputs,
            max_new_tokens=70,
            do_sample=True,
            temperature=0.8, 
            top_p=0.9,
            pad_token_id=tokenizer.eos_token_id,
        )
    response = tokenizer.decode(
        out[0][inputs.input_ids.shape[1]:], skip_special_tokens=True
    )
    return response.strip()

@st.cache_resource(show_spinner="Cargando modelo de voz (MMS-TTS)...")
def load_tts():
    tokenizer = AutoTokenizer.from_pretrained(TTS_MODEL_ID)
    model = VitsModel.from_pretrained(TTS_MODEL_ID).to(DEVICE)
    model.eval()
    return tokenizer, model


def synthesize_speech(text: str, tokenizer, model, output_path: str) -> str:
    """Genera un .wav a partir del texto. Devuelve la ruta del archivo."""
    inputs = tokenizer(text, return_tensors="pt").to(DEVICE)
    with torch.no_grad():
        waveform = model(**inputs).waveform 
    audio = waveform.squeeze().cpu().numpy()
    sr = model.config.sampling_rate 
    scipy.io.wavfile.write(output_path, sr, audio)
    return output_path

@st.cache_resource(show_spinner="Cargando modelo de similitud...")
def load_similarity_model():
    return SentenceTransformer(SIM_MODEL_ID, device=DEVICE)


def is_scene_significant(
    prev_caption: str | None,
    current_caption: str,
    sim_model,
    threshold: float = 0.75,
) -> tuple[bool, float]:
    """
    Devuelve (es_significativo, similitud_calculada).

    Lógica:
        - Codificamos ambas descripciones a vectores (embeddings).
        - Calculamos la similitud coseno.
        - Si está POR DEBAJO del umbral → la escena cambió → SÍ comentar.

    El primer frame siempre se considera significativo (no hay referencia).
    """
    if prev_caption is None:
        return True, 0.0
    emb1 = sim_model.encode(prev_caption, convert_to_tensor=True)
    emb2 = sim_model.encode(current_caption, convert_to_tensor=True)
    similarity = util.cos_sim(emb1, emb2).item()
    return similarity < threshold, similarity

def main():
    st.set_page_config(page_title="Narrador IA", page_icon="🎮", layout="wide")
    st.title("Narrador IA")
    st.caption(
        f"Comentarista automático de gameplay · Dispositivo: **{DEVICE.upper()}**"
    )

    with st.sidebar:
        st.header("Parámetros")
        interval = st.slider("Intervalo de captura (segundos)", 1, 5, 2)
        threshold = st.slider(
            "Umbral de cambio de escena", 0.50, 0.95, 0.75, 0.05,
            help=(
                "Si la similitud entre dos descripciones consecutivas es "
                "MENOR a este valor, se considera cambio de escena. "
                "Más bajo = más estricto (comenta menos)."
            ),
        )
        enable_audio = st.checkbox("Generar audio (TTS)", value=True)
        st.divider()
        st.markdown("**Modelos en uso:**")
        st.code(
            f"Visión: {VISION_MODEL_ID}\n"
            f"NLP:    {LLM_MODEL_ID}\n"
            f"TTS:    {TTS_MODEL_ID}\n"
            f"Sim:    {SIM_MODEL_ID}",
            language="text",
        )

    uploaded = st.file_uploader(
        "Sube un clip de gameplay", type=["mp4", "mov", "avi", "mkv"]
    )
    if uploaded is None:
        st.info("Sube un video para empezar.")
        return

    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
    tmp.write(uploaded.read())
    video_path = tmp.name

    st.video(video_path)

    if not st.button("Iniciar narración", type="primary"):
        return

    vis_proc, vis_model = load_vision_model()
    llm_tok, llm_model = load_llm()
    sim_model = load_similarity_model()
    if enable_audio:
        tts_tok, tts_model = load_tts()

    cap = cv2.VideoCapture(video_path)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    cap.release()
    estimated = max(1, int(total_frames / (fps * interval)))

    progress = st.progress(0.0, text="Procesando...")
    prev_caption = None

    for idx, ts, frame in extract_frames(video_path, interval):
        progress.progress(
            min(1.0, (idx + 1) / estimated), text=f"Frame en t={ts:.1f}s"
        )

        caption = describe_frame(frame, vis_proc, vis_model)

        significant, sim = is_scene_significant(
            prev_caption, caption, sim_model, threshold
        )

        with st.container(border=True):
            col1, col2 = st.columns([1, 2])
            with col1:
                st.image(frame, caption=f"t = {ts:.1f}s", use_container_width=True)
            with col2:
                st.markdown(f"**Descripción (BLIP):** {caption}")
                st.caption(f"Similitud vs. escena anterior: {sim:.2f}")

                if significant:
                    commentary = generate_commentary(caption, llm_tok, llm_model)
                    st.markdown(f"**Comentario:** {commentary}")

                    if enable_audio:
                        audio_path = os.path.join(
                            tempfile.gettempdir(), f"narrador_{idx}.wav"
                        )
                        synthesize_speech(
                            commentary, tts_tok, tts_model, audio_path
                        )
                        st.audio(audio_path)

                    prev_caption = caption
                else:
                    st.info("Escena similar a la anterior — no se comenta.")

    progress.progress(1.0, text="¡Listo!")
    st.success("Procesamiento completado.")


if __name__ == "__main__":
    main()
