DROP DATABASE IF EXISTS gramola;
CREATE DATABASE gramola CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gramola;

-- Tabla donde guardo las URLs que antes estaban escritas directamente en el código.
CREATE TABLE app_url (
  id VARCHAR(100) NOT NULL,
  url_value VARCHAR(1000) NOT NULL,
  active BIT(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Tabla de tokens
CREATE TABLE token (
  id VARCHAR(36) NOT NULL,
  creation_time DATETIME(6) NOT NULL,
  use_time DATETIME(6) NULL,
  expires_time DATETIME(6) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Tabla de usuarios
CREATE TABLE `user` (
  email VARCHAR(255) NOT NULL,
  bar_name VARCHAR(120) NOT NULL,
  pwd VARCHAR(255) NOT NULL,

  client_id VARCHAR(255) NULL,
  client_secret VARCHAR(255) NULL,

  creation_token_id VARCHAR(36) NOT NULL,
  reset_token_id VARCHAR(36) NULL,

  subscription_paid BIT(1) NOT NULL DEFAULT b'0',

  -- Precio que este bar cobrará por cada canción.
  -- 88 significa 0,88 €
  song_price_cents INT NOT NULL DEFAULT 88,

  PRIMARY KEY (email),

  CONSTRAINT fk_user_creation_token
    FOREIGN KEY (creation_token_id) REFERENCES token(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

  CONSTRAINT fk_user_reset_token
    FOREIGN KEY (reset_token_id) REFERENCES token(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_reset_token_id ON `user`(reset_token_id);

-- Tabla de transacciones de Stripe
CREATE TABLE stripe_transaction (
  id VARCHAR(36) NOT NULL,
  email VARCHAR(255) NULL,
  plan_id VARCHAR(50) NULL,
  data JSON NOT NULL,

  PRIMARY KEY (id),

  INDEX idx_stripe_transaction_email (email),
  INDEX idx_stripe_transaction_plan_id (plan_id),

  CONSTRAINT fk_stripe_transaction_user
    FOREIGN KEY (email) REFERENCES `user`(email)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Tabla de canciones pagadas/solicitadas
CREATE TABLE requested_song (
  id VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  artist VARCHAR(255) NOT NULL,
  uri VARCHAR(255) NOT NULL,
  image VARCHAR(255) NULL,
  date BIGINT NOT NULL,
  played BIT(1) NOT NULL DEFAULT b'0',
  bar_email VARCHAR(255) NOT NULL,

  PRIMARY KEY (id),

  INDEX idx_requested_song_bar_email (bar_email),
  INDEX idx_requested_song_played (played),
  INDEX idx_requested_song_date (date),

  CONSTRAINT fk_requested_song_bar_email
    FOREIGN KEY (bar_email) REFERENCES `user`(email)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Tabla de planes y precios en base de datos
CREATE TABLE subscription_plan (
  id VARCHAR(50) NOT NULL,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(255) NULL,
  amount_cents INT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'eur',
  plan_type VARCHAR(20) NOT NULL,
  interval_type VARCHAR(20) NOT NULL,
  recommended BIT(1) NOT NULL DEFAULT b'0',
  active BIT(1) NOT NULL DEFAULT b'1',
  sort_order INT NOT NULL DEFAULT 0,

  PRIMARY KEY (id),

  INDEX idx_subscription_plan_active (active),
  INDEX idx_subscription_plan_type (plan_type)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Datos iniciales de planes
INSERT INTO subscription_plan
(id, name, description, amount_cents, currency, plan_type, interval_type, recommended, active, sort_order)
VALUES
('monthly', 'Mensual', 'Pago mensual. Cancela cuando quieras.', 888,  'eur', 'SUBSCRIPTION', 'MONTH',    b'0', b'1', 1),
('yearly',  'Anual',   'Mejor precio por año.',                6767, 'eur', 'SUBSCRIPTION', 'YEAR',     b'1', b'1', 2),
('song',    'Canción', 'Pago por canción.',                      88, 'eur', 'SONG',         'ONE_TIME', b'0', b'1', 3);

-- URLs configurables de la aplicación.
-- No meto GEO ni JDBC porque hemos decidido dejarlas fuera.
INSERT INTO app_url (id, url_value, active)
VALUES

-- Frontend permitido
('FRONTEND_LOCALHOST_URL', 'http://localhost:4200', b'1'),
('FRONTEND_127_URL', 'http://127.0.0.1:4200', b'1'),

-- Backend base
('BACKEND_BASE_URL', 'http://localhost:8080', b'1'),
('BACKEND_127_BASE_URL', 'http://127.0.0.1:8080', b'1'),

-- Registro, confirmación y recuperación de contraseña
('BACKEND_CONFIRM_TOKEN_BASE_URL', 'http://127.0.0.1:8080/users/confirmToken', b'1'),
('FRONTEND_PAYMENT_URL', 'http://127.0.0.1:4200/payment', b'1'),
('FRONTEND_RESET_PASSWORD_URL', 'http://127.0.0.1:4200/reset-password', b'1'),
('USER_FORGOT_PASSWORD_URL', 'http://localhost:8080/users/forgotPassword', b'1'),
('USER_RESET_PASSWORD_URL', 'http://localhost:8080/users/resetPassword', b'1'),

-- Endpoints principales del backend usados por Angular
('PAYMENTS_BASE_URL', 'http://127.0.0.1:8080/payments', b'1'),
('REQUESTED_SONG_BASE_URL', 'http://127.0.0.1:8080/requestedSong', b'1'),
('USERS_BASE_URL', 'http://127.0.0.1:8080/users', b'1'),
('API_SPOTI_BASE_URL', 'http://127.0.0.1:8080/api/spoti', b'1'),
('SPOTI_CONNECT_URL', 'http://127.0.0.1:8080/spoti/connect', b'1'),

-- Spotify
('SPOTIFY_CALLBACK_URL', 'http://127.0.0.1:4200/callback', b'1'),
('FRONTEND_CALLBACK_URL', 'http://127.0.0.1:4200/callback', b'1'),
('SPOTIFY_AUTHORIZE_URL', 'https://accounts.spotify.com/authorize', b'1'),
('SPOTIFY_TOKEN_URL', 'https://accounts.spotify.com/api/token', b'1'),
('SPOTIFY_API_BASE_URL', 'https://api.spotify.com/v1', b'1'),
('SPOTIFY_QUEUE_URL', 'https://api.spotify.com/v1/me/player/queue', b'1'),
('SPOTIFY_PLAYLISTS_URL', 'https://api.spotify.com/v1/me/playlists', b'1'),
('SPOTIFY_DEVICES_URL', 'https://api.spotify.com/v1/me/player/devices', b'1'),
('SPOTIFY_CURRENTLY_PLAYING_URL', 'https://api.spotify.com/v1/me/player/currently-playing', b'1'),
('SPOTIFY_SEARCH_URL', 'https://api.spotify.com/v1/search', b'1'),

-- Stripe
('STRIPE_JS_URL', 'https://js.stripe.com/v3/', b'1');