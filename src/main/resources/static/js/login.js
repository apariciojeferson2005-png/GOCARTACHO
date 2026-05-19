document.querySelector('.toggle-password').addEventListener('click', function () {
    const passwordInput = document.getElementById('password');
    const icon = this.querySelector('i');
    const isPassword = passwordInput.type === 'password';
    passwordInput.type = isPassword ? 'text' : 'password';
    icon.classList.toggle('fa-eye');
    icon.classList.toggle('fa-eye-slash');
});

document.getElementById('loginForm').addEventListener('submit', function (e) {
    e.preventDefault();
    const identificador = document.getElementById('identificador').value;
    const contrasena = document.getElementById('password').value;
    const errorDiv = document.getElementById('error-message');
    const btn = document.querySelector('.btn-submit');

    btn.innerText = 'Verificando...';
    btn.disabled = true;
    errorDiv.style.display = 'none';

    fetch('/api/v1/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identificador, contrasena })
    })
        .then(async response => {
            const data = await response.json().catch(() => null);
            if (!response.ok) {
                // Si el backend mandó errores de validación de DTO (ej. formato de email)
                if (data && data.detalles) {
                    const errorMessages = Object.values(data.detalles).join('\n');
                    throw new Error(errorMessages);
                }
                // Usar el mensaje específico del servidor o un fallback seguro
                throw new Error((data && (data.message || data.error)) || 'Usuario o contraseña incorrectos.');
            }
            return data;
        })
        .then(data => {
            localStorage.setItem('usuario_gocartacho', JSON.stringify(data));
            const rol = (data.usuario && data.usuario.rol) ? data.usuario.rol.toUpperCase().trim() : "";
            const redirectUrlInput = document.getElementById('redirect-url');
            const redirectUrl = redirectUrlInput ? redirectUrlInput.value : null;
            if (redirectUrl && redirectUrl !== '/login') {
                globalThis.location.href = redirectUrl;
            } else {
                globalThis.location.href = rol === 'ADMIN' ? '/dashboard' : '/';
            }
        })
        .catch(error => {
            errorDiv.style.display = 'block';
            // Reemplazar saltos de línea para mostrar posibles errores múltiples
            errorDiv.innerHTML = (error.message || 'Usuario o contraseña incorrectos.').replace(/\n/g, '<br>');
            btn.innerText = 'Ingresar';
            btn.disabled = false;
        });
});