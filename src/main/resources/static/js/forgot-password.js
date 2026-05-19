document.getElementById('forgot-password-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const email = document.getElementById('email').value;
    const btnSubmit = document.getElementById('btn-submit');
    const alertMsg = document.getElementById('alert-message');

    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Enviando...';
    alertMsg.style.display = 'none';

    try {
        const response = await fetch('/api/v1/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });

        const data = await response.json();
        alertMsg.style.display = 'block';
        alertMsg.style.color = '#3D7A5A'; // Verde oscuro
        alertMsg.style.backgroundColor = 'rgba(61, 122, 90, 0.15)';
        alertMsg.textContent = data.message || "Si el correo existe, recibirás un enlace.";
    } catch (error) {
        alertMsg.style.display = 'block';
        alertMsg.style.color = '#c62828'; // Rojo oscuro
        alertMsg.style.backgroundColor = 'rgba(198, 40, 40, 0.15)';
        alertMsg.textContent = "Ocurrió un error al intentar enviar el correo. Por favor, intenta de nuevo más tarde.";
    }
});