document.getElementById('registerForm').addEventListener('submit', function (e) {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const nombre = document.getElementById('nombre').value;
    const apellido = document.getElementById('apellido').value;
    const username = document.getElementById('username').value;
    const contrasena = document.getElementById('password').value;
    const errorDiv = document.getElementById('error-message');
    const successDiv = document.getElementById('success-message');
    const btn = document.querySelector('.btn-submit');

    btn.innerText = 'Registrando...'; btn.disabled = true;
    errorDiv.style.display = 'none'; successDiv.style.display = 'none';

    // SEGURIDAD: No enviamos rol desde el frontend
    fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, nombre, apellido, username, contrasena })
    })
        .then(async response => {
            const data = await response.json();
            if (!response.ok) {
                // Si hay errores de validación de los DTOs (ej. contraseña corta)
                if (data.detalles) {
                    const errorMessages = Object.values(data.detalles).join('\n');
                    throw new Error(errorMessages);
                }
                // Si hay un error lógico (ej. correo duplicado) manejado por ControllerAdvice
                throw new Error(data.message || data.error || 'Error en el registro');
            }
            return data;
        })
        .then(data => {
            // Camino exitoso
            successDiv.innerHTML = '¡Registro exitoso! Redirigiendo al login...';
            successDiv.style.display = 'block';
            document.getElementById('registerForm').reset();
            setTimeout(() => {
                window.location.href = '/login';
            }, 3000);
        })
        .catch(error => {
            // Reemplazamos los saltos de línea por <br> si vienen múltiples errores
            errorDiv.innerHTML = (error.message || 'Error al registrar. Intenta de nuevo.').replace(/\n/g, '<br>');
            errorDiv.style.display = 'block';
        })
        .finally(() => { btn.innerText = 'Registrarse'; btn.disabled = false; });
});