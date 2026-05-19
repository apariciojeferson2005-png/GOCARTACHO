document.addEventListener("DOMContentLoaded", () => {
    // Primero resolver el zonaId real (ObjectId) a partir del número de zona
    resolverZonaIdYCargar();
    inicializarNavegacionZonas();
    document.getElementById('btn-aplicar').addEventListener('click', loadComercios);
});

let zonaIdReal = null; // ObjectId de MongoDB de la zona

async function resolverZonaIdYCargar() {
    try {
        const resp = await fetch(`/api/v1/zonas/numero/${zonaNumero}`);
        if (resp.ok) {
            const zona = await resp.json();
            zonaIdReal = zona.zonaId; // ObjectId real
            cargarHistorialAfluencia(zonaIdReal);
        }
    } catch(e) {
        console.warn("No se pudo resolver la zona por número.");
    }
    loadComercios();
}

async function loadComercios() {
    const grid = document.getElementById('grid-comercios');
    const loader = document.getElementById('loader');
    const empty = document.getElementById('empty-state');
    
    const tipoId = document.getElementById('tipo-negocio').value;
    const orden = document.getElementById('ordenar').value;
    const abiertoAhora = document.getElementById('abierto-ahora').checked;

    grid.innerHTML = '';
    loader.classList.remove('hidden');
    empty.classList.add('hidden');

    try {
        // Usar el ObjectId resuelto, o el número como fallback
        const idParaApi = zonaIdReal || zonaNumero;
        let url = `/api/v1/comercios/zona/${idParaApi}`;
        if (tipoId) url += `?tipoId=${tipoId}`;
        
        const response = await fetch(url);
        let comercios = await response.json();

        // Filtrado por horario (opcional en frontend si no hay endpoint específico)
        if (abiertoAhora) {
            const ahora = new Date();
            const horaActual = ahora.getHours() * 100 + ahora.getMinutes();
            comercios = comercios.filter(c => {
                if (!c.horarioApertura || !c.horarioCierre) return true;
                const apert = parseInt(c.horarioApertura.replace(':', ''));
                const cierr = parseInt(c.horarioCierre.replace(':', ''));
                return horaActual >= apert && horaActual <= cierr;
            });
        }

        // Ordenación
        if (orden === 'valoracion') {
            comercios.sort((a, b) => (b.promedioCalificacion || 0) - (a.promedioCalificacion || 0));
        } else if (orden === 'nombre') {
            comercios.sort((a, b) => a.nombre.localeCompare(b.nombre));
        }

        loader.classList.add('hidden');

        if (comercios.length === 0) {
            empty.classList.remove('hidden');
            return;
        }

        comercios.forEach(c => {
            const card = renderComercioCard(c);
            grid.appendChild(card);
        });

    } catch (error) {
        console.error("Error cargando comercios:", error);
        loader.classList.add('hidden');
    }
}

const CARTAGENA_IMAGES = [
    "https://cdn.yate.co/img/blog/2023/1/plaza-santo-domingo-ctg-0psx640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/getsemani-cartagena-zopx640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/51-sky-bar-lv8x640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/yate-de-noche-ctg-pntx640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/cafe-del-mar-u4ox640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/-wk8x640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/-3zhx640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/muralla-cartagena-2uhx640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/chiva-rumbera-kfcx640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/carruaje-av5x640.jpg",
    "https://cdn.yate.co/img/blog/2023/1/imagen-ppal-3dcx640.jpg"
];

function renderComercioCard(c) {
    const card = document.createElement('div');
    card.className = 'comercio-card';
    
    const tipoClass = c.tipoNegocioNombre || c.tipoNegocio || 'Otro';
    const promedio = c.promedioCalificacion || 0;
    const totalRes = c.totalResenas || 0;

    let starsHTML = '';
    for(let i=1; i<=5; i++) {
        if (i <= Math.floor(promedio)) starsHTML += '<i class="fas fa-star"></i>';
        else if (i - 0.5 <= promedio) starsHTML += '<i class="fas fa-star-half-alt"></i>';
        else starsHTML += '<i class="far fa-star"></i>';
    }

    const imgIndex = Math.abs(c.nombre.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)) % CARTAGENA_IMAGES.length;
    const comercioImage = CARTAGENA_IMAGES[imgIndex];

    card.innerHTML = `
        <img src="${comercioImage}" alt="${c.nombre}" class="comercio-card-img" style="width: 100%; height: 180px; object-fit: cover; border-radius: 8px; margin-bottom: 15px;" />
        <div class="card-header-info">
             <span class="badge-tipo">${tipoClass}</span>
             <h4>${c.nombre}</h4>
        </div>
        <div class="comercio-rating">
            <span class="stars">${starsHTML}</span>
            <span class="rating-value">${promedio.toFixed(1)}</span>
            <span class="total-reviews">(${totalRes})</span>
        </div>
        <p class="comercio-description">${c.descripcion || 'Sin descripción disponible.'}</p>
        <div class="comercio-meta">
            <span><i class="fas fa-map-marker-alt"></i> ${c.direccion || 'Cartagena'}</span>
            ${c.horarioApertura ? `<span><i class="fas fa-clock"></i> ${c.horarioApertura.substring(0,5)} - ${c.horarioCierre.substring(0,5)}</span>` : ''}
        </div>
        <div class="comercio-footer">
            <div class="comercio-actions">
                <button class="btn-ver-resenas" onclick="abrirModalResenas('${c.comercioId}', '${c.nombre}')">
                    <i class="fas fa-comments"></i> Ver reseñas
                </button>
                <div style="display:flex; gap:10px;">
                    ${c.telefono ? `<a href="https://wa.me/${c.telefono.replace(/\+/g,'')}" target="_blank" class="btn-action-circle wa"><i class="fab fa-whatsapp"></i></a>` : ''}
                    <button class="btn-action-circle map" onclick="window.location.href='/?comercioId=${c.comercioId}'"><i class="fas fa-location-arrow"></i></button>
                </div>
            </div>
        </div>
    `;
    return card;
}

// Modal de reseñas (simplificado para el ejemplo, debería ser el mismo de index.js)
function abrirModalResenas(id, nombre) {
    const modal = document.getElementById('modal-resenas');
    const titulo = document.getElementById('modal-resenas-titulo');
    const lista = document.getElementById('modal-resenas-lista');
    
    titulo.textContent = `Reseñas de ${nombre}`;
    lista.innerHTML = '<p>Cargando reseñas...</p>';
    modal.classList.remove('hidden');

    fetch(`/api/v1/comercios/${id}/resenas`)
        .then(r => r.json())
        .then(data => {
            const resenas = data.content || [];
            if (resenas.length === 0) {
                lista.innerHTML = '<p>No hay reseñas todavía.</p>';
                return;
            }
            lista.innerHTML = resenas.map(r => `
                <div style="border-bottom: 1px solid #eee; padding: 15px 0;">
                    <div style="color:var(--gold); margin-bottom:5px;">${'⭐'.repeat(r.calificacion)}</div>
                    <p>${r.comentario}</p>
                    <small style="opacity:0.6;">${new Date(r.fecha).toLocaleDateString()}</small>
                </div>
            `).join('');
        });
}

function cerrarModalResenas() {
    document.getElementById('modal-resenas').classList.add('hidden');
}

async function inicializarNavegacionZonas() {
    try {
        const resp = await fetch('/api/v1/zonas');
        if (!resp.ok) return;
        const zonas = await resp.json();
        
        // Ordenar zonas por su número ascendente
        zonas.sort((a, b) => a.numero - b.numero);
        
        const currentIndex = zonas.findIndex(z => z.numero == zonaNumero);
        if (currentIndex === -1) return;
        
        const prevIndex = (currentIndex - 1 + zonas.length) % zonas.length;
        const nextIndex = (currentIndex + 1) % zonas.length;
        
        const prevZona = zonas[prevIndex];
        const nextZona = zonas[nextIndex];
        
        const prevBtn = document.getElementById('prev-zone-btn');
        const nextBtn = document.getElementById('next-zone-btn');
        
        if (prevBtn && prevZona) {
            prevBtn.href = `/explorar/${prevZona.numero}`;
            prevBtn.setAttribute('data-zone-name', `Anterior: ${prevZona.nombre}`);
            prevBtn.classList.remove('hidden');
        }
        if (nextBtn && nextZona) {
            nextBtn.href = `/explorar/${nextZona.numero}`;
            nextBtn.setAttribute('data-zone-name', `Siguiente: ${nextZona.nombre}`);
            nextBtn.classList.remove('hidden');
        }
    } catch(e) {
        console.error("Error al inicializar navegación de zonas:", e);
    }
}

function cargarHistorialAfluencia(zonaId) {
    const container = document.getElementById('historial-afluencia');
    const chart = document.getElementById('historial-afluencia-chart');
    const labels = document.getElementById('historial-afluencia-labels');
    if (!container || !chart || !labels) return;

    fetch(`/api/v1/heatmap/historia/${zonaId}`)
        .then(r => {
            if (!r.ok) throw new Error("Error al obtener historial");
            return r.json();
        })
        .then(data => {
            if (!data || data.length === 0) {
                container.classList.add('hidden');
                return;
            }

            const diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
            const diaHoy = diasSemana[new Date().getDay()];

            // Intentar filtrar por día de hoy
            let afluenciasDia = data.filter(a => a.diaSemana === diaHoy);
            
            // Si hoy no hay registros, tomar cualquiera para mostrar algo
            if (afluenciasDia.length === 0) {
                const diasConDatos = [...new Set(data.map(a => a.diaSemana))];
                if (diasConDatos.length > 0) {
                    afluenciasDia = data.filter(a => a.diaSemana === diasConDatos[0]);
                }
            }

            // Ordenar por hora
            afluenciasDia.sort((a, b) => a.hora - b.hora);

            if (afluenciasDia.length === 0) {
                container.classList.add('hidden');
                return;
            }

            chart.innerHTML = '';
            labels.innerHTML = '';
            container.classList.remove('hidden');

            const alturaNivel = {
                'Bajo': '25%',
                'Medio': '55%',
                'Alto': '80%',
                'Muy_Alto': '100%'
            };

            const colorNivel = {
                'Bajo': '#2a9d8f',
                'Medio': '#e9c46a',
                'Alto': '#f4a261',
                'Muy_Alto': '#e63946'
            };

            afluenciasDia.forEach(a => {
                const barContainer = document.createElement('div');
                barContainer.style.flex = '1';
                barContainer.style.display = 'flex';
                barContainer.style.flexDirection = 'column';
                barContainer.style.alignItems = 'center';
                barContainer.style.height = '100%';
                barContainer.style.justifyContent = 'flex-end';

                const bar = document.createElement('div');
                bar.style.width = '70%';
                bar.style.height = alturaNivel[a.nivelPromedio] || '20%';
                bar.style.backgroundColor = colorNivel[a.nivelPromedio] || '#888';
                bar.style.borderRadius = '4px 4px 0 0';
                bar.style.transition = 'height 0.3s ease';
                bar.title = `Hora: ${a.hora}:00 - Afluencia: ${a.nivelPromedio}`;
                
                barContainer.appendChild(bar);
                chart.appendChild(barContainer);

                const label = document.createElement('span');
                label.style.flex = '1';
                label.style.fontSize = '0.65rem';
                label.style.textAlign = 'center';
                label.style.color = 'var(--text-muted)';
                label.textContent = `${a.hora}h`;
                labels.appendChild(label);
            });
        })
        .catch(e => {
            console.error(e);
            container.classList.add('hidden');
        });
}
