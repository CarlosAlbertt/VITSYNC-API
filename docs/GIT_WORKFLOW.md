# 🔀 VitSync — Flujo de Git (pull / push / merge)

> Aplica a **ambos** repos (VITSYNC-API y VITSYNC-WebApp). Última actualización: 2026-06-18.

## Ramas
- **`develop`** = rama de trabajo en ambos repos → despliega en **testing**
  (Vercel `vitsync-web-app-testing`, Render `vitsync-api-testing`).
- **`master`** = **producción** (vitsync.es / api.vitsync.es) → solo se actualiza vía
  merge desde `develop` cuando el cambio está validado.

## Identidad de commits
- Commitear como **`Carlos <carlo@vitsync.es>`**:
  ```bash
  git config user.email "carlo@vitsync.es"
  git config user.name  "Carlos"
  ```
  El email `@alu.edu.gva.es` está **bloqueado por GitHub** (rechaza el push con
  `GH007: Your push would publish a private email address`).
- Mensajes **conventional commits en español** (`feat(...)`, `fix(...)`, `chore(...)`,
  `style(...)`, `docs(...)`) + trailer:
  ```
  Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
  ```

## Antes de trabajar (pull)
```bash
git checkout develop
git pull origin develop
```

## Al terminar un cambio (push)
1. **Build/test en verde** ANTES de pushear:
   - API: `export JAVA_HOME=~/.jdks/temurin-21.0.10 && ./mvnw -o compile` (y `./mvnw -o test`).
   - WebApp: `npm run build` y `npm run test`.
2. `git add <archivos>` (NO el `application.properties` de la raíz: está en `.gitignore`).
3. `git commit` + `git push origin develop`.
4. **Nunca** `git push origin master` directamente (es producción).

## Merge a master (solo cuando el usuario lo autoriza y está validado en testing)
1. **Revertir los cambios temporales del API** (ver RESUMEN_PROYECTO §8) y aplicar los
   scripts SQL pendientes (V5/V6) en la BD de producción.
2. ```bash
   git checkout master
   git merge develop          # resolver conflictos integrando lo correcto
   git push origin master     # despliega producción
   git checkout develop
   ```

## Prohibido sin pedirlo explícitamente
- Push a `master`, `--force`/force-push, reescribir historial ya publicado.
- Tocar variables de entorno o infraestructura de **producción**.
- Subir secretos (claves, `.env`, `application.properties` con valores) — solo placeholders.
