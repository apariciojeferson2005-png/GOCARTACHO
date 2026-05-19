package com.gocartacho.gocartacho.model;

/**
 * Enum tipado para los roles de usuario.
 * Evita errores de tipeo que con String harían silenciosamente inefectivo el acceso.
 */
public enum RolUsuario {
    USER,
    ADMIN,
    COMERCIANTE,
    SUPER_ADMIN
}
