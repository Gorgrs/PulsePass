-- Fase 4: seguridad. Credenciales y roles separados de la tabla users.

-- Una credencial por usuario (relacion 1:1). Guarda solo el hash BCrypt.
CREATE TABLE user_credentials (
    user_id             BIGINT       PRIMARY KEY,
    password_hash       VARCHAR(100) NOT NULL,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Un usuario puede tener varios roles; la clave primaria evita duplicados.
CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(20) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

-- Usuarios que ya existian: reciben solo el rol USER. No se les crea
-- contrasena, asi que no pueden iniciar sesion hasta una activacion segura.
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users;