// Session management
const user = localStorage.getItem('usuario_gocartacho');
if (user) {
    const btn = document.getElementById('btn-login-nav');
    const parsed = JSON.parse(user);
    const usuario = parsed.usuario || {};
    btn.innerHTML = '<i class="fas fa-sign-out-alt"></i> ' + (usuario.nombre || 'Salir').split(' ')[0];
    btn.href = "#";
    btn.onclick = () => { localStorage.removeItem('usuario_gocartacho'); window.location.href = '/logout'; };
}

function verComercios(planId) {
    const container = document.getElementById('comercios-plan-' + planId);
    if (container.classList.contains('visible')) {
        container.classList.remove('visible');
        return;
    }

    container.innerHTML = '<small style="color:var(--text-muted);">Cargando paradas...</small>';
    container.classList.add('visible');

    fetch(`/api/v1/planes/${planId}/comercios`)
        .then(res => res.json())
        .then(comercios => {
            if (comercios.length === 0) {
                container.innerHTML = '<small style="color:var(--text-muted);">No hay paradas asignadas a esta plan.</small>';
                return;
            }

            let html = '';
            comercios.forEach((c, index) => {
                html += `<div class="parada-item">
                            <span class="parada-numero">${index + 1}</span>
                            <div class="parada-info">
                                <h5>${c.nombre}</h5>
                                <small>${c.tipoNegocio}</small>
                            </div>
                            <span class="usuarios-badge"><i class="fas fa-users"></i> ${c.usuariosActuales} ahora</span>
                        </div>`;
            });

            html += `<div style="margin-top: 20px; text-align: center;">
                                <a href="/mapa?planId=${planId}" class="btn-ver filled" style="font-size: 0.95rem; padding: 12px 24px;">
                                    <i class="fas fa-check-circle"></i> Seleccionar esta Plan
                                </a>
                             </div>`;

            container.innerHTML = html;
        })
        .catch(err => {
            container.innerHTML = '<small style="color:var(--accent);">Error al cargar las paradas.</small>';
        });
}

document.addEventListener("DOMContentLoaded", function () {
    // FIX: N+1 Frontend. Obtener afluencia de todas las planes en una sola llamada.
    const planCards = document.querySelectorAll('.plan-card');
    const planIds = Array.from(planCards).map(card => {
        const match = card.querySelector('.btn-ver').getAttribute('onclick').match(/\d+/);
        return match ? match[0] : null;
    }).filter(Boolean);

    if (planIds.length === 0) return;

    // Una sola llamada para todas las planes visibles
    fetch(`/api/v1/planes/afluencia?ids=${planIds.join(',')}`)
        .then(res => res.json())
        .then(afluenciaMap => {
            planCards.forEach(card => {
                const match = card.querySelector('.btn-ver').getAttribute('onclick').match(/\d+/);
                const planId = match ? match[0] : null;
                if (!planId) return;

                const totalUsuarios = afluenciaMap[planId] || 0;

                // Si el plan tiene actividad (ej. más de 5 usuarios sumados)
                if (totalUsuarios >= 5) {
                    card.classList.add('popular');
                    const header = card.querySelector('.plan-header');
                    header.innerHTML += '<span class="popular-badge"><i class="fas fa-fire"></i> Popular ahora</span>';
                }
            });
        })
        .catch(e => console.error('Error al cargar afluencia de planes:', e));
});