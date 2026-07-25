# Se fija una versión explícita en vez de flotar sobre `latest`, para que
# un rebuild dentro de varios meses no jale silenciosamente cambios que rompan algo.
FROM apache/kafka:3.9.0

# Corre como este usuario no-root por defecto (ya viene horneado en la
# imagen base); se deja explícito acá para que sobreviva si la imagen base
# alguna vez lo cambia.
USER appuser
