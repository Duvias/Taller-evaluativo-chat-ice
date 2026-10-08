# Flujo Git recomendado

## Base común en `develop`

La infraestructura compartida debe integrarse antes que las funcionalidades individuales:

```powershell
git switch develop
git pull origin develop
```

Después de copiar/aplicar las correcciones comunes:

```powershell
.\gradlew.bat clean build
git add .
git commit -m "chore: stabilize Gradle and Ice project base"
git push origin develop
```

## Ramas de trabajo

Cada integrante actualiza su rama desde `develop`:

```powershell
git switch feature/voz-udp
git merge develop
.\gradlew.bat clean build
```

Ramas sugeridas:

```text
feature/ice-usuarios
feature/salas-archivos
feature/voz-udp
```

Cada feature entra a `develop` mediante Pull Request/Merge Request.

## Regla importante

No subir `.gradle/` ni `**/build/`. Sí subir siempre el Gradle Wrapper completo, incluido `gradle/wrapper/gradle-wrapper.jar`.
