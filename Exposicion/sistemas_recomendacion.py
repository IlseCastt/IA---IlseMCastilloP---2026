import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity

# 1. DATASET
datos = {
    'usuario': ['Juan', 'Juan', 'Ana', 'Ana', 'Ana', 'Luis', 'Luis', 'Luis'],
    'pelicula': ['The Matrix', 'Inception', 'Inception', 'Toy Story', 'Avengers', 'The Matrix', 'Inception', 'Toy Story'],
    'calificacion': [5.0, 4.5,   5.0, 2.0, 4.0,   1.0, 2.0, 4.0]
}

# Convertimos el diccionario en un DataFrame (tabla) de Pandas
df = pd.DataFrame(datos)
print("--- 1. DATASET ---")
print(df, "\n")

# 2. TRANSFORMACIÓN MATRIZ USUARIO-ÍTEM
# Hacemos un "pivot" para que los usuarios sean filas y las películas columnas
matriz = df.pivot_table(index='usuario', columns='pelicula', values='calificacion')

# Los valores nulos (NaN) significan que no han visto la película. 
# Para que la matemática funcione, rellenamos esos huecos con 0 
matriz_llena = matriz.fillna(0)
print("--- 2. MATRIZ USUARIO-ÍTEM ---")
print(matriz_llena, "\n")

# 3. EL ALGORITMO SIMILITUD DEL COSENO
# Comparamos matemáticamente qué tan parecidas son las filas (los usuarios) entre sí
similitud = cosine_similarity(matriz_llena)

# Convertimos el resultado en una tabla fácil de leer
df_similitud = pd.DataFrame(similitud, index=matriz.index, columns=matriz.index)
print("--- 3. MATRIZ DE SIMILITUD ENTRE USUARIOS ---")
print(df_similitud, "\n")

# 4. HACIENDO LA RECOMENDACIÓN PARA JUAN
print("--- 4. CONCLUSIÓN Y RECOMENDACIÓN ---")

# Buscamos a quién se parece más Juan (excluyéndolo a él mismo)
usuario_mas_similar = df_similitud['Juan'].drop('Juan').idxmax()
porcentaje_similitud = df_similitud['Juan'][usuario_mas_similar] * 100

print(f"El usuario con gustos más parecidos a Juan es: {usuario_mas_similar} (Similitud: {porcentaje_similitud:.1f}%)")

# Buscamos qué películas vio Ana que Juan no haya visto y que tengan buena calificación (ej. > 3.0)
peliculas_juan = df[df['usuario'] == 'Juan']['pelicula'].tolist()
peliculas_ana = df[(df['usuario'] == usuario_mas_similar) & (df['calificacion'] > 3.0)]

recomendaciones = peliculas_ana[~peliculas_ana['pelicula'].isin(peliculas_juan)]

print(f"\nRecomendamos a Juan las siguientes películas basándonos en {usuario_mas_similar}:")
for index, row in recomendaciones.iterrows():
    print(f"- {row['pelicula']} (Calificada con {row['calificacion']} estrellas)")