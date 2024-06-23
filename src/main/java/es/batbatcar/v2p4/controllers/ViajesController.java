package es.batbatcar.v2p4.controllers;

import es.batbatcar.v2p4.exceptions.*;
import es.batbatcar.v2p4.modelo.dto.Reserva;
import es.batbatcar.v2p4.modelo.dto.viaje.Viaje;
import es.batbatcar.v2p4.modelo.repositories.ViajesRepository;
import es.batbatcar.v2p4.utils.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
public class ViajesController {

    @Autowired
    private ViajesRepository viajesRepo;

    @GetMapping("/viajes")
    public String mostrarViajes(@RequestParam(required = false) Map<String, String> params, Model model) {
        String destino = params.get("ruta");

        if (destino != null) {

            try {
                model.addAttribute("viajes", viajesRepo.findByLugarDestino(destino));
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            model.addAttribute("viajes", viajesRepo.findAll());

        }

        model.addAttribute("titulo", "Listado de viajes");
        return "viaje/listado";
    }

    @GetMapping("/viaje/add")
    public String mostrarFormularioAdd() {
        return "viaje/viaje_form";
    }

    @PostMapping("/viaje/add")
    public String agregarViaje(@RequestParam Map<String, String> params, RedirectAttributes redirectAttrs, Model model) {
        Map<String, String> errores = new HashMap<>();
        try {
            int codViaje = viajesRepo.getNextCodViaje();
            String ruta = params.get("ruta");
            if (!Validator.isValidRoute(ruta)) errores.put("ruta", "La ruta debe tener el formato 'Origen - Destino'.");

            int plazasOfertadas = Integer.parseInt(params.get("plazasOfertadas"));
            if (!Validator.isValidPlazasOfertadas(plazasOfertadas)) errores.put("plazasOfertadas", "Las plazas ofertadas deben estar entre 1 y 6.");

            String propietario = params.get("propietario");
            if (!Validator.isValidPropietario(propietario)) errores.put("propietario", "El propietario debe contener nombre y apellido, ambos con la primera letra en mayúscula.");

            float precio = Float.parseFloat(params.get("precio"));
            if (!Validator.isPositiveFloat(precio)) errores.put("precio", "El precio debe ser mayor que 0.");

            long duracion = Long.parseLong(params.get("duracion"));
            if (!Validator.isPositiveInt((int) duracion)) errores.put("duracion", "La duración debe ser mayor que 0.");

            String fecha = params.get("fecha");
            String hora = params.get("hora");
            if (!Validator.isValidDate(fecha)) errores.put("fecha", "El día de salida debe tener un formato válido (yyyy-MM-dd).");
            if (!Validator.isValidTime(hora)) errores.put("hora", "La hora de salida debe tener un formato válido (HH:mm).");

            LocalDateTime fechaHora = LocalDateTime.of(LocalDate.parse(fecha), LocalTime.parse(hora));

            if (!errores.isEmpty()) {
                redirectAttrs.addFlashAttribute("errors", errores);
                return "redirect:/viaje/add";
            }

            Viaje viaje = new Viaje(codViaje, propietario, ruta, fechaHora, duracion, precio, plazasOfertadas);
            viajesRepo.save(viaje);
            redirectAttrs.addFlashAttribute("infoMessage", "Viaje añadido con éxito");
            return "redirect:/viajes";
        } catch (ViajeAlreadyExistsException | ViajeNotFoundException e) {
            errores.put("error", "El viaje no se ha podido realizar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("errors", errores);
            return "redirect:/viaje/add";
        }
    }

    @GetMapping("/viaje")
    public String verDetalleViaje(@RequestParam Map<String, String> params, Model model, RedirectAttributes redirectAttrs) {
        try {
            String codViaje = params.get("codViaje");
            Viaje viaje = viajesRepo.findByCod(Integer.parseInt(codViaje));
            List<Reserva> reservas = viajesRepo.findReservasByViaje(viaje);
            model.addAttribute("reservas", reservas);
            model.addAttribute("viaje", viaje);
            return "viaje/viaje_detalle";
        } catch (ViajeNotFoundException e) {
            redirectAttrs.addFlashAttribute("errors", Collections.singletonMap("error", e.getMessage()));
            return "redirect:/viaje/viaje_detalle";
        }
    }

    @PostMapping("/viaje/cancelar")
    public String cancelarViaje(@RequestParam Map<String, String> params, RedirectAttributes redirectAttrs) {
        try {
            String codViaje = params.get("codViaje");
            Viaje viaje = viajesRepo.findByCod(Integer.parseInt(codViaje));
            if (!viaje.isCancelado()) {
                viaje.cancelar();
                redirectAttrs.addFlashAttribute("infoMessage", "Viaje cancelado con éxito");
            }
            return "redirect:/viajes";
        } catch (ViajeNotFoundException | ViajeNotCancelableException e) {
            redirectAttrs.addFlashAttribute("errors", Collections.singletonMap("error", e.getMessage()));
            return "redirect:/viajes";
        }
    }
}
