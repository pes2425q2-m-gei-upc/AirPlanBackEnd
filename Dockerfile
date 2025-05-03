# Imatge base amb Java 17
FROM openjdk:17

# Defineix el directori de treball
WORKDIR /app

# Copia l'arxiu JAR generat al contenidor
COPY build/libs/*.jar app.jar

# Crear un directorio para los recursos si no existe
RUN mkdir -p /app/src/main/resources

# Comanda per executar l'aplicació
# La aplicación buscará las credenciales en la variable de entorno FIREBASE_SERVICE_ACCOUNT
# si no encuentra el archivo en la ruta por defecto
CMD ["java", "-jar", "app.jar"]

# Para ejecutar el contenedor pasando las credenciales:
# docker run -e FIREBASE_SERVICE_ACCOUNT="$(cat firebase-credentials-base64.txt)" -p 8080:8080 my-backend
