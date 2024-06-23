package es.batbatcar.v2p4.modelo.dao.sqldao;

import es.batbatcar.v2p4.exceptions.ReservaAlreadyExistsException;
import es.batbatcar.v2p4.exceptions.ReservaNotFoundException;
import es.batbatcar.v2p4.modelo.dto.Reserva;
import es.batbatcar.v2p4.modelo.dto.viaje.Viaje;
import es.batbatcar.v2p4.modelo.services.MySQLConnection;
import es.batbatcar.v2p4.modelo.dao.interfaces.ReservaDAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class SQLReservaDAO implements ReservaDAO {

	@Autowired
	private MySQLConnection mySQLConnection;

	@Override
	public Set<Reserva> findAll() {
		Set<Reserva> reservas = new HashSet<>();
		String query = "SELECT * FROM reservas";

		try (Connection connection = mySQLConnection.getConnection();
			 Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery(query)) {

			while (rs.next()) {
				reservas.add(mapResultSetToReserva(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return reservas;
	}

	@Override
	public Reserva findById(String id) {
		String query = "SELECT * FROM reservas WHERE codigoReserva = ?";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, id);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return mapResultSetToReserva(rs);
				} else {
					throw new ReservaNotFoundException("Reserva not found with id: " + id);
				}
			} catch (ReservaNotFoundException e) {
				throw new RuntimeException(e);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public ArrayList<Reserva> findAllByUser(String user) {
		ArrayList<Reserva> reservas = new ArrayList<>();
		String query = "SELECT * FROM reservas WHERE usuario = ?";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, user);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					reservas.add(mapResultSetToReserva(rs));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return reservas;
	}

	@Override
	public ArrayList<Reserva> findAllByTravel(Viaje viaje) {
		ArrayList<Reserva> reservas = new ArrayList<>();
		String query = "SELECT * FROM reservas WHERE codigoReserva LIKE ?";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, viaje.getCodViaje() + "-%");
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					reservas.add(mapResultSetToReserva(rs));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return reservas;
	}

	@Override
	public Reserva getById(String id) throws ReservaNotFoundException {
		return findById(id);
	}

	@Override
	public List<Reserva> findAllBySearchParams(Viaje viaje, String searchParams) {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public void add(Reserva reserva) throws ReservaAlreadyExistsException {
		String query = "INSERT INTO reservas (codigoReserva, usuario, plazasSolicitadas, fechaRealizacion, viaje) VALUES (?, ?, ?, ?, ?)";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, reserva.getCodigoReserva());
			pstmt.setString(2, reserva.getUsuario());
			pstmt.setInt(3, reserva.getPlazasSolicitadas());
			pstmt.setTimestamp(4, Timestamp.valueOf(reserva.getFechaRealizacion()));
			pstmt.setString(5, String.valueOf(reserva.getViaje().getCodViaje()));

			pstmt.executeUpdate();
		} catch (SQLException e) {
			if (e.getSQLState().equals("23000")) {
				throw new ReservaAlreadyExistsException(reserva);
			}
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void update(Reserva reserva) throws ReservaNotFoundException {
		String query = "UPDATE reservas SET usuario = ?, plazasSolicitadas = ?, fechaRealizacion = ?, viaje = ? WHERE codigoReserva = ?";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, reserva.getUsuario());
			pstmt.setInt(2, reserva.getPlazasSolicitadas());
			pstmt.setTimestamp(3, Timestamp.valueOf(reserva.getFechaRealizacion()));
			pstmt.setString(4, String.valueOf(reserva.getViaje().getCodViaje()));
			pstmt.setString(5, reserva.getCodigoReserva());

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected == 0) {
				throw new ReservaNotFoundException("Reserva not found with id: " + reserva.getCodigoReserva());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void remove(Reserva reserva) throws ReservaNotFoundException {
		String query = "DELETE FROM reservas WHERE codigoReserva = ?";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, reserva.getCodigoReserva());

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected == 0) {
				throw new ReservaNotFoundException("Reserva not found with id: " + reserva.getCodigoReserva());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public int getNumPlazasReservadasEnViaje(Viaje viaje) {
		String query = "SELECT SUM(plazasSolicitadas) AS totalPlazas FROM reservas WHERE codigoReserva LIKE ?";
		int totalPlazas = 0;

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, viaje.getCodViaje() + "-%");
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					totalPlazas = rs.getInt("totalPlazas");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return totalPlazas;
	}

	@Override
	public Reserva findByUserInTravel(String usuario, Viaje viaje) {
		String query = "SELECT * FROM reservas WHERE usuario = ? AND codigoReserva LIKE ?";

		try (Connection connection = mySQLConnection.getConnection();
			 PreparedStatement pstmt = connection.prepareStatement(query)) {

			pstmt.setString(1, usuario);
			pstmt.setString(2, viaje.getCodViaje() + "-%");
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return mapResultSetToReserva(rs);
				} else {
					throw new ReservaNotFoundException(String.valueOf(viaje.getCodViaje()));
				}
			} catch (ReservaNotFoundException e) {
				throw new RuntimeException(e);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private Reserva mapResultSetToReserva(ResultSet rs) throws SQLException {
		String codigoReserva = rs.getString("codigoReserva");
		String usuario = rs.getString("usuario");
		int plazasSolicitadas = rs.getInt("plazasSolicitadas");
		LocalDateTime fechaRealizacion = rs.getTimestamp("fechaRealizacion").toLocalDateTime();
		String codViaje = rs.getString("viaje");
		Viaje viaje = new Viaje(Integer.parseInt(codViaje));

		return new Reserva(codigoReserva, usuario, plazasSolicitadas, fechaRealizacion, viaje);
	}
}
