# AirPlan BackEnd

## Overview
AirPlanBackEnd és el servidor de l'aplicació AirPlan, encarregat de gestionar la lògica de negoci, l'accés a dades i la integració amb fonts externes. Dona suport a les funcionalitats de l'aplicació mòbil i web proporcionant serveis RESTful i gestió de dades d'usuaris, activitats i qualitat de l'aire.

## Features
- API RESTful per a la gestió de dades internes dels usuaris, activitats i rutes.
- Integració amb serveis externs per consultar la qualitat de l'aire.
- Autenticació i control d'accés.
- Gestió d'activitats i recomanacions segons ubicació.
- Estructura modular i escalable.

## Tech Stack
- **Llenguatge principal:** Kotlin (Spring Boot)
- **Contenidors:** Docker

## Getting Started

### Prerequisites
- Docker i Docker Compose (opcional per desplegament ràpid)
- JDK 17+ (si vols executar sense Docker)
- Git

### Instal·lació

1. **Clona el repositori:**
   ```bash
   git clone https://github.com/pes2425q2-m-gei-upc/AirPlanBackEnd.git
   cd AirPlanBackEnd
   ```

2. **Executa amb Docker (recomanat):**
   ```bash
   docker compose up --build
   ```
   > El servei s'iniciarà normalment al port 8080.

3. **O executa manualment:**
   - Assegura't de tenir la base de dades configurada i accessible.
   - Compila i executa l'aplicació:
     ```bash
     ./gradlew bootRun
     ```

## Directory Structure
```
AirPlanBackEnd/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   ├── resources/
│   └── test/
├── docker/
├── build.gradle.kts
├── Dockerfile
├── README.md
```

## Contributing
1. Fes un fork del projecte.
2. Crea una nova branca (`git checkout -b feature/novafuncio`).
3. Fes els teus canvis i commiteja'ls (`git commit -am 'Afegeix nova funció'`).
4. Fes un push a la branca (`git push origin feature/novafuncio`).
5. Obre una Pull Request.

## License
Aquest projecte està sota la llicència MIT. Consulta el fitxer `LICENSE` per a més informació.

## Contributors

- David González: [davigo2411](https://github.com/davigo2411)
- Marwan Aliaoui: [hospola](https://github.com/hospola)
- Oscar Cerezo: [oscecon](https://github.com/oscecon)
- Víctor Llorens: [Strifere](https://github.com/Strifere)
- Iker Santín: [iksaba](https://github.com/iksaba)
- Jan Santos: [JanSanBas](https://github.com/JanSanBas)
