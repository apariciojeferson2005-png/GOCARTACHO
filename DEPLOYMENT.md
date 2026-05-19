# 🚀 Guía de Despliegue - Gocartacho

Esta guía detalla el proceso completo paso a paso para desplegar la aplicación **Gocartacho** en producción utilizando **GitHub**, **Docker** y servicios en la nube (**Railway** o **Render**).

---

## 📋 Arquitectura de Producción

Gocartacho es una aplicación de arquitectura moderna que requiere:
1. **Frontend + Backend (Spring Boot)** ejecutándose en un contenedor Docker.
2. **Base de Datos NoSQL (MongoDB)** para la afluencia histórica, mapas de calor y rutas inteligentes.
3. **Base de Datos Relacional (MySQL)** para usuarios, comercios, reseñas, planes y la lógica principal.

---

## 🛠️ Paso 1: Subir el Proyecto a GitHub

Si aún no has subido el código a GitHub, sigue estos pasos desde la consola dentro de la carpeta del proyecto (`gocartacho`):

1. **Inicializar el repositorio Git:**
   ```bash
   git init
   ```

2. **Asegurar que los archivos estén en seguimiento:**
   El archivo `.gitignore` ya está configurado para omitir las carpetas de compilación (`target/`, `.idea/`, etc.). Agrega todos los archivos necesarios:
   ```bash
   git add .
   ```

3. **Crear el primer commit:**
   ```bash
   git commit -m "chore: preparar proyecto para despliegue en produccion"
   ```

4. **Configurar la rama principal y subir a GitHub:**
   Crea un repositorio vacío en tu cuenta de GitHub (no agregues README ni `.gitignore` al crearlo) y ejecuta:
   ```bash
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
   git push -u origin main
   ```

*A partir de este momento, cada vez que hagas `git push`, el flujo de **GitHub Actions** configurado en `.github/workflows/maven.yml` compilará y probará el código automáticamente.*

---

## 🍃 Paso 2: Configurar MongoDB en la Nube (MongoDB Atlas)

Utilizaremos la capa gratuita de **MongoDB Atlas** para almacenar los datos NoSQL de forma segura:

1. Registrate de forma gratuita en [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
2. Crea un nuevo clúster eligiendo el **proveedor gratuito (M0)** (usualmente en AWS o GCP en una región cercana).
3. **Configuración de Seguridad (Network Access):**
   - Ve a **Security** > **Network Access** > **Add IP Address**.
   - Selecciona **Allow Access from Anywhere (0.0.0.0/0)**. Esto es necesario porque plataformas como Railway o Render cambian la dirección IP del servidor dinámicamente.
4. **Configuración del Usuario (Database Access):**
   - Ve a **Security** > **Database Access** > **Add New Database User**.
   - Elige autenticación por contraseña, ingresa un usuario (ej. `gocartacho_user`) y una contraseña segura. Asígnale el rol de **Read and Write to Any Database**.
5. **Obtener la URI de conexión:**
   - Ve a **Database** > **Connect** > **Drivers**.
   - Copia la cadena de conexión (`Connection String`). Tendrá una estructura similar a:
     `mongodb+srv://gocartacho_user:<password>@cluster0.xxxx.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0`
   - Reemplaza `<password>` con la contraseña que creaste para el usuario de base de datos.
   - Agrega el nombre de la base de datos antes del `?` (ej. `...mongodb.net/gocartacho_db?retryWrites=...`).
   - Guarda esta URI para el Paso 4.

---

## 🐬 Paso 3: Configurar MySQL en la Nube

### Opción A: Base de datos integrada en Railway (Recomendada)
Si decides desplegar la aplicación en **Railway**, la base de datos MySQL se puede crear dentro de la misma plataforma y se conectará automáticamente.

### Opción B: Proveedor externo de MySQL (TiDB Cloud / Aiven)
Si prefieres un servicio externo gratuito:
1. Regístrate en [TiDB Cloud](https://en.pingcap.com/tidb-cloud/) o [Aiven](https://aiven.io/).
2. Crea una instancia gratuita de MySQL 8.
3. Copia los datos de conexión (Host, Puerto, Usuario, Contraseña, Nombre de la base de datos).
4. La URL JDBC tendrá esta estructura:
   `jdbc:mysql://HOST_PROVEEDOR:PUERTO/NOMBRE_BD?useSSL=true&allowPublicKeyRetrieval=true`

---

## 🚀 Paso 4: Despliegue de la Aplicación

### Opción 1: Desplegar en Railway (Más rápido y recomendado)

Railway detectará automáticamente nuestro `Dockerfile` y compilará la aplicación en un contenedor de producción.

1. Ve a [Railway](https://railway.app/) e inicia sesión con tu cuenta de GitHub.
2. Haz clic en **New Project** > **Deploy from GitHub repo**.
3. Selecciona tu repositorio de **Gocartacho**.
4. **Agregar base de datos MySQL en el mismo proyecto (Si elegiste Opción A del Paso 3):**
   - Haz clic en **+ New** > **Database** > **Add MySQL**.
   - Railway creará la base de datos en segundos.
5. **Configurar las Variables de Entorno:**
   - Haz clic en el servicio de tu aplicación Spring Boot y ve a la pestaña **Variables**.
   - Agrega las siguientes variables:
     | Nombre de Variable | Valor / Origen | Descripción |
     |-------------------|----------------|-------------|
     | `PORT` | `8081` (o déjalo vacío, Railway asigna uno dinámico) | El puerto de escucha |
     | `MONGODB_URI` | La URI obtenida en el **Paso 2** | Conexión a MongoDB Atlas |
     | `MYSQL_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}` | Conexión automática a la BD MySQL interna de Railway |
     | `MYSQL_USERNAME` | `${{MySQL.MYSQLUSER}}` | Usuario de la BD MySQL interna |
     | `MYSQL_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` | Contraseña de la BD MySQL interna |
     | `JWT_SECRET` | Genera una cadena aleatoria larga y segura | Llave para firmar los tokens JWT |
     | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` | Tu Client ID de Google Console | ID de cliente OAuth2 |
     | `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` | Tu Client Secret de Google Console | Clave secreta OAuth2 |
     | `GOOGLE_REDIRECT_URI` | `https://TU_APP.up.railway.app/login/oauth2/code/google` | Cambia por tu dominio de Railway |
     | `MAIL_USERNAME` | Tu cuenta de correo (ej: `contacto@gocartacho.com`) | Para envío de notificaciones |
     | `MAIL_PASSWORD` | Tu contraseña de aplicación de Google | Para autenticar el SMTP de envío |
6. ¡Listo! Railway desplegará la aplicación y te otorgará un dominio público (ej. `https://gocartacho.up.railway.app`).

---

### Opción 2: Desplegar en Render

Render es una alternativa excelente que ofrece despliegues de contenedores Docker de forma gratuita para proyectos no comerciales.

1. Ve a [Render](https://render.com/) e inicia sesión con tu cuenta de GitHub.
2. Haz clic en **New** > **Web Service**.
3. Selecciona tu repositorio de Gocartacho.
4. **Configuración básica:**
   - **Name:** `gocartacho`
   - **Environment:** `Docker` (Render detectará automáticamente el archivo `Dockerfile`).
   - **Instance Type:** `Free`.
5. **Configurar Variables de Entorno (Advanced > Add Environment Variable):**
   - Agrega las mismas variables especificadas en la tabla de Railway.
   - *Nota:* En Render, para la base de datos MySQL deberás usar la URL, usuario y contraseña del proveedor externo que hayas elegido (ej. TiDB Cloud).
6. Haz clic en **Create Web Service**. El despliegue inicial tomará unos minutos mientras descarga las dependencias y compila.

---

## 🔒 Paso 5: Configurar Google OAuth2 en Producción

Para que el inicio de sesión con Google funcione en el servidor desplegado:

1. Ve a la [Google Cloud Console](https://console.cloud.google.com/).
2. Ve a **API y Servicios** > **Credenciales**.
3. Edita las credenciales de tu cliente OAuth2 de Gocartacho.
4. En **Orígenes de JavaScript autorizados**, añade la URL de producción de tu app:
   - `https://gocartacho.up.railway.app` (o el subdominio que te dé tu proveedor).
5. En **URIs de redireccionamiento autorizados**, añade:
   - `https://gocartacho.up.railway.app/login/oauth2/code/google`
6. Guarda los cambios. (Los cambios de Google pueden tardar de 5 a 10 minutos en propagarse).

---

## 🔍 Diagnóstico y Logs

Si la aplicación no arranca o encuentras un error `500`:
- **En Railway:** Ve a la pestaña **Logs** de tu servicio para ver la consola de Spring Boot en tiempo real.
- **En Render:** Ve a la sección **Logs** en el menú lateral izquierdo de tu Web Service.
- Los errores comunes suelen ser credenciales incorrectas en la base de datos MySQL o que no habilitaste la IP `0.0.0.0/0` en MongoDB Atlas.
