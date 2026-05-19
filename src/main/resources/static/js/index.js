let map, heatLayer, routingControl;
let zonaSeleccionada = null;
let activeRouteLayers = [];
let activeBusinessLayers = [];
let zonaPolygons = {}; // zonaId -> Leaflet polygon layer
let highlightComercioId = null;

// ===== CUSTOM PREMIUM PIN ICON =====
let customPinIcon = null;
function getCustomPinIcon() {
    if (!customPinIcon && typeof L !== 'undefined') {
        customPinIcon = L.icon({
            iconUrl: '/images/pin_GC.png',
            iconSize: [32, 42],
            iconAnchor: [16, 42],
            popupAnchor: [0, -40]
        });
    }
    return customPinIcon;
}

// Convierte hora de la API al formato HH:mm
// Soporta string "12:00:00" y array [12, 0] (distintas configs de Jackson)
function formatHora(h) {
    if (!h) return '';
    if (Array.isArray(h)) {
        return String(h[0]).padStart(2, '0') + ':' + String(h[1] || 0).padStart(2, '0');
    }
    return String(h).substring(0, 5);
}

// ===== PALETA DE COLORES POR ZONA =====
const ZONA_COLORS = {
    1: { color: '#ff1900ff', fill: '#ff1900ff', name: 'Centro Histórico' },
    2: { color: '#7D3C98', fill: '#9B59B6', name: 'Getsemaní' },
    3: { color: '#1A5276', fill: '#2980B9', name: 'Bocagrande' },
    4: { color: '#117A65', fill: '#1ABC9C', name: 'Castillogrande' },
    5: { color: '#B7950B', fill: '#F1C40F', name: 'Manga' },
    7: { color: '#ff0077ff', fill: '#ff0077ff', name: 'San Diego' },
    8: { color: '#1E8449', fill: '#27AE60', name: 'La Matuna' }
};

// ===== POLIGONOS REALES DE CADA BARRIO (coordenadas [lat, lng]) =====
const ZONA_POLYGONS = {
    1: [ /* Centro Histórico — polígono real */
        [10.428479757820767, -75.54948262676733], [10.428552748589155, -75.54950926525729],
        [10.429639140168561, -75.54851142348055], [10.429342349161331, -75.54908195585695],
        [10.428452466075086, -75.54981842605919], [10.427791362095846, -75.55100620093992],
        [10.426610065142185, -75.5523805313416], [10.425549993263857, -75.55394756933221],
        [10.424423048816536, -75.55455993473585], [10.421442226606771, -75.55382550514597],
        [10.418470583029418, -75.55271166175847], [10.419012059139641, -75.55169568343923],
        [10.420775594885686, -75.55029683150308], [10.422918111714814, -75.54885382998614],
        [10.424775315595493, -75.54702829548185], [10.424779705457922, -75.54707391592551],
        [10.424866402159825, -75.54692355622517], [10.425582421344595, -75.54718664594081],
        [10.425238848068375, -75.54870827217565], [10.42685306116318, -75.54840183286531],
        [10.42813764903623, -75.54829277850108], [10.428848019101299, -75.54843891372376],
        [10.42815493330658, -75.54923640576905], [10.428478231301906, -75.5494764491593],
        [10.428479757820767, -75.54948262676733]
    ],
    2: [ /* Getsemaní — polígono real */
        [10.42236005016369, -75.548852208485], [10.421976319912169, -75.54860737479243],
        [10.42077041201118, -75.54976268103519], [10.419711841552214, -75.54858823895272],
        [10.41908010775893, -75.54796088098524], [10.418677871690221, -75.54797116639342],
        [10.418609048265921, -75.54775352464084], [10.418450356043195, -75.54718476224708],
        [10.418089425623592, -75.54678457888367], [10.417253732489499, -75.54535351116111],
        [10.41752736401753, -75.5452495280187], [10.41769514147795, -75.54530985242944],
        [10.419858546854329, -75.54390570947783], [10.423042192466056, -75.54351714275323],
        [10.42458890180336, -75.54393188297104], [10.422747600720172, -75.54855124687117],
        [10.42236005016369, -75.548852208485]
    ],
    3: [ /* Bocagrande — polígono real */
        [10.418459846280754, -75.55271115930577], [10.414957499012033, -75.55146296341341],
        [10.409705820385923, -75.55215915795743], [10.408058482834903, -75.55368106749135],
        [10.406172490493546, -75.55464038601713], [10.40468258497716, -75.55618790701223],
        [10.40339447163123, -75.5567061898005], [10.402129936615083, -75.55862371669454],
        [10.400478657550678, -75.56047629871048], [10.399027394271414, -75.56263495734221],
        [10.397030139721274, -75.56183274197114], [10.397203050292289, -75.5614617814663],
        [10.39700936526059, -75.56065331122562], [10.395983750321477, -75.5596653912951],
        [10.395406018623689, -75.55816888413536], [10.397961892058287, -75.55556651025269],
        [10.400110159581208, -75.55296492651019], [10.403474812255496, -75.55224803870462],
        [10.405762999743088, -75.55112030849887], [10.406336987572157, -75.55035772916591],
        [10.410554551640233, -75.54837002938154], [10.410449046661181, -75.54786576174487],
        [10.410307626435563, -75.54703219767485], [10.411943987031428, -75.54641415471306],
        [10.411957278368853, -75.54588870840762], [10.413078183813191, -75.54686043158033],
        [10.412452665227368, -75.5480171259542], [10.413374199543231, -75.5486970729168],
        [10.413864020543514, -75.54814911006305], [10.413533480987715, -75.54752759642538],
        [10.413809778119717, -75.54705709950726], [10.415226283022548, -75.54799036607021],
        [10.416207973537746, -75.54950258851508], [10.417174612500162, -75.54975659341903],
        [10.418935681686676, -75.55159643162301], [10.418459846280754, -75.55271115930577]
    ],
    4: [ /* Castillogrande — polígono real */
        [10.395305518801479, -75.5582554648505], [10.394469358761185, -75.55692972204696],
        [10.394450374541613, -75.55529304184493], [10.393842239506615, -75.55433945052097],
        [10.393431746538988, -75.55283474557044], [10.393479261169286, -75.5525219595677],
        [10.393047860334889, -75.55201982185298], [10.391484922360874, -75.54919202945354],
        [10.390785723183242, -75.54794304110598], [10.390608162770818, -75.54613936759253],
        [10.390243406141138, -75.54498067496911], [10.39077940733629, -75.54471240646947],
        [10.391475048333191, -75.54487694188062], [10.392182294406481, -75.5453998323029],
        [10.392958229791176, -75.54693251754796], [10.395620318618349, -75.55112543110937],
        [10.397217424764168, -75.55369526716775], [10.397789826356659, -75.55446623055307],
        [10.398601682098075, -75.55477733978199], [10.395305518801479, -75.5582554648505]
    ],
    5: [ /* Manga — polígono real (TopoJSON) */
        [10.417926991896465, -75.54260763590896], [10.41759750142306, -75.5429635343514],
        [10.417327619192676, -75.54330243265122], [10.41724836063464, -75.54386857330344],
        [10.41692983440318, -75.54413737469339], [10.416684417744262, -75.54421020247764],
        [10.416439001085344, -75.54417490689339], [10.41541745638041, -75.54426348671575],
        [10.415132562555613, -75.54386809663517], [10.414894193353632, -75.54372949955419],
        [10.414723188094568, -75.5438037063395], [10.414673618177002, -75.54370884233133],
        [10.414547892927054, -75.54376239189386], [10.414602175814977, -75.5440722223351],
        [10.414803297791394, -75.54438342584652], [10.415057591254469, -75.54460677912223],
        [10.415074346512075, -75.54482372351575], [10.414908466192989, -75.54485214967052],
        [10.4148558531387, -75.54462517813863], [10.414418308889742, -75.54425232760163],
        [10.4142477369742, -75.54457434479036], [10.414079399547385, -75.54459666979852],
        [10.413556626978849, -75.54366820950123], [10.413720725463484, -75.54354126279983],
        [10.413038671778367, -75.54265214460837], [10.411787394433262, -75.54069655978701],
        [10.411233260875278, -75.539457053422], [10.41010711541405, -75.53845018168306],
        [10.408342808534712, -75.5385719513701], [10.408523356032603, -75.53747238927276],
        [10.407683218023138, -75.53719250073908], [10.4061673685203, -75.53946068833827],
        [10.40476591436753, -75.52640406997696], [10.40622457059699, -75.52521181752044],
        [10.408998858721702, -75.52875949556056], [10.409542273094075, -75.53047517592492],
        [10.410743501503404, -75.5303588586119], [10.410972306389809, -75.53082412786321],
        [10.410343092547706, -75.53166742838064], [10.410857903967198, -75.53213269763195],
        [10.411516916652339, -75.53155012203439], [10.41217592933748, -75.53096754643681],
        [10.41263611375863, -75.53107304915387], [10.41280611169762, -75.53084727299131],
        [10.413426111884363, -75.5310065160996], [10.416749574134897, -75.53474983717028],
        [10.417025526505899, -75.53617544589795], [10.41720768546329, -75.53613065930392],
        [10.417363259120913, -75.53618048065753], [10.417441785066572, -75.53643555070556],
        [10.41732092134388, -75.53667710533198], [10.417346276699702, -75.53699975248512],
        [10.41755403931682, -75.53693879899515], [10.417655460742788, -75.53718532133315],
        [10.41769041900221, -75.53756699788136], [10.41786494982773, -75.53789461274609],
        [10.417845874230892, -75.53809896644107], [10.417707165014406, -75.53827628929433],
        [10.417813766680375, -75.53847848422792], [10.417906945388243, -75.53877555529891],
        [10.418013416708405, -75.53886989505529], [10.41830737079271, -75.53872674716158],
        [10.41859467843274, -75.53854981071566], [10.41867965747617, -75.53868291559266],
        [10.418645003226548, -75.53891062841578], [10.418768437230833, -75.53920386901073],
        [10.41869628317058, -75.53926324878043], [10.418724778665455, -75.53950364710927],
        [10.41860896601484, -75.53963304281969], [10.418739066225458, -75.53972864997716],
        [10.418869166436075, -75.5396891029244], [10.418884449039991, -75.53985528481878],
        [10.419008062336697, -75.53990382476468], [10.419467970117402, -75.54101456123567],
        [10.419124991805077, -75.54115465933722], [10.418988533981912, -75.54144440477627],
        [10.418759028515797, -75.54150438805891], [10.418619039556212, -75.54182708593885],
        [10.417926991896465, -75.54260763590896]
    ],
    7: [ /* San Diego — polígono real */
        [10.431788383522942, -75.54561562238723], [10.43168588316, -75.54605720027149],
        [10.430973502616226, -75.54700525158023], [10.430083642512926, -75.5476955798921],
        [10.429635278440031, -75.54845986229019], [10.428590000166139, -75.54941948548077],
        [10.42826122754019, -75.54927857033935], [10.428903922867008, -75.54838152381757],
        [10.427896857856467, -75.54823776831392], [10.425236033122232, -75.54869647622874],
        [10.425636868263737, -75.54712058248829], [10.424890398280738, -75.54688923546954],
        [10.426704339862852, -75.54423705804301], [10.42840634656595, -75.5448630221383],
        [10.42975476078324, -75.54417129165833], [10.431788383522942, -75.54561562238723]
    ],
    8: [ /* La Matuna — polígono real */
        [10.42650297168619, -75.54457576052212],
        [10.42621450432378, -75.54507722670779],
        [10.424948966928596, -75.5466951647793],
        [10.42335773246515, -75.54853072025227],
        [10.4228738466893, -75.54848341212154],
        [10.424641887227168, -75.54397967807454],
        [10.42650297168619, -75.54457576052212]
    ]
};

document.addEventListener("DOMContentLoaded", async function () {
    // Verificar si venimos de un login de Google (token en la URL)
    const urlParams = new URLSearchParams(window.location.search);
    const tokenOauth = urlParams.get('token');

    if (tokenOauth) {
        try {
            console.log("Token OAuth detectado, sincronizando sesión...");
            const res = await fetch('/api/v1/auth/me', {
                headers: { 'Authorization': 'Bearer ' + tokenOauth }
            });
            if (res.ok) {
                const usuario = await res.json();
                localStorage.setItem('usuario_gocartacho', JSON.stringify({
                    token: tokenOauth,
                    usuario: usuario
                }));
                console.log("Sesión sincronizada con éxito.");
                // Limpiar la URL sin recargar
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        } catch (e) {
            console.error("Error sincronizando sesión OAuth:", e);
        }
    }

    gestionarBotones();
    initMap();
    generarChipsZonas();
    configurarFiltros();

    // Desvanecer preloader global
    const preloader = document.getElementById('preloader-global');
    if (preloader) {
        preloader.style.opacity = '0';
        preloader.style.visibility = 'hidden';
        setTimeout(() => preloader.remove(), 600);
    }

    setInterval(actualizarHeatmap, 90000);
});

function gestionarBotones() {
    const btnLogin = document.getElementById('btn-login');
    const userMenu = document.getElementById('user-menu');
    const btnDashboard = document.getElementById('btn-dashboard');
    const btnNegocio = document.getElementById('btn-negocio');
    const userDisplay = document.getElementById('username-display');

    try {
        const userStr = localStorage.getItem('usuario_gocartacho');
        if (userStr) {
            const user = JSON.parse(userStr);

            if (isTokenExpired(user.token)) {
                logout();
                return;
            }

            userMenu.classList.remove('hidden');
            btnLogin.classList.add('hidden');
            btnNegocio.classList.remove('hidden');
            const usuario = user.usuario || {};
            const miniNombre = usuario.nombre ? usuario.nombre.split(' ')[0] : 'Usuario';
            userDisplay.innerHTML = miniNombre;

            const navbarAvatar = document.getElementById('navbar-avatar');
            if (navbarAvatar) {
                navbarAvatar.src = usuario.fotoUrl || 'https://cdn-icons-png.flaticon.com/512/149/149071.png';
            }

            const rol = usuario.rol ? usuario.rol.trim().toUpperCase() : "";
            if (rol === 'ADMIN') {
                btnDashboard.classList.remove('hidden');
                btnNegocio.classList.add('hidden');
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
        } else {
            btnLogin.classList.remove('hidden');
            userMenu.classList.add('hidden');
            btnDashboard.classList.add('hidden');
            btnNegocio.classList.add('hidden');
        }
    } catch (e) { console.error("Error:", e); }
}

// ===== SETTINGS LOGIC =====
function abrirModalSettings() {
    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) return;
    const user = JSON.parse(userStr).usuario;

    document.getElementById('set-nombre').value = user.nombre || '';
    document.getElementById('set-apellido').value = user.apellido || '';
    document.getElementById('set-username').value = user.username || '';
    document.getElementById('set-email').value = user.email || '';
    document.getElementById('set-email-edit').value = user.email || '';
    document.getElementById('set-foto-url').value = user.fotoUrl || '';
    document.getElementById('set-biografia').value = user.biografia || '';
    document.getElementById('set-tipo-viajero').value = user.tipoViajero || '';

    // Set avatar preview
    const previewImg = document.getElementById('profile-avatar-preview');
    if (user.fotoUrl) {
        previewImg.src = user.fotoUrl;
    } else {
        previewImg.src = 'https://cdn-icons-png.flaticon.com/512/149/149071.png';
    }

    // Set Google connection status
    const statusText = document.getElementById('oauth-google-status');
    const connectBtn = document.getElementById('btn-oauth-google');
    if (user.proveedor === 'GOOGLE') {
        statusText.textContent = 'Vinculado con Google';
        statusText.style.color = '#2b7ba0';
        statusText.style.fontWeight = 'bold';
        connectBtn.textContent = 'Desvincular';
        connectBtn.className = 'btn-action-outline';
    } else {
        statusText.textContent = 'No vinculado';
        statusText.style.color = 'var(--text-muted)';
        statusText.style.fontWeight = 'normal';
        connectBtn.textContent = 'Vincular';
        connectBtn.className = 'btn-action-outline';
    }

    // Hide verification box
    document.getElementById('verificacion-correo-container').classList.add('hidden');
    document.getElementById('set-email-code').value = '';

    document.getElementById('modal-settings').classList.remove('hidden');
}

function cerrarModalSettings() {
    document.getElementById('modal-settings').classList.add('hidden');
}

function switchTab(tabId) {
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));

    document.getElementById('tab-' + tabId).classList.add('active');

    // Add active to current click target
    if (event && event.currentTarget) {
        event.currentTarget.classList.add('active');
    } else {
        // Fallback if event is not passed
        document.querySelectorAll('.tab-btn').forEach(b => {
            if (b.getAttribute('onclick').includes(tabId)) {
                b.classList.add('active');
            }
        });
    }
}

// Preview and convert avatar to base64
function previewAndLoadAvatar(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById('profile-avatar-preview').src = e.target.result;
            // Also store base64 in URL input for sending to server
            document.getElementById('set-foto-url').value = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    }
}

// Update preview from external URL
function updateAvatarPreviewFromUrl(url) {
    const previewImg = document.getElementById('profile-avatar-preview');
    if (url.trim() !== '') {
        previewImg.src = url;
    } else {
        previewImg.src = 'https://cdn-icons-png.flaticon.com/512/149/149071.png';
    }
}

async function guardarPerfil() {
    const nombre = document.getElementById('set-nombre').value.trim();
    const apellido = document.getElementById('set-apellido').value.trim();
    const username = document.getElementById('set-username').value.trim();
    const biografia = document.getElementById('set-biografia').value.trim();
    const tipoViajero = document.getElementById('set-tipo-viajero').value;
    const fotoUrl = document.getElementById('set-foto-url').value.trim();

    if (!nombre || !apellido || !username) {
        alert('Nombre, Apellido y Nombre de Usuario son requeridos.');
        return;
    }

    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) return;
    const userObj = JSON.parse(userStr);

    try {
        const res = await fetch('/api/v1/auth/me', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + userObj.token
            },
            body: JSON.stringify({
                nombre,
                apellido,
                username,
                biografia,
                tipoViajero,
                fotoUrl
            })
        });

        if (res.ok) {
            const updatedUser = await res.json();
            userObj.usuario = updatedUser;
            localStorage.setItem('usuario_gocartacho', JSON.stringify(userObj));

            // Update UI elements
            gestionarBotones();
            alert('¡Perfil actualizado con éxito!');
            cerrarModalSettings();
        } else {
            const errorMsg = await res.text();
            alert('Error al actualizar perfil: ' + (errorMsg || 'Verifica los campos.'));
        }
    } catch (e) {
        console.error(e);
        alert('Error de conexión al servidor.');
    }
}

async function cambiarPassword() {
    const oldPass = document.getElementById('set-pass-old').value;
    const newPass = document.getElementById('set-pass-new').value;
    if (!oldPass || !newPass) return alert('Completa ambos campos');

    const userObj = JSON.parse(localStorage.getItem('usuario_gocartacho'));

    try {
        const res = await fetch('/api/v1/auth/usuarios/password', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + userObj.token
            },
            body: JSON.stringify({ oldPassword: oldPass, newPassword: newPass })
        });

        if (res.ok) {
            alert('Contraseña actualizada correctamente');
            document.getElementById('set-pass-old').value = '';
            document.getElementById('set-pass-new').value = '';
        } else {
            try {
                const err = await res.json();
                alert('Error: ' + (err.error || 'Contraseña actual incorrecta'));
            } catch (jsonErr) {
                alert('Error: Contraseña actual incorrecta');
            }
        }
    } catch (e) { alert('Error de conexión'); }
}

// Mail verification flow
function iniciarVerificacionCorreo() {
    const emailEdit = document.getElementById('set-email-edit').value.trim();
    if (!emailEdit) {
        alert('Por favor, ingresa un correo válido.');
        return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(emailEdit)) {
        alert('Por favor, ingresa un correo electrónico con formato válido.');
        return;
    }

    // Toggle verification code box
    document.getElementById('verificacion-correo-container').classList.remove('hidden');
    alert('Código de verificación de prueba enviado a ' + emailEdit + '. Use el código: 123456');
}

async function confirmarCambioCorreo() {
    const emailEdit = document.getElementById('set-email-edit').value.trim();
    const code = document.getElementById('set-email-code').value.trim();

    if (code !== '123456') {
        alert('Código de verificación inválido. Use el código 123456.');
        return;
    }

    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) return;
    const userObj = JSON.parse(userStr);

    try {
        userObj.usuario.email = emailEdit;
        localStorage.setItem('usuario_gocartacho', JSON.stringify(userObj));
        document.getElementById('set-email').value = emailEdit;
        document.getElementById('verificacion-correo-container').classList.add('hidden');
        alert('¡Correo electrónico verificado y cambiado correctamente!');
    } catch (e) {
        alert('Error al confirmar correo.');
    }
}

// OAuth connection toggle
async function toggleGoogleConnection() {
    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) return;
    const userObj = JSON.parse(userStr);

    if (userObj.usuario.proveedor === 'GOOGLE') {
        // Unlink Google account: set provider back to LOCAL
        mostrarConfirmacion('Desvincular Google', '¿Estás seguro de que deseas desvincular tu cuenta de Google? Deberás usar tu correo y contraseña para iniciar sesión.')
            .then(async confirmado => {
                if (!confirmado) return;
                try {
                    userObj.usuario.proveedor = 'LOCAL';
                    const res = await fetch('/api/v1/auth/me', {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': 'Bearer ' + userObj.token
                        },
                        body: JSON.stringify({
                            nombre: userObj.usuario.nombre,
                            apellido: userObj.usuario.apellido,
                            username: userObj.usuario.username,
                            fotoUrl: userObj.usuario.fotoUrl,
                            biografia: userObj.usuario.biografia,
                            tipoViajero: userObj.usuario.tipoViajero
                        })
                    });

                    if (res.ok) {
                        localStorage.setItem('usuario_gocartacho', JSON.stringify(userObj));
                        alert('Cuenta de Google desvinculada con éxito.');
                        abrirModalSettings();
                    } else {
                        alert('Error al desvincular la cuenta.');
                    }
                } catch (e) {
                    alert('Error de conexión.');
                }
            });
    } else {
        // Link Google: redirect to Google OAuth authorization endpoint
        mostrarConfirmacion('Vincular con Google', 'Serás redirigido a Google para autorizar y vincular tu cuenta.', 'fa-link')
            .then(confirmado => {
                if (confirmado) {
                    window.location.href = '/oauth2/authorization/google';
                }
            });
    }
}

// Delete account danger zone
async function eliminarMiCuentaPropia() {
    mostrarConfirmacion('⚠️ ADVERTENCIA CRÍTICA', '¿Estás absolutamente seguro de que deseas eliminar tu cuenta permanentemente?\n\nEsta acción es irreversible y borrará tu perfil, favoritos, reseñas y comercios creados.', 'fa-triangle-exclamation')
        .then(doubleCheck => {
            if (!doubleCheck) return;

            mostrarPrompt('Baja de Cuenta', 'Para confirmar la eliminación permanente de tu cuenta, por favor escribe la palabra: "ELIMINAR"', 'ELIMINAR')
                .then(async finalCheck => {
                    if (finalCheck !== 'ELIMINAR') return; // Cancelado o incorrecto

                    const userStr = localStorage.getItem('usuario_gocartacho');
                    if (!userStr) return;
                    const userObj = JSON.parse(userStr);

                    try {
                        const res = await fetch('/api/v1/auth/me', {
                            method: 'DELETE',
                            headers: {
                                'Authorization': 'Bearer ' + userObj.token
                            }
                        });

                        if (res.status === 204 || res.ok) {
                            alert('Su cuenta ha sido eliminada con éxito. Sentimos que te vayas.');
                            localStorage.clear();
                            window.location.href = '/';
                        } else {
                            alert('No se pudo eliminar la cuenta en este momento.');
                        }
                    } catch (e) {
                        alert('Error al comunicar con el servidor.');
                    }
                });
        });
}

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

// Decodifica el payload del JWT para verificar su fecha de expiración (exp)
function isTokenExpired(token) {
    if (!token) return true;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return (payload.exp * 1000) < Date.now();
    } catch (e) {
        return true;
    }
}

// Verifica si el token está a punto de expirar (menos de 5 minutos / 300,000 ms)
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
                userObj.token = data.token; // Actualizamos el token
                localStorage.setItem('usuario_gocartacho', JSON.stringify(userObj));
            }
        } else {
            logout(); // Si el backend rechaza la renovación, es seguro cerrar sesión
        }
    } catch (e) { console.error("Error renovando sesión silenciosamente"); }
    finally { isRefreshing = false; }
}

function limpiarMarcadoresComercios() {
    activeBusinessLayers.forEach(layer => map.removeLayer(layer));
    activeBusinessLayers = [];
    const histEl = document.getElementById('historial-afluencia');
    if (histEl) histEl.classList.add('hidden');
}

function limpiarPlan() {
    if (routingControl) {
        map.removeControl(routingControl);
        routingControl = null;
    }
    activeRouteLayers.forEach(layer => map.removeLayer(layer));
    activeRouteLayers = [];
    limpiarMarcadoresComercios();
    document.getElementById('zona-titulo').innerHTML = `<i class="fas fa-fire" style="color:var(--accent);"></i> Explora Cartagena`;
    document.getElementById('lista-comercios').innerHTML = '<div class="empty-state"><i class="fas fa-hand-pointer"></i><p>Selecciona una zona en el mapa<br>o un chip arriba para explorar</p></div>';
    zonaSeleccionada = null;
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    resetPolygonStyles();
    window.history.pushState({}, document.title, "/");
    map.setView([10.415, -75.54], 14);
}

function resetPolygonStyles() {
    Object.entries(zonaPolygons).forEach(([id, poly]) => {
        const colors = ZONA_COLORS[id] || { color: '#555', fill: '#888' };
        poly.setStyle({
            color: colors.color,
            fillColor: colors.fill,
            fillOpacity: 0.18,
            weight: 2,
            dashArray: '6, 4'
        });
        poly.openTooltip();
    });
}

function highlightPolygon(zonaId) {
    Object.entries(zonaPolygons).forEach(([id, poly]) => {
        if (parseInt(id) !== zonaId) {
            poly.setStyle({
                color: 'transparent',
                fillColor: 'transparent',
                fillOpacity: 0,
                weight: 0
            });
            poly.closeTooltip();
        } else {
            poly.openTooltip();
        }
    });
    const poly = zonaPolygons[zonaId];
    if (poly) {
        const colors = ZONA_COLORS[zonaId] || { color: '#555', fill: '#888' };
        poly.setStyle({
            color: colors.color,
            fillColor: colors.fill,
            fillOpacity: 0.42,
            weight: 3.5,
            dashArray: null
        });
        poly.bringToFront();
    }
}

function initMap() {
    if (typeof L === 'undefined') return;

    const mapBounds = [
        [10.37959238980092, -75.57132518896317], // Suroeste
        [10.435620748178579, -75.51811601279864]  // Noreste
    ];

    map = L.map('map', {
        zoomControl: false,
        attributionControl: false,
        maxBounds: mapBounds,
        maxBoundsViscosity: 0.8,
        minZoom: 13
    }).setView([10.415, -75.54], 14);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
        maxZoom: 19,
        attribution: '© CARTO © OpenStreetMap'
    }).addTo(map);

    L.control.zoom({ position: 'topright' }).addTo(map);

    // ===== DIBUJAR POLÃƒÂGONOS DE ZONA =====
    Object.keys(ZONA_POLYGONS).forEach(idStr => {
        const id = Number(idStr);
        const coords = ZONA_POLYGONS[id];
        const colors = ZONA_COLORS[id] || { color: '#555', fill: '#888', name: `Zona ${id}` };
        const zonaInfo = zonasData.find(z => z.zonaId === id);
        const nombre = zonaInfo ? zonaInfo.nombre : colors.name;
        const descripcion = zonaInfo ? (zonaInfo.descripcion || '') : '';

        const poly = L.polygon(coords, {
            color: colors.color,
            fillColor: colors.fill,
            fillOpacity: 0.18,
            weight: 2,
            dashArray: '6, 4',
            smoothFactor: 1.2
        }).addTo(map);

        poly.bindTooltip(nombre, {
            permanent: true,
            direction: 'center',
            className: 'zona-tooltip',
            interactive: false
        });

        poly.bindPopup(`
                    <div style="text-align:center;padding:6px 2px;min-width:160px;">
                        <div style="display:inline-block;width:11px;height:11px;background:${colors.fill};border-radius:2px;margin-right:6px;vertical-align:middle;border:1.5px solid ${colors.color};"></div>
                        <strong style="font-size:14px;">${nombre}</strong><br>
                        <small style="opacity:0.7;line-height:1.6;display:block;margin:4px 0;">${descripcion}</small>
                        <a href="/explorar/${id}" style="color:${colors.color};text-decoration:none;font-size:12px;font-weight:600;display:inline-block;margin-top:5px;">Ver negocios &rarr;</a>
                    </div>
                `);

        poly.on('mouseover', function () {
            if (zonaSeleccionada == null) {
                poly.setStyle({ fillOpacity: 0.30, weight: 2.5 });
            }
        });
        poly.on('mouseout', function () {
            if (zonaSeleccionada == null) {
                poly.setStyle({ fillOpacity: 0.18, weight: 2, dashArray: '6, 4' });
            }
        });

        poly.on('click', () => {
            if (zonaSeleccionada === id) {
                zonaSeleccionada = null;
                limpiarMarcadoresComercios();
                document.querySelectorAll('.chip').forEach(c => {
                    c.classList.remove('active');
                    c.style.background = '';
                    c.style.color = '';
                });
                document.getElementById('zona-titulo').innerHTML = `<i class="fas fa-fire" style="color:var(--accent);"></i> Explora Cartagena`;
                document.getElementById('lista-comercios').innerHTML = '<div class="empty-state"><i class="fas fa-hand-pointer"></i><p>Selecciona una zona en el mapa<br>o un chip arriba para explorar</p></div>';
                resetPolygonStyles();
            } else {
                zonaSeleccionada = id;
                highlightPolygon(id);
                cargarComercios(id, nombre, colors);
                activarChip(id, colors);
                map.fitBounds(poly.getBounds(), { padding: [40, 40], maxZoom: 16 });
            }
        });

        zonaPolygons[id] = poly;
    });

    construirLeyenda();
    actualizarHeatmap();
    setTimeout(() => map.invalidateSize(), 300);

    document.getElementById('btn-locate').addEventListener('click', () => {
        const btn = document.getElementById('btn-locate');
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        if ("geolocation" in navigator) {
            navigator.geolocation.getCurrentPosition(position => {
                const { latitude, longitude } = position.coords;
                map.flyTo([latitude, longitude], 15, { duration: 1.5 });
                L.circleMarker([latitude, longitude], {
                    radius: 8, fillColor: '#3498db', color: 'white', fillOpacity: 0.9, weight: 2
                }).addTo(map).bindPopup("<b>Estás aquí</b>").openPopup();
                btn.innerHTML = '<i class="fas fa-crosshairs"></i>';
            }, () => {
                btn.innerHTML = '<i class="fas fa-crosshairs"></i>';
            }, { timeout: 10000 });
        }
    });

    const urlParams = new URLSearchParams(window.location.search);
    const planId = urlParams.get('planId');
    if (planId) {
        fetch(`/api/v1/planes/${planId}/comercios`)
            .then(r => r.json())
            .then(comercios => {
                if (comercios && comercios.length > 0) {
                    const coordenadasFallback = {
                        1: [10.4240, -75.5515], 2: [10.4232, -75.5506], 3: [10.4245, -75.5498],
                        4: [10.4238, -75.5502], 5: [10.4255, -75.5518], 6: [10.4205, -75.5535],
                        7: [10.4212, -75.5528], 8: [10.4208, -75.5532], 9: [10.4215, -75.5525],
                        10: [10.3990, -75.5608], 11: [10.3975, -75.5615], 12: [10.3995, -75.5598],
                        13: [10.3960, -75.5625], 14: [10.4272, -75.5492], 15: [10.4268, -75.5488],
                        16: [10.4265, -75.5495], 17: [10.4198, -75.5478], 18: [10.4192, -75.5472],
                        19: [10.4125, -75.5425], 20: [10.4230, -75.5508]
                    };
                    const points = [];
                    comercios.forEach((c, i) => {
                        const lat = c.latitud || (coordenadasFallback[c.id] ? coordenadasFallback[c.id][0] : null);
                        const lng = c.longitud || (coordenadasFallback[c.id] ? coordenadasFallback[c.id][1] : null);
                        if (lat && lng) {
                            points.push([lat, lng]);
                            const marker = L.marker([lat, lng], {
                                icon: getCustomPinIcon()
                            }).addTo(map)
                              .bindPopup(generarPopupComercio(c, `${i + 1}.`))
                              .on('click', () => map.flyTo([lat, lng], 16, { duration: 0.8 }));
                            activeRouteLayers.push(marker);
                        }
                    });
                    if (points.length > 1) {
                        const routeLine = L.polyline(points, { color: 'var(--accent2)', weight: 4, opacity: 0.8, dashArray: '10, 10' }).addTo(map);
                        activeRouteLayers.push(routeLine);
                        map.fitBounds(routeLine.getBounds(), { padding: [50, 50] });
                    }
                    document.getElementById('zona-titulo').innerHTML = `<i class="fas fa-route" style="color:var(--accent2);"></i> Plan Seleccionado
                                <button id="btn-limpiar-plan" onclick="limpiarPlan()" title="Limpiar plan"><i class="fas fa-times"></i></button>`;

                    let itineraryHtml = `
                    <div class="itinerary-container">
                        <div class="itinerary-header">
                            <span class="itinerary-title"><i class="fas fa-compass"></i> Itinerario del Plan</span>
                        </div>
                        <div class="itinerary-timeline">
                    `;

                    comercios.forEach((c, i) => {
                        const lat = c.latitud || (coordenadasFallback[c.id] ? coordenadasFallback[c.id][0] : null);
                        const lng = c.longitud || (coordenadasFallback[c.id] ? coordenadasFallback[c.id][1] : null);

                        let starsHtml = '';
                        const val = c.promedioCalificacion || 0;
                        for (let s = 1; s <= 5; s++) {
                            if (s <= Math.round(val)) {
                                starsHtml += '<i class="fas fa-star"></i>';
                            } else {
                                starsHtml += '<i class="far fa-star"></i>';
                            }
                        }

                        const imgUrl = c.imagenUrl || `https://picsum.photos/seed/${c.nombre.replace(/\s+/g, '')}/400/220`;
                        const typeClass = `badge-${c.tipoNegocio || 'Otro'}`;
                        const typeName = c.tipoNegocio || 'Otro';

                        itineraryHtml += `
                        <div class="itinerary-step">
                            <div class="step-marker">${i + 1}</div>
                            <div class="itinerary-card" onclick="map.flyTo([${lat}, ${lng}], 16, { duration: 1.2 })" style="cursor:pointer;">
                                <img src="${imgUrl}" alt="${c.nombre}" class="itinerary-card-img" onerror="this.src='https://picsum.photos/400/220'">
                                <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
                                    <h4 style="margin:0; font-family:var(--font-serif); font-size:1.1rem; font-weight:700;">${c.nombre}</h4>
                                    <span class="badge-tipo ${typeClass}" style="flex-shrink:0;">${typeName}</span>
                                </div>
                                
                                <div class="itinerary-meta" style="margin-bottom:8px;">
                                    <span class="stars" style="color:var(--gold); font-size:0.8rem;">${starsHtml}</span>
                                    <span class="rating-value" style="font-weight:700; font-size:0.8rem; margin-left:4px;">${val.toFixed(1)}</span>
                                    <span class="total-reviews" style="font-size:0.75rem; color:var(--text-muted);">(${c.totalResenas || 0} reseñas)</span>
                                </div>

                                <div class="itinerary-tip">
                                    <i class="fas fa-compass"></i>
                                    <span>${c.recomendacion || 'Explora y disfruta del gran ambiente de este establecimiento.'}</span>
                                </div>

                                <div class="comercio-footer" onclick="event.stopPropagation();" style="margin-top:12px; padding-top:10px; border-top:1px solid rgba(196,113,74,0.08);">
                                    <div class="comercio-actions" style="display:flex; justify-content:space-between; align-items:center;">
                                        <button class="btn-ver-resenas" onclick="abrirModalResenas('${c.id}', '${c.nombre.replace(/'/g, "\\'")}')" style="font-size:0.75rem; padding:6px 12px;">
                                            <i class="fas fa-comments"></i> Opiniones
                                        </button>
                                        <div style="display:flex; gap:8px;">
                                            ${c.telefono ? `
                                            <a href="https://wa.me/${c.telefono.replace(/\+/g, '').replace(/\s+/g, '')}" target="_blank" class="btn-action-circle wa" title="Escribir por WhatsApp" style="width:30px; height:30px; font-size:0.9rem;">
                                                <i class="fab fa-whatsapp"></i>
                                            </a>
                                            ` : ''}
                                            <button class="btn-action-circle map" onclick="trazarPlanDirecta(${lat}, ${lng}, '${c.nombre.replace(/'/g, "\\'")}')" title="¿Cómo llegar?" style="width:30px; height:30px; font-size:0.9rem;">
                                                <i class="fas fa-directions"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        `;
                    });

                    itineraryHtml += `
                        </div>
                    </div>
                    `;

                    document.getElementById('lista-comercios').innerHTML = itineraryHtml;
                }
            });
    }

    const latParam = urlParams.get('lat');
    const lngParam = urlParams.get('lng');
    const comercioIdParam = urlParams.get('comercioId');
    const zonaNumeroParam = urlParams.get('zonaNumero');

    if (zonaNumeroParam && zonaNumeroParam !== 'null') {
        const zonaNo = parseInt(zonaNumeroParam);
        const colors = ZONA_COLORS[zonaNo] || { color: '#555', fill: '#888', name: `Zona ${zonaNo}` };
        const zonaInfo = zonasData.find(z => z.numero === zonaNo || z.zonaId === zonaNumeroParam);
        const nombre = zonaInfo ? zonaInfo.nombre : colors.name;

        zonaSeleccionada = zonaNo;
        highlightPolygon(zonaNo);
        activarChip(zonaNo, colors);

        if (comercioIdParam) {
            highlightComercioId = comercioIdParam;
        }

        if (latParam && lngParam) {
            const targetLat = parseFloat(latParam);
            const targetLng = parseFloat(lngParam);
            setTimeout(() => {
                map.flyTo([targetLat, targetLng], 18, { duration: 1.5 });
            }, 500);
        } else {
            const poly = zonaPolygons[zonaNo];
            if (poly) {
                map.fitBounds(poly.getBounds(), { padding: [40, 40], maxZoom: 16 });
            }
        }

        cargarComercios(zonaNo, nombre, colors);

        // Clean parameters from URL to maintain a clean experience
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
}

function construirLeyenda() {
    const legend = document.getElementById('zona-legend');
    const h4 = legend.querySelector('h4');
    legend.innerHTML = '';
    legend.appendChild(h4);

    Object.keys(ZONA_POLYGONS).forEach(idStr => {
        const id = Number(idStr);
        const colors = ZONA_COLORS[id];
        if (!colors) return;
        const zonaInfo = zonasData.find(z => z.zonaId === id);
        const nombre = zonaInfo ? zonaInfo.nombre : colors.name;

        const item = document.createElement('div');
        item.className = 'legend-item';
        item.title = `Ir a: ${nombre}`;
        item.innerHTML = `
                    <div class="legend-dot" style="background:${colors.fill};border-color:${colors.color};"></div>
                    <span class="legend-label">${nombre}</span>
                `;
        item.addEventListener('click', () => {
            const poly = zonaPolygons[id];
            if (poly) {
                zonaSeleccionada = id;
                highlightPolygon(id);
                map.fitBounds(poly.getBounds(), { padding: [40, 40], maxZoom: 16 });
                cargarComercios(id, nombre, colors);
                activarChip(id, colors);
            }
        });
        legend.appendChild(item);
    });
}

function actualizarHeatmap() {
    fetch('/api/v1/heatmap/realtime')
        .then(r => { if (!r.ok) throw new Error('Heatmap no disponible'); return r.json(); })
        .then(puntos => {
            if (heatLayer) map.removeLayer(heatLayer);
            if (puntos && puntos.length > 0) {
                // Encontrar la cuadrícula con mayor concentración de usuarios para ser el tope (rojo)
                // Se usa p.intensidad (el nombre que envía Java) y fijamos un tope mínimo
                // de 15 personas. Así, 1 sola persona no pintará el mapa de rojo intenso.
                const maxPuntos = Math.max(...puntos.map(p => p.intensidad || p.intensity || 1));
                const maxIntensity = Math.max(maxPuntos, 15);

                const heatData = puntos.map(p => {
                    const lat = p.lat !== undefined ? p.lat : p.latitud;
                    const lng = p.lng !== undefined ? p.lng : p.longitud;
                    return [lat, lng, (p.intensidad || p.intensity || 1) / maxIntensity];
                }).filter(arr => arr[0] !== undefined && arr[1] !== undefined);

                heatLayer = L.heatLayer(heatData, {
                    radius: 35, // Aumentado para que las cuadrículas de 100m se fusionen suavemente
                    blur: 25,
                    maxZoom: 17,
                    max: 1.0,
                    minOpacity: 0.35,
                    gradient: {
                        0.05: 'rgba(0, 119, 182, 0.45)', // Azul suave visible desde 1 sola persona (1/15 = 0.06)
                        0.25: 'rgba(42, 157, 143, 0.60)',
                        0.5: 'rgba(233, 196, 106, 0.75)',
                        0.75: 'rgba(244, 162, 97, 0.85)',
                        1.0: 'rgba(230, 57, 70, 0.95)'
                    }
                }).addTo(map);
            }
        })
        .catch(e => console.warn("Heatmap no disponible:", e.message));
}

function generarChipsZonas() {
    const container = document.getElementById('zona-chips');
    const idsDeOrden = Object.keys(ZONA_POLYGONS).map(Number);

    idsDeOrden.forEach(id => {
        const colors = ZONA_COLORS[id] || { color: '#555', fill: '#888', name: `Zona ${id}` };
        const zonaInfo = zonasData.find(z => z.zonaId === id);
        const nombre = zonaInfo ? zonaInfo.nombre : colors.name;
        crearChip(container, id, nombre, colors);
    });
}

function crearChip(container, zonaId, nombre, colors) {
    const chip = document.createElement('span');
    chip.className = 'chip';
    chip.dataset.zonaId = zonaId;
    chip.textContent = nombre;
    chip.style.borderColor = colors.fill;

    chip.addEventListener('click', () => {
        if (zonaSeleccionada === zonaId) {
            zonaSeleccionada = null;
            limpiarMarcadoresComercios();
            document.querySelectorAll('.chip').forEach(c => {
                c.classList.remove('active');
                c.style.background = '';
                c.style.color = '';
            });
            document.getElementById('zona-titulo').innerHTML = `<i class="fas fa-fire" style="color:var(--accent);"></i> Explora Cartagena`;
            document.getElementById('lista-comercios').innerHTML = '<div class="empty-state"><i class="fas fa-hand-pointer"></i><p>Selecciona una zona en el mapa<br>o un chip arriba para explorar</p></div>';
            resetPolygonStyles();
        } else {
            zonaSeleccionada = zonaId;
            highlightPolygon(zonaId);
            cargarComercios(zonaId, nombre, colors);
            activarChip(zonaId, colors);
            const poly = zonaPolygons[zonaId];
            if (poly) map.fitBounds(poly.getBounds(), { padding: [40, 40], maxZoom: 16 });
        }
    });
    container.appendChild(chip);
}

function activarChip(zonaId, colors) {
    document.querySelectorAll('.chip').forEach(c => {
        c.classList.remove('active');
        c.style.background = '';
        c.style.color = '';
    });
    const chip = document.querySelector(`.chip[data-zona-id="${zonaId}"]`);
    if (chip && colors) {
        chip.classList.add('active');
        chip.style.background = colors.fill;
        chip.style.color = 'white';
        chip.style.borderColor = colors.color;
    }
}

function configurarFiltros() {
    document.getElementById('filtro-tipo').addEventListener('change', () => {
        if (zonaSeleccionada !== null) {
            const colors = ZONA_COLORS[zonaSeleccionada] || {};
            const zonaInfo = zonasData.find(z => z.zonaId === zonaSeleccionada);
            const nombre = zonaInfo ? zonaInfo.nombre : (colors.name || '');
            cargarComercios(zonaSeleccionada, nombre, colors);
        }
    });
    document.getElementById('filtro-hora').addEventListener('change', () => {
        if (zonaSeleccionada !== null) {
            const colors = ZONA_COLORS[zonaSeleccionada] || {};
            const zonaInfo = zonasData.find(z => z.zonaId === zonaSeleccionada);
            const nombre = zonaInfo ? zonaInfo.nombre : (colors.name || '');
            cargarComercios(zonaSeleccionada, nombre, colors);
        }
    });
}

function cargarComercios(id, nombre, colors) {
    const accentColor = (colors && colors.fill) ? colors.fill : 'var(--accent2)';
    const borderColor = (colors && colors.color) ? colors.color : 'var(--accent2)';

    const titulo = document.getElementById('zona-titulo');
    titulo.innerHTML = `<i class="fas fa-map-marker-alt" style="color:${accentColor};"></i> ${nombre}`;

    const div = document.getElementById('lista-comercios');
    div.innerHTML = '<div class="empty-state"><i class="fas fa-spinner fa-spin"></i><p>Cargando negocios...</p></div>';

    const tipoId = document.getElementById('filtro-tipo').value;

    // BUG FIX #1: resolver el ObjectId de MongoDB a partir del número de zona.
    // El polígono usa el número entero (1, 2, 3...) pero la API espera el ObjectId.
    const zonaInfo = zonasData.find(z => z.numero === id || z.zonaId === id || z.numero === Number(id));
    const zonaIdReal = zonaInfo ? zonaInfo.zonaId : id; // Fallback al id original si no se encuentra


    let url = `/api/v1/comercios/zona/${zonaIdReal}`;
    if (tipoId) url += `?tipoId=${tipoId}`;

    fetch(url)
        .then(r => r.json())
        .then(async data => {
            limpiarMarcadoresComercios();
            div.innerHTML = '';
            if (!data || data.length === 0) {
                div.innerHTML = '<div class="empty-state"><i class="fas fa-store-slash"></i><p>No hay negocios en esta zona con los filtros aplicados</p></div>';
                return;
            }

            let promos = [];
            try {
                const promoRes = await fetch(`/api/v1/promociones/zona/${id}`);
                promos = await promoRes.json();
            } catch (e) { }

            data.forEach(c => {
                const tipoClass = c.tipoNegocioNombre || c.tipoNegocio || 'Otro';
                const comercioPromos = promos.filter(p => p.comercio && p.comercio.comercioId === c.comercioId);
                const promoHTML = comercioPromos.length > 0
                    ? `<span class="promo-badge"><i class="fas fa-tag"></i> ${comercioPromos[0].titulo}</span>`
                    : '';
                const horario = c.horarioApertura && c.horarioCierre
                    ? `${formatHora(c.horarioApertura)} - ${formatHora(c.horarioCierre)}`
                    : '';

                const card = document.createElement('div');
                card.className = 'comercio-card';
                card.style.cssText += `--card-border: ${accentColor};`;
                card.style.setProperty('--accent2', accentColor);

                const indexImg = Math.abs(c.nombre.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)) % 11;
                const listImgs = [
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
                const cardImg = document.createElement('img');
                cardImg.src = listImgs[indexImg];
                cardImg.alt = c.nombre;
                cardImg.className = 'comercio-card-img';
                cardImg.style.cssText = 'width: 100%; height: 110px; object-fit: cover; border-radius: 8px; margin-bottom: 10px;';
                cardImg.onerror = () => { cardImg.style.display = 'none'; };
                card.appendChild(cardImg);

                let marker = null;
                if (c.latitud && c.longitud) {
                    marker = L.marker([c.latitud, c.longitud], {
                        icon: getCustomPinIcon()
                    }).addTo(map)
                      .bindPopup(generarPopupComercio(c))
                      .on('click', () => map.flyTo([c.latitud, c.longitud], 16, { duration: 0.8 }));
                    activeBusinessLayers.push(marker);

                    if (highlightComercioId && c.comercioId === highlightComercioId) {
                        card.style.border = `2px solid ${accentColor}`;
                        card.style.boxShadow = `0 0 15px ${accentColor}`;
                        setTimeout(() => {
                            card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        }, 600);
                        setTimeout(() => {
                            marker.openPopup();
                        }, 1500);
                        highlightComercioId = null;
                    }
                }

                card.addEventListener('click', () => {
                    if (c.latitud && c.longitud) {
                        map.flyTo([c.latitud, c.longitud], 16, { duration: 1.2 });
                        if (marker) setTimeout(() => marker.openPopup(), 300);
                    }
                });

                const h4 = document.createElement('h4');
                h4.textContent = c.nombre;
                card.appendChild(h4);

                // Valoración con estrellas
                const ratingDiv = document.createElement('div');
                ratingDiv.className = 'comercio-rating';
                const promedio = c.promedioCalificacion || 0;
                const totalRes = c.totalResenas || 0;

                let starsHTML = '';
                for (let i = 1; i <= 5; i++) {
                    if (i <= Math.floor(promedio)) starsHTML += '<i class="fas fa-star"></i>';
                    else if (i - 0.5 <= promedio) starsHTML += '<i class="fas fa-star-half-alt"></i>';
                    else starsHTML += '<i class="far fa-star"></i>';
                }

                ratingDiv.innerHTML = `
                            <span class="stars">${starsHTML}</span>
                            <span class="rating-value">${promedio.toFixed(1)}</span>
                            <span class="total-reviews">(${totalRes} ${totalRes === 1 ? 'reseña' : 'reseñas'})</span>
                        `;
                card.appendChild(ratingDiv);

                const metaDiv = document.createElement('div');
                metaDiv.className = 'comercio-meta';
                metaDiv.innerHTML = `<span class="badge-tipo badge-${tipoClass}">${tipoClass}</span>` +
                    (horario ? `<span><i class="fas fa-clock"></i> ${horario}</span>` : '');
                if (c.direccion) {
                    const dirSpan = document.createElement('span');
                    dirSpan.innerHTML = `<i class="fas fa-map-pin"></i> `;
                    dirSpan.appendChild(document.createTextNode(c.direccion));
                    metaDiv.appendChild(dirSpan);
                }
                card.appendChild(metaDiv);

                if (c.descripcion) {
                    const descP = document.createElement('p');
                    descP.className = 'comercio-description';
                    descP.textContent = c.descripcion;
                    card.appendChild(descP);
                }

                const footDiv = document.createElement('div');
                footDiv.className = 'comercio-footer';

                const actionsDiv = document.createElement('div');
                actionsDiv.className = 'comercio-actions';

                const leftActions = document.createElement('div');
                leftActions.style.cssText = 'display: flex; gap: 8px;';

                // Botón de WhatsApp
                if (c.telefono) {
                    const btnWA = document.createElement('a');
                    const cleanTel = c.telefono.replace(/\+/g, '').replace(/\s/g, '');
                    btnWA.href = `https://wa.me/${cleanTel}?text=Hola%20${encodeURIComponent(c.nombre)},%20te%20vi%20en%20GO%20CARTACHO!`;
                    btnWA.target = '_blank';
                    btnWA.className = 'btn-action-circle wa';
                    btnWA.innerHTML = '<i class="fab fa-whatsapp"></i>';
                    btnWA.onclick = (e) => e.stopPropagation();
                    leftActions.appendChild(btnWA);
                }

                // Botón de Ruta
                if (c.latitud && c.longitud) {
                    const btnMap = document.createElement('button');
                    btnMap.className = 'btn-action-circle map';
                    btnMap.innerHTML = '<i class="fas fa-route"></i>';
                    btnMap.title = "Cómo llegar";
                    btnMap.onclick = (e) => {
                        e.stopPropagation();
                        trazarPlanDirecta(c.latitud, c.longitud, c.nombre);
                    };
                    leftActions.appendChild(btnMap);
                }

                actionsDiv.appendChild(leftActions);

                const btnResenas = document.createElement('button');
                btnResenas.className = 'btn-ver-resenas';
                btnResenas.innerHTML = '<i class="fas fa-comments"></i> Ver reseñas';
                btnResenas.onclick = (e) => {
                    e.stopPropagation();
                    abrirModalResenas(c.comercioId, c.nombre);
                };
                actionsDiv.appendChild(btnResenas);

                if (promoHTML) {
                    const pDiv = document.createElement('div');
                    pDiv.innerHTML = promoHTML;
                    footDiv.appendChild(pDiv);
                }

                footDiv.appendChild(actionsDiv);
                card.appendChild(footDiv);
                div.appendChild(card);
            });
        })
        .catch(() => {
            div.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><p>Error al cargar negocios</p></div>';
        });
}

// ===== MODAL DE RESEÑAS Y REPORTES =====
let currentComercioId = null;
let selectedStars = 0;

function abrirModalResenas(comercioId, nombreComercio) {
    currentComercioId = comercioId;
    selectedStars = 0;
    resetStars();

    const modal = document.getElementById('modal-resenas');
    const titulo = document.getElementById('modal-resenas-titulo');
    const lista = document.getElementById('modal-resenas-lista');
    const form = document.getElementById('form-nueva-resena');
    const msgLogin = document.getElementById('msg-login-resena');

    titulo.textContent = `Reseñas de ${nombreComercio}`;
    lista.innerHTML = '<p style="text-align:center; padding:20px;"><i class="fas fa-spinner fa-spin"></i> Cargando opiniones...</p>';

    // Check login
    const userStr = localStorage.getItem('usuario_gocartacho');
    if (userStr) {
        form.classList.remove('hidden');
        msgLogin.classList.add('hidden');
    } else {
        form.classList.add('hidden');
        msgLogin.classList.remove('hidden');
    }

    modal.classList.remove('hidden');

    fetch(`/api/v1/comercios/${comercioId}/resenas`)
        .then(r => r.json())
        .then(data => {
            const resenas = data.content || [];
            if (resenas.length === 0) {
                lista.innerHTML = '<p style="text-align:center; padding:20px; color:var(--muted);">Este negocio aún no tiene reseñas. ¡Sé el primero!</p>';
                return;
            }

            lista.innerHTML = '';
            resenas.forEach(res => {
                const item = document.createElement('div');
                item.style.cssText = 'padding:15px; border-bottom:1px solid var(--border); margin-bottom:10px; position:relative;';

                const estrellas = '⭐'.repeat(res.calificacion);

                item.innerHTML = `
                            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                                <div style="color:var(--gold); font-size:0.8rem; margin-bottom:5px;">${estrellas}</div>
                                <button onclick="reportarResena('${res.resenaId}')" style="background:transparent; border:none; color:var(--muted); cursor:pointer; font-size:0.8rem;" title="Reportar comentario inapropiado">
                                    <i class="fas fa-flag"></i>
                                </button>
                            </div>
                            <p style="font-size:0.9rem; line-height:1.4;">${escapeHtml(res.comentario)}</p>
                            <small style="opacity:0.6; font-size:0.7rem;">${new Date(res.fecha).toLocaleDateString()}</small>
                        `;
                lista.appendChild(item);
            });
        })
        .catch(() => {
            lista.innerHTML = '<p style="color:var(--accent); text-align:center;">Error al cargar las reseñas.</p>';
        });
}

// Lógica de estrellas
document.querySelectorAll('.star-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        selectedStars = parseInt(btn.dataset.val);
        updateStarsUI();
    });
});

function updateStarsUI() {
    document.querySelectorAll('.star-btn').forEach(btn => {
        const val = parseInt(btn.dataset.val);
        if (val <= selectedStars) {
            btn.classList.remove('far');
            btn.classList.add('fas');
        } else {
            btn.classList.remove('fas');
            btn.classList.add('far');
        }
    });
}

function resetStars() {
    selectedStars = 0;
    updateStarsUI();
    document.getElementById('resena-comentario').value = '';
}

let enviandoResena = false; // Bandera para prevenir múltiples clics

function enviarResena() {
    if (enviandoResena) return; // Si ya se está enviando, ignorar clics adicionales

    if (selectedStars === 0) { alert('Selecciona una calificación (estrellas).'); return; }
    const comentario = document.getElementById('resena-comentario').value;

    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) { alert('Debes iniciar sesión.'); return; }
    const user = JSON.parse(userStr);

    const body = {
        comercioId: currentComercioId,
        calificacion: selectedStars,
        comentario: comentario
    };

    enviandoResena = true; // Bloqueamos la función

    fetch('/api/v1/resenas', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + user.token
        },
        body: JSON.stringify(body)
    })
        .then(async r => {
            if (!r.ok) {
                const txt = await r.text();
                throw new Error(txt || 'Error al enviar reseña');
            }
            return r.json();
        })
        .then(() => {
            alert('¡Reseña enviada con éxito!');
            abrirModalResenas(currentComercioId, document.getElementById('modal-resenas-titulo').textContent.replace('Reseñas de ', ''));

            // Recargar los comercios en el mapa para actualizar el promedio visible y total de reseñas
            if (zonaSeleccionada !== null) {
                const colors = ZONA_COLORS[zonaSeleccionada] || {};
                const zonaInfo = zonasData.find(z => z.zonaId === zonaSeleccionada);
                const nombre = zonaInfo ? zonaInfo.nombre : (colors.name || '');
                cargarComercios(zonaSeleccionada, nombre, colors);
            }
        })
        .catch(err => alert(err.message))
        .finally(() => {
            enviandoResena = false; // Liberamos el bloqueo sin importar si falló o fue exitoso
        });
}

function cerrarModalResenas() {
    document.getElementById('modal-resenas').classList.add('hidden');
}

function reportarResena(resenaId) {
    const userStr = localStorage.getItem('usuario_gocartacho');
    if (!userStr) { alert('Debes iniciar sesión para reportar un comentario.'); return; }
    const user = JSON.parse(userStr);

    mostrarConfirmacion('Reportar Comentario', '¿Deseas reportar este comentario por ser inapropiado o falso? El administrador del mapa lo revisará de inmediato.', 'fa-flag')
        .then(confirmado => {
            if (!confirmado) return;

            fetch(`/api/v1/resenas/${resenaId}/reportar`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + user.token
                }
            })
                .then(r => r.json())
                .then(data => {
                    if (data.status === 'success') {
                        alert('Gracias por tu reporte. El administrador revisará el comentario pronto.');
                    } else if (data.error) {
                        alert(data.error);
                    }
                })
                .catch(() => alert('Hubo un error al enviar el reporte.'));
        });
}

function trazarPlanDirecta(destLat, destLng, nombreDest) {
    if (!("geolocation" in navigator)) {
        alert("Tu navegador no soporta geolocalización.");
        return;
    }

    // Mostrar estado de carga
    const titulo = document.getElementById('zona-titulo');
    const originalTitulo = titulo.innerHTML;
    titulo.innerHTML = `<i class="fas fa-spinner fa-spin"></i> Obteniendo tu ubicación...`;

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const userLat = position.coords.latitude;
            const userLng = position.coords.longitude;

            // Limpiar plan previa si existe
            if (routingControl) {
                map.removeControl(routingControl);
            }

            titulo.innerHTML = `<i class="fas fa-route" style="color:var(--accent2);"></i> Plan a ${nombreDest}
                        <button id="btn-limpiar-plan" onclick="limpiarPlan()" title="Limpiar plan"><i class="fas fa-times"></i></button>`;

            routingControl = L.Routing.control({
                waypoints: [
                    L.latLng(userLat, userLng),
                    L.latLng(destLat, destLng)
                ],
                lineOptions: {
                    styles: [{ color: 'var(--accent2)', weight: 5, opacity: 0.7 }]
                },
                routeWhileDragging: false,
                addWaypoints: false,
                draggableWaypoints: false,
                fitSelectedRoutes: true,
                showAlternatives: false,
                createMarker: function (i, wp, nWps) {
                    if (i === 0) {
                        return L.marker(wp.latLng, {
                            icon: L.divIcon({
                                className: 'route-marker-user',
                                html: '<i class="fas fa-user-pin" style="color:var(--accent2); font-size:1.5rem;"></i>',
                                iconSize: [30, 30],
                                iconAnchor: [15, 30]
                            })
                        }).bindPopup("Tu ubicación");
                    } else {
                        return L.marker(wp.latLng, {
                            icon: L.divIcon({
                                className: 'route-marker-dest',
                                html: '<i class="fas fa-map-marker-alt" style="color:var(--accent); font-size:1.5rem;"></i>',
                                iconSize: [30, 30],
                                iconAnchor: [15, 30]
                            })
                        }).bindPopup(nombreDest);
                    }
                }
            }).addTo(map);

            // Pequeño hack para asegurar que el mapa se ajuste
            setTimeout(() => map.invalidateSize(), 500);
        },
        (error) => {
            titulo.innerHTML = originalTitulo;
            if (error.code === error.PERMISSION_DENIED) {
                alert("Por favor, activa el acceso a tu ubicación para poder mostrarte el camino al negocio.");
            } else {
                alert("No pudimos obtener tu ubicación. Por favor, verifica tu conexión o configuración de GPS.");
            }
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
    );
}

function escapeHtml(str) {
    if (!str) return '';
    const p = document.createElement('p');
    p.textContent = str;
    return p.innerHTML;
}

// Genera la estructura HTML premium para el popup interactivo del comercio en el mapa
function generarPopupComercio(c, prefix) {
    const tipoClass = c.tipoNegocioNombre || c.tipoNegocio || 'Otro';
    const promedio = c.promedioCalificacion || 0;
    const totalRes = c.totalResenas || 0;

    let starsHTML = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= Math.floor(promedio)) {
            starsHTML += '<i class="fas fa-star" style="margin-right: 2px;"></i>';
        } else if (i - 0.5 <= promedio) {
            starsHTML += '<i class="fas fa-star-half-alt" style="margin-right: 2px;"></i>';
        } else {
            starsHTML += '<i class="far fa-star" style="opacity: 0.3; margin-right: 2px;"></i>';
        }
    }

    const indexImg = Math.abs((c.nombre || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)) % 11;
    const listImgs = [
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
    const cardImg = listImgs[indexImg];

    const horarioHTML = c.horarioApertura && c.horarioCierre
        ? `<span><i class="fas fa-clock" style="color: var(--turquesa-caribe);"></i> ${formatHora(c.horarioApertura)} - ${formatHora(c.horarioCierre)}</span>`
        : '';

    const cleanTel = c.telefono ? c.telefono.replace(/\+/g, '').replace(/\s/g, '') : '';
    const waLink = c.telefono ? `https://wa.me/${cleanTel}?text=Hola%20${encodeURIComponent(c.nombre)},%20te%20vi%20en%20GO%20CARTACHO!` : '';

    return `
        <div class="custom-map-popup">
            <div class="popup-image-container" style="background-image: url('${cardImg}');">
                <span class="popup-badge-tipo">${escapeHtml(tipoClass)}</span>
            </div>
            <div class="popup-details">
                <h4 class="popup-title">${prefix ? `${prefix} ` : ''}${escapeHtml(c.nombre)}</h4>
                <div class="popup-rating">
                    <span class="popup-stars">${starsHTML}</span>
                    <span class="popup-rating-value">${promedio.toFixed(1)}</span>
                    <span class="popup-total-reviews">(${totalRes} ${totalRes === 1 ? 'reseña' : 'reseñas'})</span>
                </div>
                ${c.descripcion ? `<p class="popup-description">${escapeHtml(c.descripcion)}</p>` : ''}
                <div class="popup-meta">
                    ${c.direccion ? `<span><i class="fas fa-map-marker-alt" style="color: var(--naranja-ciudad);"></i> ${escapeHtml(c.direccion)}</span>` : ''}
                    ${horarioHTML}
                </div>
                <div class="popup-actions">
                    <button class="popup-btn-reviews" onclick="abrirModalResenas('${c.comercioId || c.id}', '${escapeHtml(c.nombre).replace(/'/g, "\\'")}')">
                        <i class="fas fa-comments"></i> Reseñas
                    </button>
                    <div class="popup-btn-group">
                        ${c.telefono ? `
                        <a href="${waLink}" target="_blank" class="popup-action-circle wa" title="Escribir por WhatsApp">
                            <i class="fab fa-whatsapp"></i>
                        </a>` : ''}
                        ${c.latitud && c.longitud ? `
                        <button class="popup-action-circle route" onclick="trazarPlanDirecta(${c.latitud}, ${c.longitud}, '${escapeHtml(c.nombre).replace(/'/g, "\\'")}')" title="Cómo llegar">
                            <i class="fas fa-route"></i>
                        </button>` : ''}
                    </div>
                </div>
            </div>
        </div>
    `;
}

let searchTimeout;
function buscarComerciosGlobal(nombreBusqueda) {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        if (!nombreBusqueda || nombreBusqueda.trim() === '') {
            if (zonaSeleccionada) {
                const colors = ZONA_COLORS[zonaSeleccionada] || {};
                const zonaInfo = zonasData.find(z => z.zonaId === zonaSeleccionada);
                cargarComercios(zonaSeleccionada, zonaInfo ? zonaInfo.nombre : '', colors);
            } else {
                document.getElementById('lista-comercios').innerHTML = '<div class="empty-state"><i class="fas fa-hand-pointer"></i><p>Selecciona una zona en el mapa<br>o un chip arriba para explorar</p></div>';
                limpiarMarcadoresComercios();
                document.getElementById('zona-titulo').innerHTML = `<i class="fas fa-archway" style="color:var(--accent); opacity: 0.8;"></i> La Heroica`;
            }
            return;
        }

        const div = document.getElementById('lista-comercios');
        div.innerHTML = '<div class="empty-state"><i class="fas fa-spinner fa-spin"></i><p>Buscando negocios...</p></div>';
        document.getElementById('zona-titulo').innerHTML = `<i class="fas fa-search" style="color:var(--primary-blue);"></i> Resultados de "${escapeHtml(nombreBusqueda)}"`;

        // Deseleccionar zona visualmente si existía
        zonaSeleccionada = null;
        document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));

        fetch(`/api/v1/comercios?nombre=${encodeURIComponent(nombreBusqueda.trim())}&size=50`)
            .then(r => r.json())
            .then(async pageData => {
                limpiarMarcadoresComercios();
                div.innerHTML = '';
                const data = pageData.content || pageData;
                if (!data || data.length === 0) {
                    div.innerHTML = '<div class="empty-state"><i class="fas fa-store-slash"></i><p>No se encontraron negocios con ese nombre</p></div>';
                    return;
                }

                const accentColor = 'var(--primary-blue)';
                data.forEach(c => {
                    const tipoClass = c.tipoNegocioNombre || c.tipoNegocio || 'Otro';
                    const card = document.createElement('div');
                    card.className = 'comercio-card';
                    card.style.cssText += `--card-border: ${accentColor};`;
                    card.style.setProperty('--accent2', accentColor);

                    const indexImg = Math.abs((c.nombre || '').split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)) % 11;
                    const listImgs = [
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

                    card.innerHTML = `
                        <img src="${listImgs[indexImg]}" alt="${escapeHtml(c.nombre)}" class="comercio-card-img" style="width: 100%; height: 110px; object-fit: cover; border-radius: 8px; margin-bottom: 10px;" onerror="this.style.display='none'">
                        <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                            <div>
                                <h4 style="margin:0 0 5px 0; font-size:1.1rem; color:var(--text-primary); font-family:var(--font-heading);">${escapeHtml(c.nombre)}</h4>
                                <span class="status-badge" style="background:rgba(0,0,0,0.05); color:var(--text-secondary); margin-bottom:8px; display:inline-block;">${escapeHtml(tipoClass)}</span>
                                <div style="font-size:0.85rem; color:var(--text-secondary); display:flex; align-items:center; gap:5px;">
                                    <i class="fas fa-map-marker-alt" style="color:${accentColor}"></i> ${escapeHtml(c.zonaNombre || 'Cartagena')}
                                </div>
                            </div>
                        </div>
                    `;
                    card.onclick = () => {
                        if (c.latitud && c.longitud) {
                            map.flyTo([c.latitud, c.longitud], 17);
                        }
                    };
                    div.appendChild(card);

                    if (c.latitud && c.longitud) {
                        let marker = L.marker([c.latitud, c.longitud], {
                            icon: getCustomPinIcon()
                        }).addTo(map)
                          .bindPopup(generarPopupComercio(c))
                          .on('click', () => map.flyTo([c.latitud, c.longitud], 16, { duration: 0.8 }));
                        activeBusinessLayers.push(marker);
                    }
                });
            })
            .catch(() => {
                div.innerHTML = '<div class="empty-state"><i class="fas fa-exclamation-triangle"></i><p>Error en la búsqueda</p></div>';
            });
    }, 400); // 400ms debounce
}

// ===== DIÁLOGOS GLOBALES TEMATIZADOS (CONFIRM / PROMPT) =====
let currentConfirmResolver = null;
let currentPromptResolver = null;

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

function mostrarPrompt(titulo, mensaje, valorObligatorio = null) {
    return new Promise((resolve) => {
        const modal = document.getElementById('promptModal');
        const titEl = document.getElementById('promptTitulo');
        const msgEl = document.getElementById('promptMensaje');
        const inputEl = document.getElementById('promptInput');
        const btnConfirm = document.getElementById('btnAceptarPrompt');

        if (!modal) {
            resolve(prompt(mensaje));
            return;
        }

        if (titEl) titEl.textContent = titulo;
        if (msgEl) msgEl.textContent = mensaje;
        if (inputEl) {
            inputEl.value = '';
            inputEl.placeholder = valorObligatorio ? `Escribe "${valorObligatorio}"...` : 'Escribe aquí...';
        }

        modal.classList.remove('hidden');

        if (currentPromptResolver) {
            currentPromptResolver(null);
        }
        currentPromptResolver = resolve;

        const newBtnConfirm = btnConfirm.cloneNode(true);
        btnConfirm.parentNode.replaceChild(newBtnConfirm, btnConfirm);

        newBtnConfirm.addEventListener('click', () => {
            const val = inputEl.value.trim();
            if (valorObligatorio && val !== valorObligatorio) {
                alert(`Para confirmar, debes ingresar exactamente la palabra: "${valorObligatorio}"`);
                return;
            }
            modal.classList.add('hidden');
            const res = currentPromptResolver;
            currentPromptResolver = null;
            res(val);
        });
    });
}

function cerrarPromptModal() {
    const modal = document.getElementById('promptModal');
    if (modal) {
        modal.classList.add('hidden');
    }
    if (currentPromptResolver) {
        const res = currentPromptResolver;
        currentPromptResolver = null;
        res(null);
    }
}



