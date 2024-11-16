import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class SistemaGestionTurnos {

    private static final String URL = "jdbc:mysql://localhost:3306/GestionTurnos";
    private static final String USER = "root";  // Usuario de MySQL
    private static final String PASSWORD = "diego4466";  // Contraseña de MySQL

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conexión exitosa a la base de datos.");

            Administrador admin = new Administrador("admin", "1234"); //Hago utilizacion de "admin" como usuario" y "1234" como contraseña

            // Solicitar usuario y contraseña para iniciar sesion
            boolean sesionIniciada = false;
            do {
                System.out.println("-------------------------------------------- ");
                System.out.println(" Bienvenidos al Sistema de Gestion de Turnos ");
                System.out.println("-------------------------------------------- ");
                System.out.println("  |       Iniciar Sesion        |  ");
                System.out.println("-------------------------------------------");
                System.out.print("Usuario: ");
                String usuario = scanner.nextLine();
                System.out.print("Contraseña: ");
                String contrasena = scanner.nextLine();

                if (admin.iniciarSesion(usuario, contrasena)) {
                    sesionIniciada = true;
                    System.out.println("Bienvenido, " + admin.getUsuario() + "!");
                } else {
                    System.out.println("Credenciales incorrectas. Intente nuevamente.");
                }
            } while (!sesionIniciada);


            int opcion;
            do {
                mostrarMenu();
                opcion = obtenerEntero(scanner, "Seleccione una opción: ");
                switch (opcion) {
                    case 1 -> reservarTurno(conn, scanner);
                    case 2 -> modificarTurno(conn, scanner);
                    case 3 -> cancelarTurno(conn, scanner);
                    case 4 -> consultarDisponibilidad(conn, scanner);
                    case 5 -> mostrarDetallesTurno(conn, scanner);
                    case 6 -> System.out.println("Saliendo del sistema...");
                    default -> System.out.println("Opción inválida. Intente nuevamente.");
                }
            } while (opcion != 6);
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }

    private static void mostrarMenu() {
        System.out.println("---------------------------------");
        System.out.println("Sistema de Gestión de Turnos");
        System.out.println("---------------------------------");
        System.out.println("1. Reservar Turno");
        System.out.println("2. Modificar Turno");
        System.out.println("3. Cancelar Turno");
        System.out.println("4. Consultar Disponibilidad de una Cancha");
        System.out.println("5. Mostrar Detalles del Turno");
        System.out.println("6. Salir");
    }

    private static void reservarTurno(Connection conn, Scanner scanner) {
        try {
            int idCancha = obtenerEntero(scanner, "Ingrese ID de la cancha: ");

            // Verificar si la cancha existe en la base de datos
            if (!canchaExiste(conn, idCancha)) {
                System.out.println("Error: El ID de la cancha no existe. Verifique e intente nuevamente.");
                return;
            }

            LocalDate fecha = obtenerFecha(scanner, "Ingrese la fecha (yyyy-MM-dd): ");
            LocalTime hora = obtenerHora(scanner, "Ingrese la hora (HH:mm): ");

            // Verificar Turnos Duplicados
            if (existeTurno(conn, idCancha, fecha, hora)) {
                System.out.println("Ya existe un turno reservado para esta cancha en la fecha y hora especificadas.");
                return;
            }

            String sql = "INSERT INTO turno (idCancha, fecha, hora, duracion, estado) VALUES (?, ?, ?, '01:00:00', 'Reservado')";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idCancha);
                stmt.setDate(2, Date.valueOf(fecha));
                stmt.setTime(3, Time.valueOf(hora));
                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Turno reservado exitosamente.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al reservar turno: " + e.getMessage());
        }
    }

    private static boolean canchaExiste(Connection conn, int idCancha) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cancha WHERE idCancha = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCancha);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Devuelve true si existe al menos una cancha con ese ID
            }
        }
        return false;
    }


    private static void modificarTurno(Connection conn, Scanner scanner) {
        try {
            int idTurno = obtenerEntero(scanner, "Ingrese el ID del turno a modificar: ");
            LocalDate nuevaFecha = obtenerFecha(scanner, "Ingrese nueva fecha (yyyy-MM-dd): ");
            LocalTime nuevaHora = obtenerHora(scanner, "Ingrese nueva hora (HH:mm): ");

            String sql = "UPDATE turno SET fecha = ?, hora = ? WHERE idTurno = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDate(1, Date.valueOf(nuevaFecha));
                stmt.setTime(2, Time.valueOf(nuevaHora));
                stmt.setInt(3, idTurno);
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("Turno modificado exitosamente.");
                } else {
                    System.out.println("No se encontró el turno con el ID especificado.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al modificar turno: " + e.getMessage());
        }
    }

    private static void cancelarTurno(Connection conn, Scanner scanner) {
        try {
            int idTurno = obtenerEntero(scanner, "Ingrese el ID del turno a cancelar: ");

            // Verificar si el turno existe antes de intentar cancelarlo
            if (!turnoExiste(conn, idTurno)) {
                System.out.println("Error: No se encontró un turno con el ID especificado.");
                return;
            }

            scanner.nextLine();

            // Confirmación del usuario
            System.out.print("¿Está seguro de que desea cancelar el turno? (s/n): ");
            String confirmacion = scanner.nextLine().trim().toLowerCase(); // Aceptar mayúsculas y minúsculas
            if (!confirmacion.equals("s")) {
                System.out.println("Cancelación abortada.");
                return;
            }

            // Cancelacion del Turno
            String sql = "DELETE FROM turno WHERE idTurno = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idTurno);
                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    System.out.println("Turno cancelado exitosamente.");
                } else {
                    System.out.println("No se encontró el turno con el ID especificado. No se realizó ninguna acción.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al cancelar turno: " + e.getMessage());
        }
    }

    private static boolean turnoExiste(Connection conn, int idTurno) throws SQLException {
        String sql = "SELECT COUNT(*) FROM turno WHERE idTurno = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idTurno);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Devuelve true si el turno existe
            }
        }
        return false;
    }


    private static void consultarDisponibilidad(Connection conn, Scanner scanner) {
        try {
            int idCancha = obtenerEntero(scanner, "Ingrese ID de la cancha para consultar disponibilidad: ");
            String sql = "SELECT * FROM turno WHERE idCancha = ? AND estado = 'Disponible'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idCancha);
                ResultSet rs = stmt.executeQuery();
                System.out.println("Turnos disponibles para la cancha " + idCancha + ":");
                while (rs.next()) {
                    System.out.println("ID Turno: " + rs.getInt("idTurno") + ", Fecha: " + rs.getDate("fecha") + ", Hora: " + rs.getTime("hora"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar disponibilidad: " + e.getMessage());
        }
    }

    private static void mostrarDetallesTurno(Connection conn, Scanner scanner) {
        try {
            int idTurno = obtenerEntero(scanner, "Ingrese el ID del turno para ver detalles: ");
            String sql = "SELECT * FROM turno WHERE idTurno = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, idTurno);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("ID Turno: " + rs.getInt("idTurno") + ", ID Cancha: " + rs.getInt("idCancha") +
                            ", Fecha: " + rs.getDate("fecha") + ", Hora: " + rs.getTime("hora") +
                            ", Duración: " + rs.getTime("duracion") + ", Estado: " + rs.getString("estado"));
                } else {
                    System.out.println("No se encontró el turno con el ID especificado.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar detalles del turno: " + e.getMessage());
        }
    }

    private static boolean existeTurno(Connection conn, int idCancha, LocalDate fecha, LocalTime hora) throws SQLException {
        String sql = "SELECT COUNT(*) FROM turno WHERE idCancha = ? AND fecha = ? AND hora = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCancha);
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setTime(3, Time.valueOf(hora));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static int obtenerEntero(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        while (!scanner.hasNextInt()) {
            System.out.print("Entrada inválida. " + mensaje);
            scanner.next();
        }
        return scanner.nextInt();
    }

    private static LocalDate obtenerFecha(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        while (true) {
            try {
                return LocalDate.parse(scanner.nextLine(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.print("Formato inválido. " + mensaje);
            }
        }
    }

    private static LocalTime obtenerHora(Scanner scanner, String mensaje) {
        System.out.print(mensaje);
        while (true) {
            try {
                return LocalTime.parse(scanner.nextLine(), TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.print("Formato inválido. " + mensaje);
            }
        }
    }

    static class Administrador {
        private String usuario;
        private String contrasena;

        public Administrador(String usuario, String contrasena) {
            this.usuario = usuario;
            this.contrasena = contrasena;
        }

        public String getUsuario() {
            return usuario;
        }

        public boolean iniciarSesion(String usuario, String contrasena) {
            return this.usuario.equals(usuario) && this.contrasena.equals(contrasena);
        }
    }
}
