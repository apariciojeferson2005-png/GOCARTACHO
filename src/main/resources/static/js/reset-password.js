document.getElementById('reset-password-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const token = document.getElementById('token').value;
    const nuevaContrasena = document.getElementById('nuevaContrasena').value;
    const confirmarContrasena = document.getElementById('confirmarContrasena').value;

    const btnSubmit = document.getElementById('btn-submit');
    const alertMsg = document.getElementById('alert-message');

    if (!token) {
        alertMsg.style.display = 'block';
        alertMsg.style.color = '#c62828';
        alertMsg.style.backgroundColor = 'rgba(198, 40, 40, 0.15)';
        alertMsg.textContent = "Token de seguridad inválido o ausente. Solicita un nuevo enlace.";
        return;
    }

    if (nuevaContrasena !== confirmarContrasena) {
        alertMsg.style.display = 'block';
        alertMsg.style.color = '#c62828';
        alertMsg.style.backgroundColor = 'rgba(198, 40, 40, 0.15)';
        alertMsg.textContent = "Las contraseñas no coinciden.";
        return;
    }

    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Guardando...';
    alertMsg.style.display = 'none';

    try {
        const response = await fetch('/api/v1/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token, nuevaContrasena })
        });

        const data = await response.json();
        alertMsg.style.display = 'block';

        if (response.ok) {
            alertMsg.style.color = '#3D7A5A';
            alertMsg.style.backgroundColor = 'rgba(61, 122, 90, 0.15)';
            alertMsg.textContent = data.message + " Redirigiendo al login...";
            setTimeout(() => window.location.href = '/login', 3000);
        } else {
            throw new Error(data.error || "Ocurrió un error");
        }
    } catch (error) {
        alertMsg.style.color = '#c62828';
        alertMsg.style.backgroundColor = 'rgba(198, 40, 40, 0.15)';
        alertMsg.textContent = error.message;
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Guardar Contraseña';
    }
});