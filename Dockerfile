# Imatge base amb Java 17
FROM openjdk:17

# Defineix el directori de treball
WORKDIR /app

# Copia l'arxiu JAR generat al contenidor
COPY build/libs/*.jar app.jar

# Comanda per executar l'aplicació
CMD ["java", "-jar", "app.jar"]
