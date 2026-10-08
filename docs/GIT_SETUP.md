# Flujo Git recomendado

## Caso A: el repositorio remoto ya existe y estás parado en él

Copia el contenido de esta base a la raíz del repositorio y ejecuta:

```bash
git status
git switch -c develop
git add .
git commit -m "chore: initialize distributed chat project structure"
git push -u origin develop
```

Si `develop` ya existe en remoto:

```bash
git fetch origin
git switch develop
git pull origin develop
git add .
git commit -m "chore: initialize distributed chat project structure"
git push origin develop
```

## Ramas de trabajo

Después de subir la base:

```bash
git switch develop
git pull origin develop
git switch -c feature/ice-usuarios
```

```bash
git switch develop
git pull origin develop
git switch -c feature/salas-archivos
```

```bash
git switch develop
git pull origin develop
git switch -c feature/voz-udp
```

Cada integrante trabaja únicamente en su rama y abre PR/MR hacia `develop`.
