package es.batbatcar.v2p4.modelo.repositories;

import es.batbatcar.v2p4.exceptions.*;
import es.batbatcar.v2p4.modelo.dao.inmemorydao.InMemoryReservaDAO;
import es.batbatcar.v2p4.modelo.dao.inmemorydao.InMemoryViajeDAO;
import es.batbatcar.v2p4.modelo.dao.sqldao.SQLReservaDAO;
import es.batbatcar.v2p4.modelo.dao.sqldao.SQLViajeDAO;
import es.batbatcar.v2p4.modelo.dto.Reserva;
import es.batbatcar.v2p4.modelo.dto.viaje.EstadoViaje;
import es.batbatcar.v2p4.modelo.dto.viaje.Viaje;
import es.batbatcar.v2p4.modelo.dao.interfaces.ReservaDAO;
import es.batbatcar.v2p4.modelo.dao.interfaces.ViajeDAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class ViajesRepository {

	private final ViajeDAO viajeDAO;
	private final ReservaDAO reservaDAO;

	@Autowired
	public ViajesRepository(@Autowired SQLViajeDAO viajeDAO, @Autowired SQLReservaDAO reservaDAO) {
		this.viajeDAO = viajeDAO;
		this.reservaDAO = reservaDAO;
	}

	public Set<Viaje> findAll() {
		Set<Viaje> viajes = viajeDAO.findAll();

		for (Viaje viaje : viajes) {
			List<Reserva> reservas = this.reservaDAO.findAllByTravel(viaje);
			viaje.setNumReservas(reservas.size());
		}
		return viajes;
	}

	public int getNextCodViaje() {
		return viajeDAO.findAll().size() + 1;
	}

	public int getNextCodReserva(Viaje viaje) {
		return reservaDAO.findAllByTravel(viaje).size() + 1;
	}

	public void save(Viaje viaje) throws ViajeAlreadyExistsException, ViajeNotFoundException {
		if (viajeDAO.findById(viaje.getCodViaje()) == null) {
			viajeDAO.add(viaje);
		} else {
			viajeDAO.update(viaje);
		}
	}

	public List<Reserva> findReservasByViaje(Viaje viaje) {
		return reservaDAO.findAllByTravel(viaje);
	}

	public void save(Reserva reserva) throws ReservaAlreadyExistsException, ReservaNotFoundException {
		if (reservaDAO.findById(reserva.getCodigoReserva()) == null) {
			reservaDAO.add(reserva);
		} else {
			reservaDAO.update(reserva);
		}
	}

	public void remove(Reserva reserva) throws ReservaNotFoundException {
		reservaDAO.remove(reserva);
	}

	public void remove(Viaje viaje) throws ViajeNotFoundException {
		viajeDAO.remove(viaje);
	}

	public Set<Viaje> findByLugarDestino(String destino) throws ViajeNotFoundException {
		Set<Viaje> viajes = viajeDAO.findAll();
		Set<Viaje> viajesFiltrados = new HashSet<>();

		for (Viaje viaje : viajes) {
			if (viaje.getRuta().toLowerCase().contains(destino.toLowerCase())) {
				viajesFiltrados.add(viaje);
			}
		}

		if (viajesFiltrados.isEmpty()) {
			throw new ViajeNotFoundException("No se encontraron viajes con destino a " + destino);
		}

		return viajesFiltrados;
	}

	public Viaje findByCod(int codViaje) throws ViajeNotFoundException {
		Viaje viaje = viajeDAO.findById(codViaje);
		if (viaje == null) {
			throw new ViajeNotFoundException(codViaje);
		}
		return viaje;
	}

	public Reserva findByCod(String codigoReserva) throws ReservaNotFoundException {
		Reserva reserva = reservaDAO.findById(codigoReserva);
		if (reserva == null) {
			throw new ReservaNotFoundException(codigoReserva);
		}
		return reserva;
	}

	public Viaje findViajesSiPermiteReserva(int codViaje, String usuario, int plazasSolicitadas) throws ReservaNoValidaException {
		Viaje viaje = viajeDAO.findById(codViaje);
		if (viaje == null) {
			throw new ReservaNoValidaException("Viaje no encontrado");
		}
		if (viaje.getPropietario().equals(usuario)) {
			throw new ReservaNoValidaException("El propietario no puede hacer una reserva");
		}
		if (!viaje.tieneEsteEstado(EstadoViaje.ABIERTO) || viaje.isCancelado()) {
			throw new ReservaNoValidaException("El viaje no está disponible para reservas");
		}
		if (viaje.getPlazasOfertadas() < plazasSolicitadas ||
				viaje.getPlazasOfertadas() - findReservasByViaje(viaje).size() < plazasSolicitadas) {
			throw new ReservaNoValidaException("No hay plazas suficientes");
		}
		for (Reserva reserva : findReservasByViaje(viaje)) {
			if (reserva.getUsuario().equals(usuario)) {
				throw new ReservaNoValidaException("Usuario ya tiene una reserva en este viaje");
			}
		}
		return viaje;
	}
}
