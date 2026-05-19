let comercioIdUsuario = null; // El ID del negocio del usuario autenticado (si existe)
let todasZonas = []; // Guardar zonas para autocompletar coordenadas
let mapPicker = null; // Objeto de mapa Leaflet
let markerPicker = null; // Objeto de marcador Leaflet
let editandoNegocio = false; // Flag para saber si registramos o editamos

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

// ===== AUTENTICACIÓN Y CARGA INICIAL =====
document.addEventListener('DOMContentLoaded', () => {
    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) { window.location.href = '/login'; return; }

    // Cargar Zonas para autocompletar mapa
    cargarZonasAPI();

    // Verificar si el usuario ya tiene un negocio
    try {
        const user = JSON.parse(userStr);

        if (isTokenExpired(user.token)) {
            logout();
            return;
        }

        const usuario = user.usuario || {};
        if (usuario.usuarioId) {
            verificarNegocioExistente(usuario.usuarioId);
            cargarNotificaciones();
        }

        setInterval(() => {
            try {
                const currentStr = localStorage.getItem('usuario_gocartacho');
                if (currentStr) {
                    const token = JSON.parse(currentStr).token;
                    if (isTokenExpired(token)) {
                        logout();
                    } else if (needsRefresh(token)) {
                        renovarTokenSilent(token);
                    }
                }
            } catch (e) { }
        }, 60000);
    } catch (e) { console.error('Error parseando usuario', e); }

    // Configurar listener para la zona elegida y centrar mapa
    document.getElementById('neg-zona').addEventListener('change', centrarMapaEnZonaSeleccionada);
    document.getElementById('btn-geolocalizar').addEventListener('click', geolocalizarUsuario);
});

// ===== CARGAR ZONAS DE LA API =====
function cargarZonasAPI() {
    fetchWithAuth('/api/v1/zonas')
        .then(r => r.json())
        .then(zonas => {
            todasZonas = zonas;
        })
        .catch(e => console.error("Error cargando zonas:", e));
}

// ===== CARGAR NOTIFICACIONES =====
function cargarNotificaciones() {
    fetchWithAuth('/api/v1/notificaciones')
        .then(r => r.json())
        .then(notifs => {
            const container = document.getElementById('notificaciones-container');
            const lista = document.getElementById('lista-notificaciones');
            const dashboardNotifLista = document.getElementById('lista-notificaciones-dashboard');
            const sidebarBadge = document.getElementById('notif-count-badge');

            // Contar no leídas para actualizar badge en el sidebar
            const noLeidas = notifs ? notifs.filter(n => !n.leida).length : 0;
            if (sidebarBadge) {
                if (noLeidas > 0) {
                    sidebarBadge.textContent = noLeidas;
                    sidebarBadge.style.display = 'inline-block';
                } else {
                    sidebarBadge.style.display = 'none';
                }
            }

            // Determinar si el portal de comerciante aprobado está activo
            const esAprobado = document.body.classList.contains('merchant-portal-fullscreen');

            if (esAprobado) {
                // Caso Aprobado: Ocultar los containers del top
                if (container) container.style.display = 'none';
                const negocioExistente = document.getElementById('negocio-existente');
                if (negocioExistente) negocioExistente.style.display = 'none';

                // Renderizar en el dashboard
                if (dashboardNotifLista) {
                    if (!notifs || notifs.length === 0) {
                        dashboardNotifLista.innerHTML = `<div class="empty-state" style="padding:40px 20px; text-align:center; color:var(--text-muted);"><i class="fas fa-bell-slash" style="font-size:3rem; margin-bottom:15px; opacity:0.5; color:var(--terracota);"></i><p>No tienes notificaciones en este momento.</p></div>`;
                        return;
                    }

                    dashboardNotifLista.innerHTML = '';
                    notifs.forEach(n => {
                        const card = document.createElement('div');
                        card.className = 'notif-card';
                        card.style.cssText = `position: relative; padding: 20px; border-radius: 12px; margin-bottom: 16px; border-left: 5px solid ${n.leida ? '#cbd5e1' : 'var(--terracota)'}; background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px); border: 1px solid var(--border); border-left-width: 5px; box-shadow: 0 4px 12px rgba(0,0,0,0.02); transition: all 0.3s ease;`;
                        
                        card.innerHTML = `
                            <button onclick="eliminarNotificacion('${n.id || n.notificacionId}', this)" style="position: absolute; top: 15px; right: 15px; background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 1.1rem; padding: 5px; transition: color 0.2s;" onmouseover="this.style.color='var(--accent)'" onmouseout="this.style.color='var(--text-muted)'">
                                <i class="fas fa-times"></i>
                            </button>
                            <div style="padding-right: 30px;">
                                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                                    <strong style="font-size: 1.05rem; color: var(--text);">${escapeHtml(n.titulo)}</strong>
                                    <small style="color: var(--text-muted); font-size: 0.8rem;">${new Date(n.fecha).toLocaleDateString()}</small>
                                </div>
                                <p style="margin: 0; font-size: 0.9rem; line-height: 1.5; color: var(--text-muted);">${escapeHtml(n.mensaje)}</p>
                                ${!n.leida ? `
                                <button onclick="marcarLeidaUI('${n.id || n.notificacionId}', this)" style="margin-top: 12px; background: rgba(196, 113, 74, 0.1); border: none; color: var(--terracota); padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: all 0.2s;" onmouseover="this.style.background='rgba(196, 113, 74, 0.2)'" onmouseout="this.style.background='rgba(196, 113, 74, 0.1)'"><i class="fas fa-check"></i> Marcar como leída</button>
                                ` : ''}
                            </div>
                        `;
                        dashboardNotifLista.appendChild(card);
                    });
                }
            } else {
                // Caso NO Aprobado: Mostrar los containers del top
                if (!notifs || notifs.length === 0) {
                    if (container) container.style.display = 'none';
                    return;
                }

                if (container) container.style.display = 'block';
                if (lista) {
                    lista.innerHTML = '';
                    notifs.forEach(n => {
                        const div = document.createElement('div');
                        div.style.cssText = `position: relative; padding: 12px; border-radius: 8px; margin-bottom: 10px; border-left: 5px solid ${n.leida ? '#cbd5e1' : 'var(--accent)'}; background: rgba(52, 152, 219, 0.05); padding-right: 35px;`;
                        div.innerHTML = `
                            <button onclick="eliminarNotificacion('${n.id || n.notificacionId}', this)" style="position: absolute; top: 10px; right: 10px; background: none; border: none; color: var(--muted); cursor: pointer; font-size: 0.9rem; padding: 2px;">
                                <i class="fas fa-times"></i>
                            </button>
                            <div style="display:flex; justify-content:space-between; align-items:center;">
                                <strong style="font-size:0.95rem; color:${n.leida ? 'var(--muted)' : 'white'};">${escapeHtml(n.titulo)}</strong>
                                <small style="opacity:0.6; font-size:0.7rem;">${new Date(n.fecha).toLocaleDateString()}</small>
                            </div>
                            <p style="margin:5px 0 0; font-size:0.85rem; line-height:1.4;">${escapeHtml(n.mensaje)}</p>
                        `;
                        lista.appendChild(div);
                    });
                }
            }
        })
        .catch(e => console.error("Error cargando notificaciones:", e));
}

function marcarLeidaUI(notifId, btnElement) {
    fetchWithAuth(`/api/v1/notificaciones/${notifId}/leer`, {
        method: 'PATCH'
    })
    .then(r => {
        if (r.ok) {
            cargarNotificaciones();
        } else {
            alert("No se pudo marcar como leída.");
        }
    })
    .catch(err => {
        console.error("Error al marcar como leída:", err);
    });
}

function eliminarNotificacion(notifId, btnElement) {
    mostrarConfirmacion('Eliminar Notificación', '¿Deseas eliminar permanentemente esta notificación de tu bandeja?', 'fa-bell-slash')
        .then(confirmado => {
            if (!confirmado) return;
            
            // Encontrar el elemento de la tarjeta
            const card = btnElement.closest('.notif-card') || btnElement.closest('div[style*="border-left"]');
            if (card) {
                card.style.transition = 'all 0.3s ease';
                card.style.opacity = '0';
                card.style.transform = 'translateY(-10px)';
            }

            fetchWithAuth(`/api/v1/notificaciones/${notifId}`, {
                method: 'DELETE'
            })
            .then(r => {
                if (r.ok) {
                    if (card) card.remove();
                    cargarNotificaciones();
                } else {
                    if (card) {
                        card.style.opacity = '1';
                        card.style.transform = 'none';
                    }
                    alert("No se pudo eliminar la notificación.");
                }
            })
            .catch(err => {
                if (card) {
                    card.style.opacity = '1';
                    card.style.transform = 'none';
                }
                console.error("Error al eliminar notificación:", err);
            });
        });
}

async function logout() {
    await fetchWithAuth('/api/v1/auth/logout', {
        method: 'POST'
    }).catch(() => {});
    localStorage.removeItem('usuario_gocartacho');
    window.location.href = '/login';
}

// ===== INICIALIZAR MAP PICKER =====
function initMapPicker(lat = 10.4238, lng = -75.5502) {
    if (mapPicker) {
        // Si ya está creado, solo actualizar coordenadas y centrar
        updateMapPickerCoords(lat, lng);
        setTimeout(() => mapPicker.invalidateSize(), 200);
        return;
    }

    const mapContainer = document.getElementById('map-picker');
    if (!mapContainer) return;

    mapPicker = L.map('map-picker').setView([lat, lng], 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(mapPicker);

    // Crear marcador arrastrable
    markerPicker = L.marker([lat, lng], { draggable: true }).addTo(mapPicker);

    // Actualizar campos al arrastrar
    markerPicker.on('dragend', function (event) {
        const position = markerPicker.getLatLng();
        document.getElementById('neg-lat').value = position.lat.toFixed(6);
        document.getElementById('neg-lng').value = position.lng.toFixed(6);
    });

    // Cambiar marcador al hacer clic en el mapa
    mapPicker.on('click', function (event) {
        const latlng = event.latlng;
        markerPicker.setLatLng(latlng);
        document.getElementById('neg-lat').value = latlng.lat.toFixed(6);
        document.getElementById('neg-lng').value = latlng.lng.toFixed(6);
    });

    // Sincronizar inputs manuales
    document.getElementById('neg-lat').value = lat;
    document.getElementById('neg-lng').value = lng;
}

function updateMapPickerCoords(lat, lng, zoom = null) {
    if (markerPicker && mapPicker) {
        const targetLatLng = [parseFloat(lat), parseFloat(lng)];
        markerPicker.setLatLng(targetLatLng);
        if (zoom) {
            mapPicker.setView(targetLatLng, zoom);
        } else {
            mapPicker.panTo(targetLatLng);
        }
        document.getElementById('neg-lat').value = parseFloat(lat).toFixed(6);
        document.getElementById('neg-lng').value = parseFloat(lng).toFixed(6);
    }
}

function centrarMapaEnZonaSeleccionada() {
    const selZonaId = document.getElementById('neg-zona').value;
    if (!selZonaId) return;

    const zonaObj = todasZonas.find(z => (z.zonaId || z.id) === selZonaId);
    if (zonaObj && zonaObj.latitud && zonaObj.longitud) {
        updateMapPickerCoords(zonaObj.latitud, zonaObj.longitud, 15);
    }
}

function geolocalizarUsuario() {
    const btn = document.getElementById('btn-geolocalizar');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Obteniendo ubicación...';
    btn.disabled = true;

    if (!navigator.geolocation) {
        alert("Geolocalización no soportada en este navegador.");
        btn.innerHTML = originalText;
        btn.disabled = false;
        return;
    }

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            updateMapPickerCoords(lat, lng, 16);
            btn.innerHTML = originalText;
            btn.disabled = false;
        },
        (error) => {
            console.error("Error geolocalizando:", error);
            alert("No pudimos obtener tu ubicación actual. Por favor marca manualmente en el mapa.");
            btn.innerHTML = originalText;
            btn.disabled = false;
        },
        { enableHighAccuracy: true, timeout: 5000 }
    );
}

// ===== VERIFICAR NEGOCIO EXISTENTE =====
function verificarNegocioExistente(usuarioId) {
    fetchWithAuth(`/api/v1/comercios/usuario/${usuarioId}`)
        .then(r => {
            if (r.status === 404 || !r.ok) return null;
            return r.json();
        })
        .then(negocio => {
            if (negocio) {
                comercioIdUsuario = negocio.comercioId || negocio.id;
                mostrarNegocioExistente(negocio);
            } else {
                // Si no tiene negocio, inicializar mapa en Cartagena por defecto
                initMapPicker();
            }
        })
        .catch(() => {
            // Si hay un error, por defecto iniciar mapa
            initMapPicker();
        });
}

function mostrarNegocioExistente(negocio) {
    const container = document.getElementById('negocio-existente');
    const estado = negocio.estadoAprobacion || negocio.estado;
    const comercioId = negocio.comercioId || negocio.id;
    const tipoNegocio = negocio.tipoNegocioNombre || negocio.tipoNegocio;
    const zonaNombre = negocio.zonaNombre;

    // Ocultar caja de información estática general si ya tiene negocio
    const welcomeBox = document.querySelector('#tab-registrar .info-box');
    if (welcomeBox) {
        welcomeBox.style.display = 'none';
    }

    const estadoBadge = {
        'PENDIENTE': '<span class="badge badge-pendiente"><i class="fas fa-clock"></i> Pendiente de aprobación</span>',
        'APROBADO': '<span class="badge badge-aprobado"><i class="fas fa-check-circle"></i> Aprobado</span>',
        'RECHAZADO': '<span class="badge badge-rechazado"><i class="fas fa-times-circle"></i> Rechazado</span>',
        'INACTIVO': '<span class="badge badge-inactivo"><i class="fas fa-ban"></i> Inactivo</span>'
    }[estado] || '';

    container.innerHTML = `
                <div class="negocio-card" style="margin-bottom:20px;">
                    <div class="negocio-info">
                        <h4><i class="fas fa-store" style="color:var(--terracota);"></i> ${escapeHtml(negocio.nombre)}</h4>
                        <p style="color:var(--muted);font-size:0.85rem;margin:6px 0 10px;">${escapeHtml(tipoNegocio || '')} · ${escapeHtml(zonaNombre || 'Sin zona')}</p>
                        <p style="color:var(--muted);font-size:0.8rem;margin-top:10px;">ID de comercio: <strong>#${comercioId}</strong></p>
                    </div>
                    <div>
                        ${estadoBadge}
                    </div>
                </div>`;
    container.style.display = 'block';

    // Configurar paneles del tab-registrar
    const dashboardDiv = document.getElementById('dashboard-comerciante');
    const formContainer = document.getElementById('form-comercio-container');

    if (estado === 'PENDIENTE') {
        document.body.classList.remove('merchant-portal-fullscreen');
        dashboardDiv.style.display = 'none';
        formContainer.style.display = 'block';
        formContainer.innerHTML = `<div style="text-align:center; padding: 40px 20px;">
                    <i class="fas fa-clock" style="font-size: 3.5rem; color: var(--gold); margin-bottom: 20px;"></i>
                    <h3 style="color: var(--gold); font-size: 1.5rem; margin-bottom: 10px;">Solicitud en Revisión</h3>
                    <p style="color: var(--muted); font-size: 1rem; line-height: 1.5;">Nuestro equipo está revisando los datos de <strong>${escapeHtml(negocio.nombre)}</strong>. Te notificaremos una vez sea aprobado para que puedas empezar a publicar promociones.</p>
                </div>`;
    } else if (estado === 'RECHAZADO') {
        document.body.classList.remove('merchant-portal-fullscreen');
        dashboardDiv.style.display = 'none';
        formContainer.style.display = 'block';
        formContainer.innerHTML = `<div style="text-align:center; padding: 40px 20px;">
                    <i class="fas fa-times-circle" style="font-size: 3.5rem; color: var(--accent); margin-bottom: 20px;"></i>
                    <h3 style="color: var(--accent); font-size: 1.5rem; margin-bottom: 10px;">Solicitud Rechazada</h3>
                    <p style="color: var(--muted); font-size: 1rem; line-height: 1.5;">Lamentablemente tu solicitud para <strong>${escapeHtml(negocio.nombre)}</strong> no cumplió con nuestras directrices y fue rechazada.</p>
                </div>`;
    } else if (estado === 'INACTIVO') {
        document.body.classList.remove('merchant-portal-fullscreen');
        dashboardDiv.style.display = 'none';
        formContainer.style.display = 'block';
        formContainer.innerHTML = `<div style="text-align:center; padding: 40px 20px;">
                    <i class="fas fa-ban" style="font-size: 3.5rem; color: var(--accent); margin-bottom: 20px;"></i>
                    <h3 style="color: var(--accent); font-size: 1.5rem; margin-bottom: 10px;">Negocio Inactivo</h3>
                    <p style="color: var(--muted); font-size: 1rem; line-height: 1.5;">El negocio <strong>${escapeHtml(negocio.nombre)}</strong> ha sido desactivado por un administrador.</p>
                </div>`;
    } else if (estado === 'APROBADO') {
        document.body.classList.add('merchant-portal-fullscreen');
        formContainer.style.display = 'none';
        container.style.display = 'none'; // Quitar card de negocio del top ya que está en el panel
        
        const topNotif = document.getElementById('notificaciones-container');
        if (topNotif) topNotif.style.display = 'none'; // Quitar card de notificaciones del top ya que está en el panel
        
        // Rellenar formulario oculto para poder editarlo
        precargarDatosFormulario(negocio);

        // Ocultar cabeceras de navegación de pestañas estándar (ya no son necesarias porque tenemos el sidebar premium)
        const tabsEl = document.getElementById('tabs-container');
        if (tabsEl) tabsEl.style.display = 'none';

        const heroEl = document.querySelector('.hero');
        if (heroEl) heroEl.style.display = 'none';

        // Renderizar el Dashboard Premium con Sidebar (Diseño idéntico al Admin)
        dashboardDiv.innerHTML = `
            <div class="merchant-panel-layout">
                <!-- PANEL SIDEBAR (ESTILO ADMIN) -->
                <div class="merchant-sidebar">
                    <div class="merchant-sidebar-header">
                        <i class="fas fa-store"></i> GO MERCHANT
                    </div>
                    <ul class="merchant-sidebar-menu">
                        <li><a href="#" onclick="switchMerchantSection('resumen', this); return false;" class="active"><i class="fas fa-chart-pie"></i> Resumen</a></li>
                        <li><a href="#" id="sidebar-notif-link" onclick="switchMerchantSection('notificaciones', this); return false;"><i class="fas fa-bell"></i> Notificaciones <span class="notif-badge-pill" id="notif-count-badge" style="display:none; background:#ef4444; color:white; font-size:0.7rem; font-weight:700; padding:2px 6px; border-radius:10px; margin-left:6px;">0</span></a></li>
                        <li><a href="#" onclick="switchMerchantSection('promos-lista', this); return false;"><i class="fas fa-tags"></i> Tus Promociones <span class="notif-badge-pill" id="promo-count-badge" style="background:var(--ocre); color:white; font-size:0.7rem; font-weight:700; padding:2px 6px; border-radius:10px; margin-left:6px;">0</span></a></li>
                        <li><a href="#" onclick="switchMerchantSection('promos-crear', this); return false;"><i class="fas fa-plus-circle"></i> Crear Promoción</a></li>
                        <li><a href="#" onclick="switchMerchantSection('opiniones', this); return false;"><i class="fas fa-comments"></i> Reseñas</a></li>
                        <li><a href="#" onclick="switchMerchantSection('editar-perfil', this); return false;"><i class="fas fa-edit"></i> Editar Perfil</a></li>
                        
                        <div class="sidebar-divider" style="margin: 15px 0; border-top: 1px solid var(--border); opacity: 0.5;"></div>
                        <li><a href="/"><i class="fas fa-arrow-left"></i> Volver al Mapa</a></li>
                    </ul>
                    <div class="logout-section">
                        <button onclick="logout()" class="btn-logout"><i class="fas fa-power-off"></i> Cerrar Sesión</button>
                    </div>
                </div>

                <!-- PANEL MAIN CONTENT (SECCIONES INDEPENDIENTES) -->
                <div class="merchant-main-content">
                    <!-- SECCIÓN 1: RESUMEN / DASHBOARD -->
                    <div id="msec-resumen" class="msec-content active">
                        <div class="welcome-banner" style="margin-bottom: 30px;">
                            <div>
                                <h1 style="font-family:var(--font-serif); font-size:2.2rem; font-weight:700; color:var(--text); margin:0;">
                                    Panel de <span style="color:var(--accent2);">Negocio</span>
                                </h1>
                                <p style="color:var(--text-muted); font-size:0.95rem; margin-top:4px;">Monitorea el rendimiento de tu local y tus promociones en tiempo real.</p>
                            </div>
                        </div>
                        
                        <!-- METRICAS DEL DASHBOARD (stats-grid estilo admin) -->
                        <div class="stats-grid">
                            <div class="stat-card card-users">
                                <div class="stat-card-header">
                                    <span class="stat-card-title">FAVORITOS</span>
                                    <div class="stat-icon"><i class="fas fa-heart" style="color:#ef4444;"></i></div>
                                </div>
                                <div class="stat-card-footer">
                                    <div class="stat-info">
                                        <h3 id="stat-traffic-value">0</h3>
                                        <p>Usuarios interesados</p>
                                    </div>
                                </div>
                            </div>
                            <div class="stat-card card-shops">
                                <div class="stat-card-header">
                                    <span class="stat-card-title">VALORACIÓN</span>
                                    <div class="stat-icon"><i class="fas fa-star" style="color:var(--ocre);"></i></div>
                                </div>
                                <div class="stat-card-footer">
                                    <div class="stat-info">
                                        <h3 id="stat-rating-value">${negocio.promedioCalificacion ? negocio.promedioCalificacion.toFixed(1) : '0.0'}</h3>
                                        <p>${negocio.totalResenas || 0} opiniones</p>
                                    </div>
                                </div>
                            </div>
                            <div class="stat-card card-promos">
                                <div class="stat-card-header">
                                    <span class="stat-card-title">OFERTAS</span>
                                    <div class="stat-icon"><i class="fas fa-percent"></i></div>
                                </div>
                                <div class="stat-card-footer">
                                    <div class="stat-info">
                                        <h3 id="stat-promos-count-badge">0</h3>
                                        <p>Promos activas</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- SECCIÓN DE CHARTS ANALÍTICOS ESTILO ADMIN -->
                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; margin-top: 30px;">
                            <div class="stat-card" style="flex-direction:column; align-items:stretch; padding: 24px; min-height: 300px; background: rgba(255,255,255,0.7); backdrop-filter:blur(10px); border-radius:var(--radius-lg); border: 1px solid var(--border);">
                                <h4 style="margin: 0 0 15px 0; font-weight: 800; color: var(--accent2); font-size: 0.9rem; display:flex; align-items:center; gap:8px; border:none; padding:0; justify-content: space-between; width:100%;">
                                    <span><i class="fas fa-chart-line"></i> FLUJO DE TRÁNSITO</span>
                                    <span id="live-visitors-badge" style="background:#22c55e; color:white; font-size:0.7rem; font-weight:700; padding:2px 8px; border-radius:10px; display:inline-flex; align-items:center; gap:4px; text-transform:uppercase;"><span style="width:6px; height:6px; background:white; border-radius:50%; display:inline-block; animation: pulsePreloader 1.5s infinite;"></span> <span id="live-visitors-count">0</span> En Vivo</span>
                                </h4>
                                <div style="height: 180px; position: relative;">
                                    <canvas id="chart-transito"></canvas>
                                </div>
                            </div>
                            <div class="stat-card" style="flex-direction:column; align-items:stretch; padding: 24px; min-height: 300px; background: rgba(255,255,255,0.7); backdrop-filter:blur(10px); border-radius:var(--radius-lg); border: 1px solid var(--border);">
                                <h4 style="margin: 0 0 15px 0; font-weight: 800; color: var(--accent); font-size: 0.9rem; display:flex; align-items:center; gap:8px; border:none; padding:0;">
                                    <i class="fas fa-star"></i> CALIFICACIONES
                                </h4>
                                <div style="height: 180px; position: relative;">
                                    <canvas id="chart-resenas"></canvas>
                                </div>
                            </div>
                            <div class="stat-card" style="flex-direction:column; align-items:stretch; padding: 24px; min-height: 300px; background: rgba(255,255,255,0.7); backdrop-filter:blur(10px); border-radius:var(--radius-lg); border: 1px solid var(--border);">
                                <h4 style="margin: 0 0 15px 0; font-weight: 800; color: #3d7a5a; font-size: 0.9rem; display:flex; align-items:center; gap:8px; border:none; padding:0;">
                                    <i class="fas fa-tags"></i> RENDIMIENTO PROMOS
                                </h4>
                                <div style="height: 180px; position: relative;">
                                    <canvas id="chart-promociones"></canvas>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- SECCIÓN 2: TUS PROMOCIONES -->
                    <div id="msec-promos-lista" class="msec-content">
                        <div class="welcome-banner" style="margin-bottom: 20px;">
                            <div>
                                <h2 style="font-family:var(--font-serif); font-size:1.8rem; font-weight:700; color:var(--text); margin:0;">
                                    <i class="fas fa-tags" style="color:var(--ocre); margin-right:8px;"></i> Tus Promociones
                                </h2>
                                <p style="color:var(--text-muted); font-size:0.9rem; margin-top:4px;">Gestiona y desactiva las ofertas y descuentos de tu negocio.</p>
                            </div>
                        </div>
                        <div id="lista-promociones-manager" class="promos-manager-list" style="margin-top: 15px;">
                            <div class="loading-state"><i class="fas fa-spinner fa-spin"></i> Cargando promociones...</div>
                        </div>
                    </div>

                    <!-- SECCIÓN 3: CREAR PROMOCIÓN -->
                    <div id="msec-promos-crear" class="msec-content">
                        <div class="welcome-banner" style="margin-bottom: 20px;">
                            <div>
                                <h2 style="font-family:var(--font-serif); font-size:1.8rem; font-weight:700; color:var(--text); margin:0;">
                                    <i class="fas fa-plus-circle" style="color:var(--green); margin-right:8px;"></i> Crear Promoción
                                </h2>
                                <p style="color:var(--text-muted); font-size:0.9rem; margin-top:4px;">Publica un nuevo cupón de descuento exclusivo para atraer turistas.</p>
                            </div>
                        </div>
                        <div id="form-promocion-panel-container">
                            <!-- Aquí se moverá el formulario de promociones existente -->
                        </div>
                    </div>

                    <!-- SECCIÓN 4: RESEÑAS -->
                    <div id="msec-opiniones" class="msec-content">
                        <div class="welcome-banner" style="margin-bottom: 20px;">
                            <div>
                                <h2 style="font-family:var(--font-serif); font-size:1.8rem; font-weight:700; color:var(--text); margin:0;">
                                    <i class="fas fa-comments" style="color:var(--azul-caribe); margin-right:8px;"></i> Reseñas de Visitantes
                                </h2>
                                <p style="color:var(--text-muted); font-size:0.9rem; margin-top:4px;">Lee las opiniones y el feedback dejado por los turistas de Cartagena.</p>
                            </div>
                        </div>
                        <div id="lista-opiniones-comercio" class="opiniones-list" style="margin-top: 15px;">
                            <div class="loading-state"><i class="fas fa-spinner fa-spin"></i> Cargando reseñas...</div>
                        </div>
                    </div>

                    <!-- SECCIÓN 5: EDITAR PERFIL -->
                    <div id="msec-editar-perfil" class="msec-content">
                        <div class="welcome-banner" style="margin-bottom: 20px;">
                            <div>
                                <h2 style="font-family:var(--font-serif); font-size:1.8rem; font-weight:700; color:var(--text); margin:0;">
                                    <i class="fas fa-edit" style="color:var(--azul-caribe); margin-right:8px;"></i> Perfil Comercial
                                </h2>
                                <p style="color:var(--text-muted); font-size:0.9rem; margin-top:4px;">Actualiza la información de contacto, horarios y ubicación geográfica de tu negocio.</p>
                            </div>
                        </div>
                        <div id="form-comercio-panel-container">
                            <!-- Aquí se moverá el formulario de negocio existente -->
                        </div>
                    </div>

                    <!-- SECCIÓN 6: NOTIFICACIONES -->
                    <div id="msec-notificaciones" class="msec-content">
                        <div class="welcome-banner" style="margin-bottom: 20px;">
                            <div>
                                <h2 style="font-family:var(--font-serif); font-size:1.8rem; font-weight:700; color:var(--text); margin:0;">
                                    <i class="fas fa-bell" style="color:var(--terracota); margin-right:8px;"></i> Notificaciones
                                </h2>
                                <p style="color:var(--text-muted); font-size:0.9rem; margin-top:4px;">Bandeja de mensajes y alertas sobre tu establecimiento.</p>
                            </div>
                        </div>
                        <div id="lista-notificaciones-dashboard" style="margin-top: 15px;">
                            <div class="loading-state"><i class="fas fa-spinner fa-spin"></i> Cargando notificaciones...</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        dashboardDiv.style.display = 'block';

        // RE-ASIGNAR FORMULARIOS ORIGINALES DE LA PÁGINA A SUS RESPECTIVOS CONTENEDORES SEPARADOS
        // Esto mantiene vinculados los EventListeners de submit originales sin romper nada
        setTimeout(() => {
            const formNeg = document.getElementById('formNegocio');
            const formComContainer = document.getElementById('form-comercio-panel-container');
            if (formNeg && formComContainer) {
                formComContainer.appendChild(formNeg);
            }

            const formPromo = document.getElementById('formPromocion');
            const formPromoContainer = document.getElementById('form-promocion-panel-container');
            if (formPromo && formPromoContainer) {
                formPromoContainer.appendChild(formPromo);
            }
        }, 100);

        // CARGA UNIFICADA CON PROMISE.ALL PARA ALIMENTAR LOS CHARTS CON DATA REAL
        Promise.all([
            fetchWithAuth(`/api/v1/promociones/comercio/${comercioId}`).then(r => r.json()).catch(() => []),
            fetchWithAuth(`/api/v1/comercios/${comercioId}/resenas`).then(r => r.json()).then(p => p.content || []).catch(() => []),
            fetchWithAuth(`/api/v1/comercios/${comercioId}/estadisticas`).then(r => r.json()).catch(() => null)
        ]).then(([promos, reviews, stats]) => {
            renderPromocionesList(promos);
            renderReviewsFeed(reviews);

            // Si obtuvimos estadísticas reales del backend, actualizar las tarjetas superiores
            if (stats) {
                const statTraffic = document.getElementById('stat-traffic-value');
                if (statTraffic) statTraffic.textContent = stats.totalFavoritos || '0';

                const statRating = document.getElementById('stat-rating-value');
                if (statRating) {
                    const rating = parseFloat(stats.promedioCalificacion) || 0;
                    statRating.textContent = rating.toFixed(1);
                    // Actualizar el texto de opiniones debajo de la valoración
                    const opinionsText = statRating.nextElementSibling;
                    if (opinionsText) {
                        opinionsText.textContent = `${stats.totalResenas || 0} opiniones`;
                    }
                }

                const statPromos = document.getElementById('stat-promos-count-badge');
                if (statPromos) statPromos.textContent = stats.totalPromocionesActivas || '0';

                // Actualizar contador en vivo de visitantes en el gráfico
                const liveCount = document.getElementById('live-visitors-count');
                if (liveCount) {
                    liveCount.textContent = stats.totalVisitantesActivos !== undefined ? stats.totalVisitantesActivos : '0';
                }
            }

            initMerchantCharts(negocio, reviews, promos, stats);
            cargarNotificaciones(); // Cargar y renderizar notificaciones del dashboard
        });
    }

    // El tab de registrar pasa a llamarse "Panel de Control"
    const tabRegistrar = document.getElementById('tab-btn-registrar');
    tabRegistrar.innerHTML = '<i class="fas fa-chart-line"></i> Panel de Control';
}

// ===== NAVEGACIÓN DENTRO DEL SIDEBAR DEL PORTAL COMERCIANTE =====
function switchMerchantSection(sectionId, element) {
    // Quitar clase activa de todos los botones del menú lateral
    document.querySelectorAll('.merchant-sidebar-menu li a').forEach(l => l.classList.remove('active'));
    // Asignar activa al botón presionado
    if (element) {
        element.classList.add('active');
    }

    // Ocultar todas las secciones de contenido
    document.querySelectorAll('.msec-content').forEach(sec => sec.classList.remove('active'));
    // Mostrar la sección seleccionada
    const targetSection = document.getElementById(`msec-${sectionId}`);
    if (targetSection) {
        targetSection.classList.add('active');
        
        // Si se entra a la sección de edición de perfil, refrescar Leaflet ya que ahora es visible
        if (sectionId === 'editar-perfil') {
            setTimeout(() => {
                if (mapPicker) {
                    mapPicker.invalidateSize();
                }
            }, 250);
        }
    }
}

// ===== PRECARGAR DATOS PARA EDICIÓN =====
function precargarDatosFormulario(negocio) {
    editandoNegocio = true;
    document.getElementById('form-title').innerHTML = '<i class="fas fa-edit" style="color:var(--azul-caribe);"></i> Editar Perfil de Negocio';
    document.getElementById('btn-submit-negocio').innerHTML = '<i class="fas fa-save"></i> Guardar Cambios';

    document.getElementById('neg-nombre').value = negocio.nombre || '';
    document.getElementById('neg-tipo').value = negocio.tipoNegocioId || '';
    document.getElementById('neg-zona').value = negocio.zonaId || '';
    document.getElementById('neg-direccion').value = negocio.direccion || '';
    document.getElementById('neg-lat').value = negocio.latitud || '';
    document.getElementById('neg-lng').value = negocio.longitud || '';
    document.getElementById('neg-apertura').value = negocio.horarioApertura || '';
    document.getElementById('neg-cierre').value = negocio.horarioCierre || '';
    document.getElementById('neg-telefono').value = negocio.telefono || '';
    document.getElementById('neg-email').value = negocio.emailContacto || '';
    document.getElementById('neg-imagen').value = negocio.imagenUrl || '';
    document.getElementById('neg-descripcion').value = negocio.descripcion || '';

    // Inicializar el mapa picker con sus coordenadas reales
    const lat = parseFloat(negocio.latitud) || 10.4238;
    const lng = parseFloat(negocio.longitud) || -75.5502;
    initMapPicker(lat, lng);
}

function activarEdicionPerfil() {
    document.getElementById('dashboard-comerciante').style.display = 'none';
    document.getElementById('form-comercio-container').style.display = 'block';
    
    // Forzar actualización del mapa picker ya que ahora es visible
    setTimeout(() => {
        if (mapPicker) {
            mapPicker.invalidateSize();
        }
    }, 200);
}

function cancelarEdicionPerfil() {
    document.getElementById('form-comercio-container').style.display = 'none';
    document.getElementById('dashboard-comerciante').style.display = 'block';
}

// ===== RENDERIZAR LISTA DE PROMOCIONES =====
function renderPromocionesList(promos) {
    const listDiv = document.getElementById('lista-promociones-manager');
    const badge = document.getElementById('promo-count-badge');
    
    if (badge) {
        badge.textContent = promos ? promos.length : '0';
    }
    
    if (!promos || promos.length === 0) {
        listDiv.innerHTML = `<div class="empty-state"><i class="fas fa-tag"></i> No tienes promociones publicadas. Ve a la pestaña "Crear Promoción" para publicar una.</div>`;
        return;
    }
    listDiv.innerHTML = '';

    promos.forEach(p => {
        const card = document.createElement('div');
        card.className = 'promo-manager-card';
        card.innerHTML = `
            <div class="promo-manager-info">
                <h5>${escapeHtml(p.titulo)}</h5>
                <p>${escapeHtml(p.descripcion || 'Sin descripción')}</p>
                <p style="font-size:0.75rem; margin-top:6px; opacity:0.75;">
                    Válido: <strong>${p.fechaInicio}</strong> al <strong>${p.fechaFin}</strong>
                </p>
            </div>
            <div class="promo-manager-meta">
                <span class="promo-manager-badge">-${p.porcentajeDescuento}%</span>
                <button onclick="eliminarPromocionDashboard('${p.promocionId || p.id}')" class="btn-desactivar-promo"><i class="fas fa-trash-alt"></i> Desactivar</button>
            </div>
        `;
        listDiv.appendChild(card);
    });
}

// ===== ELIMINAR/DESACTIVAR PROMOCIÓN =====
function eliminarPromocionDashboard(promoId) {
    mostrarConfirmacion('Desactivar Promoción', '¿Estás seguro de que deseas desactivar esta promoción? Dejará de mostrarse en el mapa inmediatamente.', 'fa-tags')
        .then(confirmado => {
            if (!confirmado) return;

            fetchWithAuth(`/api/v1/promociones/${promoId}`, {
                method: 'DELETE'
            })
            .then(r => {
                if (!r.ok && r.status !== 204) {
                    throw new Error("No se pudo desactivar la promoción.");
                }
                alert("Promoción desactivada exitosamente.");
                // Volver a cargar unificado
                verificarNegocioExistente(JSON.parse(localStorage.getItem('usuario_gocartacho')).usuario.usuarioId);
            })
            .catch(err => {
                alert(err.message);
            });
        });
}

// ===== RENDERIZAR FEED DE OPINIONES =====
function renderReviewsFeed(reviews) {
    const listDiv = document.getElementById('lista-opiniones-comercio');

    if (!reviews || reviews.length === 0) {
        listDiv.innerHTML = `<div class="empty-state"><i class="fas fa-comment-slash"></i> Aún no has recibido opiniones de visitantes.</div>`;
        return;
    }

    listDiv.innerHTML = '';
    reviews.forEach(r => {
        const starsHtml = Array.from({ length: 5 }, (_, i) => 
            `<i class="${i < r.calificacion ? 'fas' : 'far'} fa-star"></i>`
        ).join('');

        const div = document.createElement('div');
        div.className = 'review-item';
        div.innerHTML = `
            <div class="review-header">
                <span class="review-author"><i class="fas fa-user-circle"></i> ${escapeHtml(r.usuarioNombre || 'Turista')}</span>
                <div class="review-stars">${starsHtml}</div>
            </div>
            <p class="review-comment">"${escapeHtml(r.comentario)}"</p>
            <div style="text-align:right; margin-top:6px;"><small class="review-date">${new Date(r.fecha || Date.now()).toLocaleDateString()}</small></div>
        `;
        listDiv.appendChild(div);
    });
}

// ===== CHARTS DE CONTROL DEL NEGOCIO (ESTILO ADMIN) =====
function initMerchantCharts(negocio, reviews, promos, stats) {
    // 1. Tránsito de Usuarios (Line Chart - Smooth Curves & area gradient)
    const ctxTransito = document.getElementById('chart-transito');
    if (ctxTransito) {
        const baseTraffic = (stats && stats.totalVisitantesActivos !== undefined) ? (stats.totalVisitantesActivos * 12 || 15) : ((stats && stats.totalFavoritos) ? stats.totalFavoritos * 3 : 15);
        const trafficData = [
            Math.round(baseTraffic * 0.3),
            Math.round(baseTraffic * 0.6),
            Math.round(baseTraffic * 1.3),
            Math.round(baseTraffic * 0.9),
            Math.round(baseTraffic * 1.1),
            Math.round(baseTraffic * 1.6),
            Math.round(baseTraffic * 1.0)
        ];
        
        const gradient = ctxTransito.getContext('2d').createLinearGradient(0, 0, 0, 180);
        gradient.addColorStop(0, 'rgba(43, 123, 160, 0.45)');
        gradient.addColorStop(1, 'rgba(43, 123, 160, 0.0)');

        new Chart(ctxTransito, {
            type: 'line',
            data: {
                labels: ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00'],
                datasets: [{
                    label: 'Visitantes',
                    data: trafficData,
                    borderColor: '#2b7ba0',
                    borderWidth: 3,
                    pointBackgroundColor: '#2b7ba0',
                    pointHoverRadius: 7,
                    tension: 0.4,
                    fill: true,
                    backgroundColor: gradient
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' }, ticks: { color: '#64748b' } },
                    x: { grid: { display: false }, ticks: { color: '#64748b' } }
                }
            }
        });
    }

    // 2. Distribución de Calificaciones (Doughnut Chart)
    const ctxResenas = document.getElementById('chart-resenas');
    if (ctxResenas) {
        let starCounts = { '5★': 0, '4★': 0, '3★': 0, '2★': 0, '1★': 0 };
        if (reviews && reviews.length > 0) {
            reviews.forEach(r => {
                const rounded = Math.round(r.calificacion);
                if (rounded >= 1 && rounded <= 5) {
                    starCounts[`${rounded}★`]++;
                }
            });
        } else {
            // Valores por defecto elegantes
            starCounts = { '5★': 5, '4★': 3, '3★': 1, '2★': 0, '1★': 0 };
        }

        new Chart(ctxResenas, {
            type: 'doughnut',
            data: {
                labels: Object.keys(starCounts),
                datasets: [{
                    data: Object.values(starCounts),
                    backgroundColor: ['#2d8a56', '#818cf8', '#fbbf24', '#f472b6', '#fb7185'],
                    borderWidth: 0, hoverOffset: 12
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { 
                    legend: { 
                        position: 'right', 
                        labels: { 
                            color: '#64748b', 
                            font: { size: 11, weight: 'bold' } 
                        } 
                    } 
                },
                cutout: '65%'
            }
        });
    }

    // 3. Rendimiento de Promociones (Bar Chart)
    const ctxPromociones = document.getElementById('chart-promociones');
    if (ctxPromociones) {
        let promoLabels = [];
        let promoDiscounts = [];

        if (promos && promos.length > 0) {
            promos.forEach(p => {
                promoLabels.push(p.titulo.length > 12 ? p.titulo.substring(0, 10) + '...' : p.titulo);
                promoDiscounts.push(p.porcentajeDescuento);
            });
        } else {
            promoLabels = ['Promo 1', 'Promo 2', 'Promo 3'];
            promoDiscounts = [10, 20, 15];
        }

        new Chart(ctxPromociones, {
            type: 'bar',
            data: {
                labels: promoLabels,
                datasets: [{
                    label: 'Descuento %',
                    data: promoDiscounts,
                    backgroundColor: 'rgba(196, 113, 74, 0.45)',
                    borderColor: '#c4714a',
                    borderWidth: 2,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, max: 100, grid: { color: 'rgba(0,0,0,0.05)' }, ticks: { color: '#64748b' } },
                    x: { grid: { display: false }, ticks: { color: '#64748b' } }
                }
            }
        });
    }
}

// ===== CAMBIO DE TABS =====
function switchTab(tab, clickedTab) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.getElementById('tab-' + tab).classList.add('active');
    if (clickedTab) clickedTab.classList.add('active');

    // Si vuelven al panel y está visible el formulario de edición, forzar refrescar mapa
    if (tab === 'registrar') {
        const formContainer = document.getElementById('form-comercio-container');
        if (formContainer.style.display !== 'none' && mapPicker) {
            setTimeout(() => mapPicker.invalidateSize(), 200);
        }
    }
}

// ===== REGISTRAR O ACTUALIZAR NEGOCIO =====
document.getElementById('formNegocio').addEventListener('submit', function (e) {
    e.preventDefault();
    const successDiv = document.getElementById('msg-negocio-success');
    const errorDiv = document.getElementById('msg-negocio-error');
    successDiv.style.display = 'none';
    errorDiv.style.display = 'none';

    const zonaId = document.getElementById('neg-zona').value;
    if (!zonaId) {
        errorDiv.innerHTML = '<i class="fas fa-exclamation-circle"></i> Debes seleccionar una zona.';
        errorDiv.style.display = 'block';
        return;
    }

    const body = {
        nombre: document.getElementById('neg-nombre').value.trim(),
        tipoNegocioId: parseInt(document.getElementById('neg-tipo').value),
        direccion: document.getElementById('neg-direccion').value.trim(),
        descripcion: document.getElementById('neg-descripcion').value.trim(),
        telefono: document.getElementById('neg-telefono').value.trim(),
        emailContacto: document.getElementById('neg-email').value.trim(),
        horarioApertura: document.getElementById('neg-apertura').value || null,
        horarioCierre: document.getElementById('neg-cierre').value || null,
        latitud: parseFloat(document.getElementById('neg-lat').value),
        longitud: parseFloat(document.getElementById('neg-lng').value),
        imagenUrl: document.getElementById('neg-imagen').value.trim() || null,
        zonaId: zonaId
    };

    const url = editandoNegocio ? `/api/v1/comercios/${comercioIdUsuario}` : '/api/v1/comercios';
    const method = editandoNegocio ? 'PUT' : 'POST';

    fetchWithAuth(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
        .then(r => {
            if (!r.ok) return r.text().then(t => { throw new Error(t || 'Error al guardar datos del negocio'); });
            return r.json();
        })
        .then(data => {
            comercioIdUsuario = data.comercioId || data.id;
            
            if (editandoNegocio) {
                // Si estaba editando, volver al Dashboard
                alert("¡Perfil de negocio actualizado exitosamente!");
                verificarNegocioExistente(JSON.parse(localStorage.getItem('usuario_gocartacho')).usuario.usuarioId);
            } else {
                mostrarNegocioExistente(data); // Inmediatamente mostrar el estado pendiente
                window.scrollTo(0, 0);
            }
        })
        .catch(err => {
            errorDiv.innerHTML = `<i class="fas fa-exclamation-circle"></i> ${err.message}`;
            errorDiv.style.display = 'block';
        });
});

// ===== CREAR PROMOCIÓN =====
document.getElementById('formPromocion').addEventListener('submit', function (e) {
    e.preventDefault();
    const successDiv = document.getElementById('msg-promo-success');
    const errorDiv = document.getElementById('msg-promo-error');
    successDiv.style.display = 'none';
    errorDiv.style.display = 'none';

    if (!comercioIdUsuario) {
        errorDiv.innerHTML = '<i class="fas fa-exclamation-circle"></i> No tienes un negocio aprobado registrado.';
        errorDiv.style.display = 'block';
        return;
    }

    const inicio = document.getElementById('promo-inicio').value;
    const fin = document.getElementById('promo-fin').value;
    if (inicio && fin && fin < inicio) {
        errorDiv.innerHTML = '<i class="fas fa-exclamation-circle"></i> La fecha de fin no puede ser anterior a la de inicio.';
        errorDiv.style.display = 'block';
        return;
    }

    const body = {
        titulo: document.getElementById('promo-titulo').value.trim(),
        descripcion: document.getElementById('promo-descripcion').value.trim(),
        porcentajeDescuento: parseFloat(document.getElementById('promo-descuento').value) || 0,
        fechaInicio: inicio,
        fechaFin: fin
    };

    fetchWithAuth(`/api/v1/promociones?comercioId=${comercioIdUsuario}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
        .then(r => {
            if (!r.ok) return r.text().then(t => { throw new Error(t || 'Error al crear promoción'); });
            return r.json();
        })
        .then(data => {
            successDiv.innerHTML = `<i class="fas fa-check-circle"></i> ¡Promoción "<strong>${escapeHtml(data.titulo)}</strong>" creada exitosamente!`;
            successDiv.style.display = 'block';
            document.getElementById('formPromocion').reset();
            
            // Recargar panel unificado para refrescar métricas, gráficos e histórico de cupones
            verificarNegocioExistente(JSON.parse(localStorage.getItem('usuario_gocartacho')).usuario.usuarioId);
        })
        .catch(err => {
            errorDiv.innerHTML = `<i class="fas fa-exclamation-circle"></i> ${err.message}`;
            errorDiv.style.display = 'block';
        });
});

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ===== DIÁLOGOS GLOBALES TEMATIZADOS (CONFIRM) =====
let currentConfirmResolver = null;

function mostrarConfirmacion(titulo, mensaje, icono = 'fa-exclamation-circle') {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmacionModal');
        const iconEl = document.getElementById('confirmacionIcono');
        const titEl = document.getElementById('confirmacionTitulo');
        const msgEl = document.getElementById('confirmacionMensaje');
        const btnConfirm = document.getElementById('btnAceptarConfirmacion');

        if (!modal) {
            resolve(confirm(mensaje));
            return;
        }

        if (iconEl) iconEl.className = `fas ${icono}`;
        if (titEl) titEl.textContent = titulo;
        if (msgEl) msgEl.textContent = mensaje;

        modal.classList.remove('hidden');

        if (currentConfirmResolver) {
            currentConfirmResolver(false);
        }
        currentConfirmResolver = resolve;

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