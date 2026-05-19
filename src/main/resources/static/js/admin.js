// ===== TOAST NOTIFICATIONS (Premium Centered Style) =====
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const iconMap = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info', warning: 'fa-triangle-exclamation' };
    const icon = iconMap[type] || iconMap.info;

    toast.innerHTML = `
        <i class="fas ${icon} toast-icon"></i>
        <span class="toast-msg">${message}</span>
        <button class="toast-close" aria-label="Cerrar">&times;</button>
    `;

    // Close on X click
    toast.querySelector('.toast-close').addEventListener('click', () => dismissToast(toast));

    container.appendChild(toast);

    // Trigger entrance animation on next frame
    requestAnimationFrame(() => toast.classList.add('toast-visible'));

    // Auto-dismiss after 3 seconds
    const timer = setTimeout(() => dismissToast(toast), 3000);
    toast._timer = timer;
}

function dismissToast(toast) {
    if (toast._dismissed) return;
    toast._dismissed = true;
    clearTimeout(toast._timer);
    toast.classList.remove('toast-visible');
    toast.classList.add('toast-exit');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
    // Fallback removal in case animationend doesn't fire
    setTimeout(() => { if (toast.parentNode) toast.remove(); }, 500);
}

// ===== FETCH WRAPPER PARA JWT =====
function fetchWithAuth(url, options = {}) {
    const userStr = localStorage.getItem('usuario_gocartacho');
    let token = '';
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            token = user.token || '';
        } catch (e) { console.error('Error parseando sesión'); }
    }

    const headers = new Headers(options.headers || {});
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    options.headers = headers;
    return fetch(url, options).then(r => {
        if (!r.ok && r.status === 401) {
            localStorage.removeItem('usuario_gocartacho');
            window.location.href = '/login';
        }
        return r;
    });
}

// ===== MAPA DE FONDO =====
function initBackgroundMap() {
    const mapBg = document.getElementById('map-background');
    if (mapBg && !mapBg.classList.contains('leaflet-container')) {
        try {
            const bgMap = L.map('map-background', {
                zoomControl: false, attributionControl: false,
                dragging: false, scrollWheelZoom: false, doubleClickZoom: false
            }).setView([10.415, -75.54], 13);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager_nolabels/{z}/{x}/{y}{r}.png').addTo(bgMap);
        } catch (e) { console.warn("Fondo animado no disponible"); }
    }
}

function isTokenExpired(token) {
    if (!token) return true;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return (payload.exp * 1000) < Date.now();
    } catch (e) {
        return true;
    }
}

function needsRefresh(token) {
    if (!token) return false;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const timeRemaining = (payload.exp * 1000) - Date.now();
        return timeRemaining > 0 && timeRemaining < 300000;
    } catch (e) {
        return false;
    }
}

let isRefreshing = false;
async function renovarTokenSilent(currentToken) {
    if (isRefreshing) return;
    isRefreshing = true;
    try {
        const response = await fetch('/api/v1/auth/refresh', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + currentToken }
        });
        if (response.ok) {
            const data = await response.json();
            const userStr = localStorage.getItem('usuario_gocartacho');
            if (userStr && data.token) {
                const userObj = JSON.parse(userStr);
                userObj.token = data.token;
                localStorage.setItem('usuario_gocartacho', JSON.stringify(userObj));
            }
        } else {
            logout();
        }
    } catch (e) { console.error("Error renovando sesión silenciosamente"); }
    finally { isRefreshing = false; }
}

// ===== VALIDACIÓN DE SESIÓN =====
document.addEventListener("DOMContentLoaded", function () {
    initBackgroundMap();

    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) { window.location.href = '/login'; return; }

    try {
        const user = JSON.parse(userStr);

        if (isTokenExpired(user.token)) {
            logout();
            return;
        }

        const usuario = user.usuario || {};
        const rol = usuario.rol ? usuario.rol.trim().toUpperCase() : "";
        if (rol !== 'ADMIN') {
            window.location.href = '/';
        }

        // --- Populate profile UI with real user data ---
        const nombre = usuario.nombre || '';
        const apellido = usuario.apellido || '';
        const displayName = nombre
            ? `${nombre} ${apellido ? apellido.charAt(0) + '.' : ''}`.trim()
            : (usuario.username || 'Admin');
        const rolLabels = { ADMIN: 'Administrador', COMERCIANTE: 'Comerciante', USER: 'Usuario' };

        const greetingEl = document.getElementById('admin-greeting-name');
        if (greetingEl) greetingEl.textContent = nombre || usuario.username || 'Admin';

        const profileNameEl = document.getElementById('admin-profile-name');
        if (profileNameEl) profileNameEl.textContent = displayName;

        const profileRoleEl = document.getElementById('admin-profile-role');
        if (profileRoleEl) profileRoleEl.textContent = rolLabels[rol] || rol;

        const avatarEl = document.getElementById('admin-avatar');
        if (avatarEl) {
            const initials = (nombre ? nombre.charAt(0) : '') + (apellido ? apellido.charAt(0) : '');
            avatarEl.textContent = initials.toUpperCase() || '?';
        }
        setInterval(() => {
            const currentStr = localStorage.getItem('usuario_gocartacho');
            if (currentStr) {
                try {
                    const token = JSON.parse(currentStr).token;
                    if (isTokenExpired(token)) {
                        logout();
                    } else if (needsRefresh(token)) {
                        renovarTokenSilent(token);
                    }
                } catch (e) { }
            }
        }, 60000);
    } catch (e) { window.location.href = '/login'; }
});

async function logout() {
    const userStr = localStorage.getItem('usuario_gocartacho');
    if (userStr) {
        try {
            const token = JSON.parse(userStr).token;
            await fetch('/api/v1/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + token }
            });
        } catch (e) { }
    }
    localStorage.removeItem('usuario_gocartacho');
    window.location.href = '/login';
}

// ===== MODAL ADMIN =====
function abrirModalAdmin() {
    document.getElementById('modalAdmin').classList.remove('hidden');
}
function cerrarModalAdmin() {
    document.getElementById('modalAdmin').classList.add('hidden');
    document.getElementById('modal-msg-success').classList.add('hidden');
    document.getElementById('modal-msg-error').classList.add('hidden');
}

// ===== CREAR ADMIN =====
if (document.getElementById('formAdmin')) {
    document.getElementById('formAdmin').addEventListener('submit', function (e) {
        e.preventDefault();
        const successDiv = document.getElementById('modal-msg-success');
        const errorDiv = document.getElementById('modal-msg-error');
        const btn = e.target.querySelector('button[type="submit"]');

        successDiv.classList.add('hidden');
        errorDiv.classList.add('hidden');

        const nombre = document.getElementById('admin-nombre-input').value.trim();
        const apellido = document.getElementById('admin-apellido-input').value.trim();
        const username = document.getElementById('admin-username-input').value.trim();
        const email = document.getElementById('admin-email-input').value.trim();
        const contrasena = document.getElementById('admin-pass-input').value;

        const originalText = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Procesando...';
        btn.disabled = true;

        fetchWithAuth('/api/v1/auth/admin/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nombre, apellido, username, email, contrasena })
        })
            .then(async r => {
                const text = await r.text();
                let data;
                try {
                    data = JSON.parse(text);
                } catch (e) {
                    data = { error: text || 'Error del servidor' };
                }

                if (!r.ok) {
                    // Manejo amigable de errores de validación
                    if (data.detalles) {
                        const msgs = Object.values(data.detalles).join(' | ');
                        throw new Error(msgs);
                    }
                    throw new Error(data.error || data.mensaje || 'Error al crear administrador');
                }
                return data;
            })
            .then(data => {
                successDiv.innerText = 'Administrador registrado con éxito';
                successDiv.classList.remove('hidden');
                document.getElementById('formAdmin').reset();
                showToast('Cuenta de administrador creada', 'success');
                // Refrescar lista de usuarios en tiempo real si el panel está abierto
                if (document.getElementById('usuarios-container')) {
                    loadUsuarios();
                }
                setTimeout(() => { cerrarModalAdmin(); }, 1500);
            })
            .catch(err => {
                errorDiv.innerText = err.message;
                errorDiv.classList.remove('hidden');
                showToast(err.message, 'error');
            })
            .finally(() => {
                btn.innerHTML = originalText;
                btn.disabled = false;
            });
    });
}

// ===== NEGOCIOS PENDIENTES =====
function loadPendientes(page = 0) {
    const searchTerm = document.getElementById('search-pendientes')?.value || '';
    let url = `/api/v1/comercios/pendientes?page=${page}&size=5&sort=comercioId,desc`;
    if (searchTerm) url += `&nombre=${encodeURIComponent(searchTerm)}`;

    fetchWithAuth(url)
        .then(r => r.json())
        .then(pageData => {
            const container = document.getElementById('pendientes-container');
            if (!container) return;

            if (!pageData.content || pageData.content.length === 0) {
                container.innerHTML = '<div style="padding:40px; text-align:center; color:var(--muted);"><i class="fas fa-champagne-glasses" style="font-size:2rem; display:block; margin-bottom:10px;"></i> Todo al día. No hay pendientes.</div>';
                document.getElementById('pendientes-pagination').innerHTML = '';
                return;
            }

            const table = crearTabla(['Establecimiento', 'Categoría', 'Zona', 'Acciones']);
            pageData.content.forEach(c => {
                const tr = document.createElement('tr');
                const comercioId = c.comercioId || c.id;
                const tipoNegocio = c.tipoNegocioNombre || c.tipoNegocio || 'General';
                tr.innerHTML = `
                <td style="font-weight:700; color:var(--text-primary);">${escapeHtml(c.nombre)}</td>
                <td><span class="status-badge" style="background:rgba(0,0,0,0.05); color:var(--text-secondary);">${escapeHtml(tipoNegocio)}</span></td>
                <td><i class="fas fa-location-dot" style="margin-right:8px; color:var(--primary-blue); opacity:0.8;"></i> <span style="color:var(--text-secondary);">${escapeHtml(c.zonaNombre || 'N/A')}</span></td>
                <td style="text-align:right;">
                    <button onclick="abrirModalEditar('${comercioId}')" class="btn-action" style="background:rgba(56, 189, 248, 0.1); color:var(--accent2); border-color:rgba(56, 189, 248, 0.2);" title="Editar"><i class="fas fa-pen-to-square"></i></button>
                    <button onclick="cambiarEstado('${comercioId}', 'APROBADO')" class="btn-action" style="background:rgba(16, 185, 129, 0.1); color:var(--green); border-color:rgba(16, 185, 129, 0.2);"><i class="fas fa-check"></i> Aprobar</button>
                    <button onclick="cambiarEstado('${comercioId}', 'RECHAZADO')" class="btn-action" style="background:rgba(239, 68, 68, 0.1); color:var(--accent); border-color:rgba(239, 68, 68, 0.2);"><i class="fas fa-xmark"></i></button>
                </td>`;
                table.appendChild(tr);
            });
            container.innerHTML = '';
            container.appendChild(table);
            renderPagination('pendientes-pagination', pageData, 'loadPendientes');
        })
        .catch(() => {
            const c = document.getElementById('pendientes-container');
            if (c) c.innerHTML = '<p style="color:var(--accent); text-align:center; padding:20px;">Error de conexión.</p>';
        });
}

// ===== NEGOCIOS ACTIVOS =====
function loadActivos(page = 0) {
    const searchTerm = document.getElementById('search-activos')?.value || '';
    let url = `/api/v1/comercios?page=${page}&size=5&sort=comercioId,desc`;
    if (searchTerm) url += `&nombre=${encodeURIComponent(searchTerm)}`;

    fetchWithAuth(url)
        .then(r => r.json())
        .then(pageData => {
            const container = document.getElementById('activos-container');
            if (!container) return;

            if (!pageData.content || pageData.content.length === 0) {
                container.innerHTML = '<p style="padding:40px; text-align:center; color:var(--muted);">No hay negocios activos.</p>';
                document.getElementById('activos-pagination').innerHTML = '';
                return;
            }

            const table = crearTabla(['Negocio', 'Tipo', 'Zona', 'Acciones']);
            pageData.content.forEach(c => {
                const tr = document.createElement('tr');
                const comercioId = c.comercioId || c.id;
                const tipoNegocio = c.tipoNegocioNombre || c.tipoNegocio || 'General';
                tr.innerHTML = `
                <td style="font-weight:700;">${escapeHtml(c.nombre)}</td>
                <td>${escapeHtml(tipoNegocio)}</td>
                <td>${escapeHtml(c.zonaNombre || 'N/A')}</td>
                <td style="text-align:right;">
                    <button onclick="abrirModalEditar('${comercioId}')" class="btn-action" style="background:var(--accent2); color:white;"><i class="fas fa-edit"></i></button>
                    <button onclick="cambiarEstado('${comercioId}', 'INACTIVO')" class="btn-action" style="background:var(--accent); color:white;"><i class="fas fa-ban"></i> Desactivar</button>
                </td>`;
                table.appendChild(tr);
            });
            container.innerHTML = '';
            container.appendChild(table);
            renderPagination('activos-pagination', pageData, 'loadActivos');
        });
}

function cambiarEstado(id, estado) {
    if (estado === 'RECHAZADO') {
        mostrarPromptMotivo('Rechazar Comercio', 'Por favor, indique el motivo detallado de rechazo para el propietario del negocio (obligatorio):', true)
            .then(motivo => {
                if (!motivo) return; // Cancelado o vacío
                fetchWithAuth(`/api/v1/comercios/${id}?motivo=${encodeURIComponent(motivo)}`, { method: 'DELETE' })
                    .then(r => r.ok ? Promise.resolve() : Promise.reject())
                    .then(() => {
                        showToast('Negocio rechazado y eliminado', 'success');
                        if (typeof loadPendientes === 'function') loadPendientes(0);
                    })
                    .catch(() => showToast('Error al eliminar', 'error'));
            });
        return;
    }

    if (estado === 'INACTIVO') {
        mostrarPromptMotivo('Desactivar Comercio', 'Indique la razón detallada de desactivación de este negocio para el propietario (obligatorio):', true)
            .then(motivo => {
                if (!motivo) return; // Cancelado
                ejecutarCambioEstado(id, estado, motivo);
            });
        return;
    }

    if (estado === 'APROBADO') {
        mostrarPromptMotivo('Aprobar Comercio', 'Escribe un mensaje de bienvenida o felicitaciones para el propietario (opcional):', false)
            .then(motivo => {
                if (motivo === null) return; // Cancelado
                ejecutarCambioEstado(id, estado, motivo);
            });
        return;
    }

    ejecutarCambioEstado(id, estado, '');
}

function ejecutarCambioEstado(id, estado, motivo) {
    let url = `/api/v1/comercios/${id}/estado?estado=${estado}`;
    if (motivo) url += `&motivo=${encodeURIComponent(motivo)}`;

    fetchWithAuth(url, { method: 'PATCH' })
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(() => {
            showToast(`Negocio ${estado.toLowerCase()} correctamente`, 'success');
            if (typeof loadPendientes === 'function') loadPendientes(0);
            if (typeof loadActivos === 'function') loadActivos(0);
        })
        .catch(() => showToast('Error al actualizar estado', 'error'));
}

// ===== PROMOCIONES =====
function loadPromociones(page = 0) {
    fetchWithAuth('/api/v1/promociones/activas')
        .then(r => r.json())
        .then(promos => {
            const container = document.getElementById('promociones-container');
            if (!container) return;

            const pagContainer = document.getElementById('promociones-pagination');

            if (!promos || promos.length === 0) {
                container.innerHTML = '<p style="padding:40px; text-align:center; color:var(--muted);">No hay ofertas vigentes.</p>';
                if (pagContainer) pagContainer.innerHTML = '';
                return;
            }

            const size = 5;
            const totalPages = Math.ceil(promos.length / size);
            const start = page * size;
            const end = Math.min(promos.length, start + size);
            const pageItems = promos.slice(start, end);

            const table = crearTabla(['Oferta', 'Descuento', 'Vence', 'Acciones']);
            pageItems.forEach(p => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td style="font-weight:700;">${escapeHtml(p.titulo)}</td>
                <td><span class="status-badge badge-active">${p.porcentajeDescuento}%</span></td>
                <td>${escapeHtml(p.fechaFin)}</td>
                <td style="text-align:right;">
                    <button onclick="cambiarEstadoPromocion('${p.promocionId}', false)" class="btn-action" style="background:var(--accent); color:white;"><i class="fas fa-trash-can"></i> Retirar</button>
                </td>`;
                table.appendChild(tr);
            });
            container.innerHTML = '';
            container.appendChild(table);

            const syntheticPageData = {
                totalPages: totalPages,
                first: page === 0,
                number: page,
                last: page === totalPages - 1,
                totalElements: promos.length
            };
            renderPagination('promociones-pagination', syntheticPageData, 'loadPromociones');
        });
}

function cambiarEstadoPromocion(id, activa) {
    mostrarConfirmacion('Desactivar Oferta', '¿Estás seguro de que deseas retirar esta promoción? Dejará de mostrarse a los usuarios en el mapa.', 'fa-tags')
        .then(confirmado => {
            if (!confirmado) return;
            fetchWithAuth(`/api/v1/promociones/${id}/estado?activa=${activa}`, { method: 'PATCH' })
                .then(r => r.ok ? showToast('Promoción retirada con éxito', 'success') || loadPromociones() : Promise.reject())
                .catch(() => showToast('Error al procesar solicitud', 'error'));
        });
}

// ===== USUARIOS =====
function loadUsuarios(page = 0) {
    const searchTerm = document.getElementById('search-usuarios')?.value.toLowerCase() || '';
    fetchWithAuth('/api/v1/auth/usuarios')
        .then(r => r.json())
        .then(usuarios => {
            const container = document.getElementById('usuarios-container');
            if (!container) return;

            const pagContainer = document.getElementById('usuarios-pagination');

            const filtrados = usuarios.filter(u =>
                (u.email && u.email.toLowerCase().includes(searchTerm)) ||
                (u.nombre && u.nombre.toLowerCase().includes(searchTerm)) ||
                (u.apellido && u.apellido.toLowerCase().includes(searchTerm)) ||
                (u.username && u.username.toLowerCase().includes(searchTerm))
            );

            if (filtrados.length === 0) {
                container.innerHTML = '<p style="padding:40px; text-align:center; color:var(--muted);">Sin coincidencias.</p>';
                if (pagContainer) pagContainer.innerHTML = '';
                return;
            }

            const size = 5;
            const totalPages = Math.ceil(filtrados.length / size);
            const start = page * size;
            const end = Math.min(filtrados.length, start + size);
            const pageItems = filtrados.slice(start, end);

            const table = crearTabla(['Nombre', 'Email', 'Privilegios', 'Acciones']);
            pageItems.forEach(u => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td style="font-weight:700;">${escapeHtml(u.nombre || 'Anónimo')}</td>
                <td>${escapeHtml(u.email)}</td>
                <td><span class="status-badge" style="background:${u.rol === 'ADMIN' ? 'rgba(56,189,248,0.1)' : 'rgba(255,255,255,0.05)'}; color:${u.rol === 'ADMIN' ? 'var(--accent2)' : 'var(--muted)'};">${u.rol}</span></td>
                <td style="text-align:right;">
                    <button onclick="eliminarUsuario('${u.usuarioId}')" class="btn-action" style="background:rgba(239,68,68,0.1); color:var(--accent);" ${u.rol === 'ADMIN' ? 'disabled' : ''}><i class="fas fa-user-xmark"></i></button>
                </td>`;
                table.appendChild(tr);
            });
            container.innerHTML = '';
            container.appendChild(table);

            const syntheticPageData = {
                totalPages: totalPages,
                first: page === 0,
                number: page,
                last: page === totalPages - 1,
                totalElements: filtrados.length
            };
            renderPagination('usuarios-pagination', syntheticPageData, 'loadUsuarios');
        });
}

function eliminarUsuario(id) {
    mostrarPromptMotivo('Eliminar Usuario', 'Por favor, indique la razón o motivo detallado para eliminar o suspender permanentemente a este usuario (obligatorio):', true)
        .then(motivo => {
            if (!motivo) return; // Cancelado o vacío
            fetchWithAuth(`/api/v1/auth/usuarios/${id}?motivo=${encodeURIComponent(motivo)}`, { method: 'DELETE' })
                .then(r => r.ok ? showToast('Usuario eliminado y notificado por correo', 'success') || loadUsuarios() : Promise.reject())
                .catch(() => showToast('Error al eliminar al usuario', 'error'));
        });
}

// ===== MODERACIÓN =====
function loadResenasReportadas(page = 0) {
    fetchWithAuth('/api/v1/admin/resenas/reportadas')
        .then(r => r.json())
        .then(resenas => {
            const container = document.getElementById('resenas-reportadas-container');
            if (!container) return;

            const pagContainer = document.getElementById('moderacion-pagination');

            if (resenas.length === 0) {
                container.innerHTML = '<p style="padding:40px; text-align:center; color:var(--muted);">Sin reportes.</p>';
                if (pagContainer) pagContainer.innerHTML = '';
                return;
            }

            const size = 5;
            const totalPages = Math.ceil(resenas.length / size);
            const start = page * size;
            const end = Math.min(resenas.length, start + size);
            const pageItems = resenas.slice(start, end);

            const table = crearTabla(['Origen', 'Comercio', 'Comentario', 'Acciones']);
            pageItems.forEach(res => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td><div style="font-weight:700;">${escapeHtml(res.usuarioNombre)}</div><div style="font-size:0.8rem; color:var(--muted);">${escapeHtml(res.usuarioEmail)}</div></td>
                <td>${escapeHtml(res.comercioNombre)}</td>
                <td style="max-width:300px; font-style:italic;">"${escapeHtml(res.comentario)}"</td>
                <td style="text-align:right; white-space:nowrap;">
                    <button onclick="descartarReporteAdmin('${res.resenaId}')" class="btn-action" style="background:rgba(16, 185, 129, 0.1); color:var(--green); border-color:rgba(16, 185, 129, 0.2); margin-right:8px;" title="Descartar reporte"><i class="fas fa-check"></i> Conservar</button>
                    <button onclick="eliminarResenaAdmin('${res.resenaId}')" class="btn-action" style="background:rgba(239, 68, 68, 0.1); color:var(--accent); border-color:rgba(239, 68, 68, 0.2);" title="Borrar reseña"><i class="fas fa-trash-can"></i> Eliminar</button>
                </td>`;
                table.appendChild(tr);
            });
            container.innerHTML = '';
            container.appendChild(table);

            const syntheticPageData = {
                totalPages: totalPages,
                first: page === 0,
                number: page,
                last: page === totalPages - 1,
                totalElements: resenas.length
            };
            renderPagination('moderacion-pagination', syntheticPageData, 'loadResenasReportadas');
        });
}

function eliminarResenaAdmin(id) {
    mostrarPromptMotivo('Eliminar Reseña', 'Por favor, indique la razón detallada de por qué elimina esta reseña del sistema (obligatorio):', true)
        .then(motivo => {
            if (!motivo) return; // Cancelado o vacío
            fetchWithAuth(`/api/v1/admin/resenas/${id}?motivo=${encodeURIComponent(motivo)}`, { method: 'DELETE' })
                .then(r => r.ok ? showToast('Reseña eliminada y autor notificado', 'success') || loadResenasReportadas() : Promise.reject())
                .catch(() => showToast('Error al procesar', 'error'));
        });
}

function descartarReporteAdmin(id) {
    mostrarPromptMotivo('Desestimar Reporte', 'Indique una justificación o motivo para descartar este reporte y conservar la reseña (opcional):', false)
        .then(motivo => {
            if (motivo === null) return; // Cancelado
            let url = `/api/v1/admin/resenas/${id}/descartar`;
            if (motivo) url += `?motivo=${encodeURIComponent(motivo)}`;

            fetchWithAuth(url, { method: 'PATCH' })
                .then(r => r.ok ? showToast('Reporte descartado. Reseña conservada.', 'success') || loadResenasReportadas() : Promise.reject())
                .catch(() => showToast('Error al descartar el reporte', 'error'));
        });
}

// ===== AUDITORÍA =====
function loadAuditoria(page = 0) {
    fetchWithAuth(`/api/v1/admin/auditoria?page=${page}&size=10&sort=fecha,desc`)
        .then(r => r.json())
        .then(pageData => {
            const container = document.getElementById('auditoria-container');
            if (!container) return;

            if (!pageData.content || pageData.content.length === 0) {
                container.innerHTML = '<p style="padding:40px; text-align:center; color:var(--muted);">Sin registros.</p>';
                return;
            }

            const table = crearTabla(['Timestamp', 'Administrador', 'Operación', 'Detalles']);
            pageData.content.forEach(log => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td>${new Date(log.fecha).toLocaleString()}</td>
                <td style="color:var(--accent2); font-weight:700;">${escapeHtml(log.adminEmail)}</td>
                <td><span class="status-badge" style="background:rgba(255,255,255,0.05);">${log.accion}</span></td>
                <td style="font-size:0.85rem; opacity:0.8;">${escapeHtml(log.detalles)}</td>
            `;
                table.appendChild(tr);
            });
            container.innerHTML = '';
            container.appendChild(table);
            renderPagination('auditoria-pagination', pageData, 'loadAuditoria');
        });
}

// ===== CHARTS (Real-time Insightful Data) =====
function initCharts() {
    // 1. Categorías de Negocio (Bar Chart - Horizontal & Spaced Out)
    const ctxCats = document.getElementById('chart-categorias');
    if (ctxCats) {
        fetchWithAuth('/api/v1/admin/stats/distribucion-categorias')
            .then(r => r.ok ? r.json() : Promise.reject('Error al cargar estadísticas'))
            .then(data => {
                const ctx = ctxCats.getContext('2d');
                // Crear un degradado horizontal hermoso usando los colores de la marca
                const gradient = ctx.createLinearGradient(0, 0, ctxCats.offsetWidth || 500, 0);
                gradient.addColorStop(0, '#009EE3'); // Turquesa Caribe de la marca
                gradient.addColorStop(1, '#004B73'); // Azul Marino profundo de la marca

                new Chart(ctxCats, {
                    type: 'bar',
                    data: {
                        labels: Object.keys(data),
                        datasets: [{
                            label: 'Comercios',
                            data: Object.values(data),
                            backgroundColor: gradient,
                            hoverBackgroundColor: '#FF3D00', // Naranja Ciudad de la marca para hovers dinámicos
                            borderRadius: 6,
                            borderSkipped: false,
                            barPercentage: 0.6,
                            categoryPercentage: 0.8
                        }]
                    },
                    options: {
                        indexAxis: 'y', // Girar el gráfico a horizontal para legibilidad perfecta de categorías
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                padding: 12,
                                backgroundColor: '#004B73', // Fondo azul marino corporativo
                                titleColor: '#ffffff',
                                bodyColor: '#e0fbfc',
                                cornerRadius: 8,
                                displayColors: false
                            }
                        },
                        scales: {
                            x: {
                                beginAtZero: true,
                                grid: {
                                    color: 'rgba(0, 75, 115, 0.05)',
                                    drawBorder: false
                                },
                                ticks: {
                                    color: '#5a7b8e',
                                    font: { family: 'Inter', size: 11, weight: '500' }
                                }
                            },
                            y: {
                                grid: { display: false },
                                ticks: {
                                    color: '#004B73', // Azul marino de alta legibilidad
                                    font: { family: 'Inter', size: 10, weight: '600' }
                                }
                            }
                        }
                    }
                });
            });
    }

    // 2. Gráfica de Zonas (Sleek Doughnut)
    const ctxZonas = document.getElementById('chart-zonas');
    if (ctxZonas) {
        fetchWithAuth('/api/v1/admin/stats/distribucion-zonas')
            .then(r => r.ok ? r.json() : Promise.reject('Error al cargar estadísticas'))
            .then(data => {
                new Chart(ctxZonas, {
                    type: 'doughnut',
                    data: {
                        labels: Object.keys(data),
                        datasets: [{
                            data: Object.values(data),
                            backgroundColor: [
                                '#009EE3', // Turquesa Caribe
                                '#FF3D00', // Naranja Ciudad
                                '#004B73', // Azul Marino profundo
                                '#FF9800', // Naranja brillante
                                '#FFC107', // Amarillo solar
                                '#06D6A0', // Verde manglar
                                '#48CAE4'  // Azul caribe suave
                            ],
                            borderWidth: 2,
                            borderColor: '#ffffff',
                            hoverOffset: 10
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: 'bottom',
                                labels: {
                                    boxWidth: 8,
                                    boxHeight: 8,
                                    usePointStyle: true,
                                    pointStyle: 'circle',
                                    padding: 15,
                                    color: '#5a7b8e',
                                    font: { family: 'Inter', size: 10, weight: '600' }
                                }
                            },
                            tooltip: {
                                padding: 10,
                                backgroundColor: '#004B73', // Azul marino corporativo
                                cornerRadius: 8
                            }
                        },
                        cutout: '76%',
                    }
                });
            });
    }

    // 3. Estados de Aprobación (Sleek Doughnut - SaaS style)
    const ctxEstados = document.getElementById('chart-estados');
    if (ctxEstados) {
        fetchWithAuth('/api/v1/admin/stats/distribucion-estados')
            .then(r => r.ok ? r.json() : Promise.reject('Error al cargar estadísticas'))
            .then(data => {
                new Chart(ctxEstados, {
                    type: 'doughnut',
                    data: {
                        labels: Object.keys(data).map(k => k === 'APROBADO' ? 'Aprobados' : (k === 'PENDIENTE' ? 'Pendientes' : (k === 'RECHAZADO' ? 'Rechazados' : k))),
                        datasets: [{
                            data: Object.values(data),
                            backgroundColor: [
                                '#06D6A0', // Aprobado (Verde manglar)
                                '#FF9800', // Pendiente (Naranja brillante)
                                '#FF3D00', // Rechazado (Naranja Ciudad / Rojo)
                                '#5a7b8e'  // Otros (Gris azulado)
                            ],
                            borderWidth: 2,
                            borderColor: '#ffffff',
                            hoverOffset: 10
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: 'bottom',
                                labels: {
                                    boxWidth: 8,
                                    boxHeight: 8,
                                    usePointStyle: true,
                                    pointStyle: 'circle',
                                    padding: 15,
                                    color: '#5a7b8e',
                                    font: { family: 'Inter', size: 10, weight: '600' }
                                }
                            },
                            tooltip: {
                                padding: 10,
                                backgroundColor: '#004B73', // Azul marino corporativo
                                cornerRadius: 8
                            }
                        },
                        cutout: '76%',
                    }
                });
            });
    }
}

// ===== HELPERS =====
function crearTabla(headers) {
    const table = document.createElement('table');
    const thead = document.createElement('thead');
    thead.innerHTML = `<tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr>`;
    table.appendChild(thead);
    return table;
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": "&#39;" }[m]));
}

function renderPagination(containerId, pageData, loadFuncName) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Si no hay páginas o solo hay una, ocultamos la paginación pero permitimos que el container esté vacío
    if (pageData.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = '<ul class="pagination">';

    // Botón Anterior
    const prevDisabled = pageData.first;
    html += `<li class="${prevDisabled ? 'disabled' : ''}">
                <a href="#" onclick="${prevDisabled ? 'return false;' : `${loadFuncName}(${pageData.number - 1}); return false;`}">
                    <i class="fas fa-arrow-left"></i>
                </a>
             </li>`;

    // Páginas numéricas
    const start = Math.max(0, pageData.number - 2);
    const end = Math.min(pageData.totalPages, start + 5);

    for (let i = start; i < end; i++) {
        html += `<li class="${i === pageData.number ? 'active' : ''}">
                    <a href="#" onclick="${i === pageData.number ? 'return false;' : `${loadFuncName}(${i}); return false;`}">${i + 1}</a>
                 </li>`;
    }

    // Botón Siguiente
    const nextDisabled = pageData.last;
    html += `<li class="${nextDisabled ? 'disabled' : ''}">
                <a href="#" onclick="${nextDisabled ? 'return false;' : `${loadFuncName}(${pageData.number + 1}); return false;`}">
                    <i class="fas fa-arrow-right"></i>
                </a>
             </li>`;

    html += '</ul>';

    // Mensaje de fin si es la última página
    if (pageData.last && pageData.totalElements > 0) {
        html += `<div class="pagination-info">
                    <i class="fas fa-info-circle"></i> No hay más negocios que ver en esta sección.
                 </div>`;
    }

    container.innerHTML = html;
}

// ===== MODAL EDICIÓN =====
function abrirModalEditar(id) {
    fetchWithAuth(`/api/v1/comercios/${id}`).then(r => r.json()).then(c => {
        document.getElementById('edit-id').value = c.comercioId;
        document.getElementById('edit-nombre').value = c.nombre;
        document.getElementById('edit-tipo').value = c.tipoNegocioId || '';
        document.getElementById('edit-direccion').value = c.direccion || '';
        document.getElementById('edit-telefono').value = c.telefono || '';
        document.getElementById('edit-email').value = c.emailContacto || '';
        document.getElementById('edit-descripcion').value = c.descripcion || '';
        document.getElementById('modal-editar-negocio').classList.remove('hidden');
    });
}
function cerrarModalEditar() { document.getElementById('modal-editar-negocio').classList.add('hidden'); }
function guardarEdicionNegocio(e) {
    e.preventDefault();
    const id = document.getElementById('edit-id').value;
    fetchWithAuth(`/api/v1/comercios/${id}`).then(r => r.json()).then(cActual => {
        const body = {
            ...cActual,
            nombre: document.getElementById('edit-nombre').value,
            tipoNegocioId: parseInt(document.getElementById('edit-tipo').value),
            direccion: document.getElementById('edit-direccion').value,
            telefono: document.getElementById('edit-telefono').value,
            emailContacto: document.getElementById('edit-email').value,
            descripcion: document.getElementById('edit-descripcion').value
        };
        return fetchWithAuth(`/api/v1/comercios/${id}`, {
            method: 'PUT', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
    }).then(r => {
        if (r.ok) { showToast('Negocio actualizado', 'success'); cerrarModalEditar(); loadPendientes(0); loadActivos(0); }
    });
}

// ===== DIÁLOGO GLOBAL DE JUSTIFICACIONES (MODAL TEMATIZADO) =====
let currentPromptResolver = null;

function mostrarPromptMotivo(titulo, subtitulo, esObligatorio = true) {
    return new Promise((resolve) => {
        const modal = document.getElementById('justificacionModal');
        const titEl = document.getElementById('justificacionTitulo');
        const subEl = document.getElementById('justificacionSubtitulo');
        const lblEl = document.getElementById('justificacionLabel');
        const textEl = document.getElementById('justificacionTextarea');
        const btnConfirm = document.getElementById('btnConfirmarJustificacion');

        if (!modal) {
            // Fallback en caso de que el modal no exista por alguna razón
            const val = prompt(subtitulo);
            resolve(val);
            return;
        }

        titEl.innerHTML = `<i class="fas fa-gavel" style="color:var(--accent2);"></i> ${titulo}`;
        subEl.textContent = subtitulo;
        lblEl.textContent = esObligatorio ? "Justificación Obligatoria" : "Mensaje / Justificación (Opcional)";
        textEl.value = '';
        textEl.placeholder = esObligatorio ? "Escribe el motivo detallado aquí (mínimo 5 caracteres)..." : "Escribe un mensaje o dedicatoria opcional...";

        modal.classList.remove('hidden');

        // Resolver cualquier promesa pendiente anterior
        if (currentPromptResolver) {
            currentPromptResolver(null);
        }

        currentPromptResolver = resolve;

        // Limpiar el evento de confirmación clonándolo
        const newBtnConfirm = btnConfirm.cloneNode(true);
        btnConfirm.parentNode.replaceChild(newBtnConfirm, btnConfirm);

        newBtnConfirm.addEventListener('click', () => {
            const val = textEl.value.trim();
            if (esObligatorio && val.length < 5) {
                showToast('Por favor, ingresa una justificación válida de al menos 5 caracteres.', 'error');
                return;
            }
            modal.classList.add('hidden');
            const res = currentPromptResolver;
            currentPromptResolver = null;
            res(val);
        });
    });
}

function cerrarJustificacionModal() {
    const modal = document.getElementById('justificacionModal');
    if (modal) {
        modal.classList.add('hidden');
    }
    if (currentPromptResolver) {
        const res = currentPromptResolver;
        currentPromptResolver = null;
        res(null);
    }
}

// ===== DIÁLOGO GLOBAL DE CONFIRMACIONES (MODAL TEMATIZADO) =====
let currentConfirmResolver = null;

function mostrarConfirmacion(titulo, mensaje, icono = 'fa-exclamation-circle') {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmacionModal');
        const iconEl = document.getElementById('confirmacionIcono');
        const titEl = document.getElementById('confirmacionTitulo');
        const msgEl = document.getElementById('confirmacionMensaje');
        const btnConfirm = document.getElementById('btnAceptarConfirmacion');

        if (!modal) {
            // Fallback
            resolve(confirm(mensaje));
            return;
        }

        iconEl.className = `fas ${icono}`;
        titEl.textContent = titulo;
        msgEl.textContent = mensaje;

        modal.classList.remove('hidden');

        if (currentConfirmResolver) {
            currentConfirmResolver(false);
        }
        currentConfirmResolver = resolve;

        // Limpiar evento clonando el botón
        const newBtnConfirm = btnConfirm.cloneNode(true);
        btnConfirm.parentNode.replaceChild(newBtnConfirm, btnConfirm);

        newBtnConfirm.addEventListener('click', () => {
            modal.classList.add('hidden');
            const res = currentConfirmResolver;
            currentConfirmResolver = null;
            res(true);
        });
    });
}

function cerrarConfirmacionModal() {
    const modal = document.getElementById('confirmacionModal');
    if (modal) {
        modal.classList.add('hidden');
    }
    if (currentConfirmResolver) {
        const res = currentConfirmResolver;
        currentConfirmResolver = null;
        res(false);
    }
}
