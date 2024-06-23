package es.batbatcar.v2p4.modelo.dao.sqldao;


import es.batbatcar.v2p4.exceptions.ViajeNotFoundException;
import es.batbatcar.v2p4.modelo.services.MySQLConnection;
import es.batbatcar.v2p4.modelo.dao.interfaces.ViajeDAO;
import es.batbatcar.v2p4.modelo.dto.viaje.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Repository
public class SQLViajeDAO implements ViajeDAO {

    @Autowired
    private MySQLConnection mySQLConnection;

    @Override
    public Set<Viaje> findAll() {
        Set<Viaje> viajes = new HashSet<>();
        String query = "SELECT * FROM viajes";

        try (Connection connection = mySQLConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                viajes.add(mapResultSetToViaje(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return viajes;
    }

    @Override
    public Set<Viaje> findAll(String city) {
        Set<Viaje> viajes = new HashSet<>();
        String query = "SELECT * FROM viajes WHERE ruta LIKE ?";

        try (Connection connection = mySQLConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, "%" + city + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    viajes.add(mapResultSetToViaje(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return viajes;
    }

    @Override
    public Set<Viaje> findAll(EstadoViaje estadoViaje) {
        Set<Viaje> viajes = new HashSet<>();
        String query = "SELECT * FROM viajes WHERE estadoViaje = ?";

        try (Connection connection = mySQLConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, estadoViaje.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    viajes.add(mapResultSetToViaje(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return viajes;
    }

    @Override
    public Set<Viaje> findAll(Class<? extends Viaje> viajeClass) {
        Connection connection = mySQLConnection.getConnection();
        String tipoViaje = viajeClass.getSimpleName();
        String sql = "SELECT * FROM viajes WHERE tipo = ?";
        Set<Viaje> viajes = new HashSet<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tipoViaje);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int codViaje = rs.getInt("codViaje");
                    String propietario = rs.getString("propietario");
                    String ruta = rs.getString("ruta");
                    LocalDateTime fechaSalida = rs.getTimestamp("fechaSalida").toLocalDateTime();
                    long duracion = rs.getLong("duracion");
                    float precio = rs.getFloat("precio");
                    int plazasOfertadas = rs.getInt("plazasOfertadas");
                    EstadoViaje estadoViaje = EstadoViaje.parse(rs.getString("estadoViaje"));

                    Viaje viaje = createViajeInstance(viajeClass, codViaje, propietario, ruta, fechaSalida, duracion, precio, plazasOfertadas, estadoViaje);
                    viajes.add(viaje);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching viajes of type " + tipoViaje, e);
        }
        return viajes;
    }

    @Override
    public Viaje findById(int codViaje) {
        String query = "SELECT * FROM viajes WHERE codViaje = ?";
        try (Connection connection = mySQLConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setInt(1, codViaje);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToViaje(rs);
                } else {
                    throw new ViajeNotFoundException("Viaje not found with id: " + codViaje);
                }
            } catch (ViajeNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Viaje getById(int codViaje) {
        return findById(codViaje);
    }

    @Override
    public void add(Viaje viaje) {
        String query = "INSERT INTO viajes (codViaje, propietario, ruta, fechaSalida, duracion, precio, plazasOfertadas, estadoViaje) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = mySQLConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setInt(1, viaje.getCodViaje());
            pstmt.setString(2, viaje.getPropietario());
            pstmt.setString(3, viaje.getRuta());
            pstmt.setTimestamp(4, Timestamp.valueOf(viaje.getFechaSalida()));
            pstmt.setLong(5, viaje.getDuracion());
            pstmt.setFloat(6, viaje.getPrecio());
            pstmt.setInt(7, viaje.getPlazasOfertadas());
            pstmt.setString(8, viaje.getEstado().name());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al agregar un nuevo viaje", e);
        }
    }

    @Override
    public void update(Viaje viaje) throws ViajeNotFoundException {
        String query = "UPDATE viajes SET propietario = ?, ruta = ?, fechaSalida = ?, duracion = ?, precio = ?, plazasOfertadas = ?, estadoViaje = ? WHERE codViaje = ?";

        try (Connection connection = mySQLConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, viaje.getPropietario());
            pstmt.setString(2, viaje.getRuta());
            pstmt.setTimestamp(3, Timestamp.valueOf(viaje.getFechaSalida()));
            pstmt.setLong(4, viaje.getDuracion());
            pstmt.setFloat(5, viaje.getPrecio());
            pstmt.setInt(6, viaje.getPlazasOfertadas());
            pstmt.setString(7, viaje.getEstado().name());
            pstmt.setInt(8, viaje.getCodViaje());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new ViajeNotFoundException("Viaje not found with id: " + viaje.getCodViaje());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el viaje", e);
        }
    }

    @Override
    public void remove(Viaje viaje) throws ViajeNotFoundException {
        String query = "DELETE FROM viajes WHERE codViaje = ?";

        try (Connection connection = mySQLConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setInt(1, viaje.getCodViaje());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new ViajeNotFoundException("Viaje not found with id: " + viaje.getCodViaje());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el viaje", e);
        }
    }

    private Viaje mapResultSetToViaje(ResultSet rs) throws SQLException {
        int codViaje = rs.getInt("codViaje");
        String propietario = rs.getString("propietario");
        String ruta = rs.getString("ruta");
        LocalDateTime fechaSalida = rs.getTimestamp("fechaSalida").toLocalDateTime();
        long duracion = rs.getLong("duracion");
        float precio = rs.getFloat("precio");
        int plazasOfertadas = rs.getInt("plazasOfertadas");
        EstadoViaje estadoViaje = EstadoViaje.parse(rs.getString("estadoViaje"));

        return new Viaje(codViaje, propietario, ruta, fechaSalida, duracion, precio, plazasOfertadas, estadoViaje);
    }

    private Viaje createViajeInstance(Class<? extends Viaje> viajeClass, int codViaje, String propietario, String ruta, LocalDateTime fechaSalida, long duracion, float precio, int plazasOfertadas, EstadoViaje estadoViaje) {
        try {
            return viajeClass.getDeclaredConstructor(int.class, String.class, String.class, LocalDateTime.class, long.class, float.class, int.class, EstadoViaje.class)
                    .newInstance(codViaje, propietario, ruta, fechaSalida, duracion, precio, plazasOfertadas, estadoViaje);
        } catch (Exception e) {
            throw new RuntimeException("Error creating instance of " + viajeClass.getSimpleName(), e);
        }
    }
}
