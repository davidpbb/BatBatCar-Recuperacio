package es.batbatcar.v2p4.controllers;

import es.batbatcar.v2p4.exceptions.*;
import es.batbatcar.v2p4.modelo.dto.Reserva;
import es.batbatcar.v2p4.modelo.dto.viaje.Viaje;
import es.batbatcar.v2p4.modelo.repositories.ViajesRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class ReservaController {

    @Autowired
    private ViajesRepository viajeRepo;

    @GetMapping("/viaje/reserva/add")
    public String mostrarFormularioReserva(@RequestParam Map<String, String> parametros, Model modelo) {
        String viajeId = parametros.get("codViaje");
        if (viajeId == null) {
            return "redirect:/viajes";
        }
        modelo.addAttribute("codViaje", viajeId);

        return "reserva/formulario_reserva";
    }

    @PostMapping("/viaje/reserva/add")
    public String crearReserva(@RequestParam Map<String, String> parametros, RedirectAttributes redirectAttrs) {
        String viajeId = parametros.get("codViaje");
        try {
            String usuario = parametros.get("usuario");
            int plazas = Integer.parseInt(parametros.get("plazasSolicitadas"));
            Viaje viaje = viajeRepo.findByCod(Integer.parseInt(viajeId));
            String reservaId = String.valueOf(viajeRepo.getNextCodReserva(viaje));
            Reserva reserva = new Reserva(viajeId + "-" + reservaId, usuario, plazas, viaje);
            if (viajeRepo.findViajesSiPermiteReserva(Integer.parseInt(viajeId), usuario, plazas) != null) {
                viajeRepo.save(reserva);
                redirectAttrs.addFlashAttribute("infoMessage", "Reserva realizada con éxito");
            }
            return "redirect:/viajes";
        } catch (ReservaAlreadyExistsException | ReservaNotFoundException | ViajeNotFoundException e) {
            redirectAttrs.addFlashAttribute("errors", Collections.singletonMap("error", "No se pudo realizar la reserva: " + e.getMessage()));
            return "redirect:/viaje/reserva/nueva";
        } catch (ReservaNoValidaException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/viaje/reservas")
    public String listarReservas(@RequestParam Map<String, String> parametros, Model modelo) {
        String viajeId = parametros.get("codViaje");
        try {
            if (viajeId != null) {
                Viaje viaje = viajeRepo.findByCod(Integer.parseInt(viajeId));
                modelo.addAttribute("reservas", viajeRepo.findReservasByViaje(viaje));
                modelo.addAttribute("codViaje", viaje.getCodViaje());
                return "reserva/lista_reservas";
            }
            return "redirect:/viajes";
        } catch (ViajeNotFoundException e) {
            modelo.addAttribute("errors", "No se pudo encontrar las reservas: " + e.getMessage());
            return "redirect:/viajes";
        }
    }

    @PostMapping("/viaje/reserva/cancelar")
    public String anularReserva(@RequestParam Map<String, String> parametros, RedirectAttributes redirectAttrs) {
        String reservaId = parametros.get("codigoReserva");
        try {
            Reserva reserva = viajeRepo.findByCod(reservaId);
            viajeRepo.remove(reserva);
            redirectAttrs.addFlashAttribute("infoMessage", "Reserva cancelada con éxito");
            return "redirect:/viajes";
        } catch (ReservaNotFoundException | ReservaNoCancelableException e) {
            redirectAttrs.addFlashAttribute("errors", Collections.singletonMap("error", e.getMessage()));
            return "redirect:/viajes";
        }
    }

    @GetMapping("/reserva")
    public String verDetalleReserva(@RequestParam Map<String, String> parametros, Model modelo, RedirectAttributes redirectAttrs) {
        String reservaId = parametros.get("codigoReserva");
        try {
            Reserva reserva = viajeRepo.findByCod(reservaId);
            modelo.addAttribute("reservas", reserva);
            return "reserva/detalle_reserva";
        } catch (ReservaNotFoundException e) {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("error: ", e.getMessage());
            redirectAttrs.addFlashAttribute("errors", errors);
            return "redirect:/reserva/reserva_detalle";
        }
    }
}
