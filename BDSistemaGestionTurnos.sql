CREATE DATABASE GestionTurnos;
USE GestionTurnos;

CREATE TABLE turno (
	idTurno INT AUTO_INCREMENT PRIMARY KEY,
    idCancha INT,
    fecha DATE,
    hora TIME,
    duracion TIME,
    estado ENUM('Disponible', 'Reservado'),
    FOREIGN KEY (idCancha) REFERENCES cancha(idCancha)
);


CREATE TABLE reserva (
	idReserva INT AUTO_INCREMENT PRIMARY KEY,
    idCliente INT,
    idTurno INT,
    idCancha INT,
    estado ENUM('Registrada', 'Cancelada', 'Modificada'),
    FOREIGN KEY (idCliente) REFERENCES cliente(idCliente),
    FOREIGN KEY (idCancha) REFERENCES cancha(idCancha),
    FOREIGN KEY (idTurno) REFERENCES turno(idTurno)
);


CREATE TABLE cliente (
	idCliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    apellido VARCHAR(100),
    dni VARCHAR(100),
    telefono VARCHAR(50)
);


CREATE TABLE cancha (
	idCancha INT AUTO_INCREMENT PRIMARY KEY,
    tipo ENUM('Futbol 5', 'Futbol 8'),
    numCancha VARCHAR(50),
    capacidad VARCHAR(20),
    estado ENUM('Disponible','Ocupada'),
    precio DECIMAL
);

INSERT INTO cancha (tipo, numCancha, capacidad, estado, precio)
VALUES ('Futbol 5', 'Cancha 1', '10 jugadores', 'Disponible', 20000.00),
		('Futbol 5', 'Cancha 2', '10 jugadores', 'Disponible',20000.00),
        ('Futbol 5', 'Cancha 3', '10 jugadores', 'Disponible',20000.00),
        ('Futbol 5', 'Cancha 4', '10 jugadores', 'Disponible',20000.00),
        ('Futbol 5', 'Cancha 5', '10 jugadores', 'Disponible',20000.00),
		('Futbol 8', 'Cancha 1', '16 jugadores', 'Disponible', 32000.00),
		('Futbol 8', 'Cancha 2', '16 jugadores', 'Disponible', 32000.00),
        ('Futbol 8', 'Cancha 3', '16 jugadores', 'Disponible', 32000.00);





DELETE FROM cancha;
DELETE FROM turno;