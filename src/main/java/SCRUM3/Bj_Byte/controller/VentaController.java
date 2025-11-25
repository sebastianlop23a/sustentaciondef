package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.*;
import SCRUM3.Bj_Byte.repository.*;
import SCRUM3.Bj_Byte.service.*;
import SCRUM3.Bj_Byte.util.ExportarExcelVentas;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.thymeleaf.context.Context;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.util.Base64;
import SCRUM3.Bj_Byte.dto.DetalleFacturaDTO;


@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired private InventarioRepository inventarioRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaDetalleRepository ventaDetalleRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private PdfService pdfService;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private VentaService ventaService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private InvoicePdfService invoicePdfService;

    private Empleado getEmpleadoLogueado(HttpSession session) {
        return (Empleado) session.getAttribute("empleadoLogueado");
    }

    /* ============================
           FORMULARIO REGISTRAR
       ============================ */
    @GetMapping("/registrar")
    public String mostrarFormularioVenta(Model model, HttpSession session) {

        if (getEmpleadoLogueado(session) == null)
            return "redirect:/empleados/login";

        model.addAttribute("inventarios", inventarioRepository.findAll());
        model.addAttribute("venta", new Venta());
        model.addAttribute("clientes",
                clienteRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActivo())).toList());

        return "ventas/registrar_venta";
    }

    /* ============================
        REGISTRAR VENTA
   ============================ */
@PostMapping("/registrar")
@Transactional
public String registrarVenta(
        @ModelAttribute Venta venta,
        @RequestParam List<Long> inventarioId,
        @RequestParam List<Integer> cantidad,
        @RequestParam(required = false) Long clienteId,
        HttpSession session,
        RedirectAttributes ra
) {
    try {

        Empleado empleado = getEmpleadoLogueado(session);
        if (empleado == null)
            return "redirect:/empleados/login";

        Set<Proveedor> proveedoresAsociados = new HashSet<>();
        BigDecimal totalVenta = BigDecimal.ZERO;
        List<VentaDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < inventarioId.size(); i++) {

            Long idInv = inventarioId.get(i);
            int cant = cantidad.get(i);

            Inventario inv = inventarioRepository.findById(idInv).orElseThrow();
            Producto prod = inv.getProducto();

            /* ============================
                 VALIDACIONES
               ============================ */
            if (!prod.getActivo()) {
                ra.addFlashAttribute("error", "Producto deshabilitado: " + prod.getNombre());
                return "redirect:/ventas/registrar";
            }

            if (cant > inv.getCantidad()) {
                ra.addFlashAttribute("error", "Stock insuficiente: " + prod.getNombre());
                return "redirect:/ventas/registrar";
            }

            /* ============================
               DESCONTAR INVENTARIO
               ============================ */
            inv.setCantidad(inv.getCantidad() - cant);
            inventarioRepository.save(inv);

            if (prod.getProveedores() != null)
                proveedoresAsociados.addAll(prod.getProveedores());

            /* ============================
                 CALCULO DE PRECIOS
               ============================ */

            BigDecimal precioUnit = prod.getPrecio();
            BigDecimal subtotalLinea = precioUnit.multiply(BigDecimal.valueOf(cant));

            // IVA 19% SOLO si el producto NO es exento
            BigDecimal ivaLinea = BigDecimal.ZERO;
            if (!Boolean.TRUE.equals(prod.getExento())) {
                ivaLinea = subtotalLinea.multiply(new BigDecimal("0.19"));
            }

            BigDecimal totalLinea = subtotalLinea.add(ivaLinea);

            /* ============================
                      DETALLE
               ============================ */
            VentaDetalle det = new VentaDetalle();
            det.setInventario(inv);
            det.setCantidad(cant);
            det.setPrecioUnitario(precioUnit);
            det.setSubtotal(subtotalLinea);
            det.setIva(ivaLinea);
            det.setTotalLinea(totalLinea);
            det.setVenta(venta);

            detalles.add(det);

            /* ============================
                  SUMAR TOTAL GENERAL
               ============================ */
            totalVenta = totalVenta.add(totalLinea);
        }

        /* ============================
               DATOS GENERALES VENTA
           ============================ */
        venta.setEmpleado(empleado);
        venta.setNombreEmpleado(empleado.getNombre());
        venta.setFecha(LocalDateTime.now());
        venta.setTotalVenta(totalVenta);
        venta.setProveedores(proveedoresAsociados);

        if (clienteId != null && clienteId > 0) {
            clienteRepository.findById(clienteId)
                    .ifPresent(c -> venta.setCliente(c.getNombre()));
        }

        venta.setDetalles(detalles);

        /* ============================
                GUARDAR VENTA
           ============================ */
        Venta ventaGuardada = ventaRepository.save(venta);

        for (VentaDetalle d : detalles) {
            d.setVenta(ventaGuardada);
            ventaDetalleRepository.save(d);
        }

        ra.addFlashAttribute("mensaje", "Venta registrada correctamente");

        return "redirect:/ventas/resumen/" + ventaGuardada.getId();

    } catch (Exception e) {
        ra.addFlashAttribute("error", "Error: " + e.getMessage());
        return "redirect:/ventas/registrar";
    }
}


    /* ============================
           EDITAR VENTA
       ============================ */
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, HttpSession session, Model model) {

        if (getEmpleadoLogueado(session) == null)
            return "redirect:/empleados/login";

        Venta venta = ventaRepository.findById(id).orElse(null);
        if (venta == null) return "redirect:/ventas/lista";

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", ventaDetalleRepository.findByVentaId(id));
        model.addAttribute("inventarios", inventarioRepository.findAll());

        return "ventas/editar_venta";
    }

    /* ============================
           ELIMINAR VENTA
       ============================ */
    @GetMapping("/eliminar/{id}")
    @Transactional
    public String eliminar(@PathVariable Long id, HttpSession session) {

        if (getEmpleadoLogueado(session) == null)
            return "redirect:/empleados/login";

        Venta venta = ventaRepository.findById(id).orElse(null);
        if (venta != null) {

            List<VentaDetalle> detalles = ventaDetalleRepository.findByVentaId(id);

            for (VentaDetalle d : detalles) {
                Inventario inv = d.getInventario();
                inv.setCantidad(inv.getCantidad() + d.getCantidad());
                inventarioRepository.save(inv);
            }

            ventaDetalleRepository.deleteAll(detalles);
            ventaRepository.delete(venta);
        }

        return "redirect:/ventas/lista";
    }

    /* ============================
           LISTAR VENTAS
       ============================ */
    @GetMapping("/lista")
    public String lista(HttpSession session, Model model) {
        Empleado emp = getEmpleadoLogueado(session);
        if (emp == null) return "redirect:/empleados/login";

        if (emp.getRolId() != 1)
            return "redirect:/ventas/mis-ventas";

        model.addAttribute("ventas", ventaRepository.findAll());
        return "ventas/listar_ventas";
    }

    /* ============================
           MIS VENTAS
       ============================ */
    @GetMapping("/mis-ventas")
    public String misVentas(HttpSession session, Model model) {

        Empleado emp = getEmpleadoLogueado(session);
        if (emp == null) return "redirect:/empleados/login";

        model.addAttribute("ventas", ventaRepository.findByEmpleadoId(emp.getId()));
        return "ventas/ventas_empleado";
    }

    /* ============================
           RESUMEN EMPLEADO
       ============================ */
    @GetMapping("/resumen")
    public String resumen(HttpSession session, Model model) {

        Empleado emp = getEmpleadoLogueado(session);
        if (emp == null) return "redirect:/empleados/login";

        Long id = emp.getId();

        BigDecimal totalHist = ventaRepository.obtenerTotalVendidoPorEmpleado(id);
        BigDecimal totalHoy = ventaRepository.obtenerTotalVendidoPorEmpleadoEntreFechas(
                id, LocalDate.now().atStartOfDay(), LocalDateTime.now());

        model.addAttribute("empleado", emp);
        model.addAttribute("totalHistorico", totalHist != null ? totalHist : BigDecimal.ZERO);
        model.addAttribute("totalHoy", totalHoy != null ? totalHoy : BigDecimal.ZERO);

        return "ventas/resumen_ventas";
    }

    /* ============================
           EXPORTAR EXCEL
       ============================ */
    @GetMapping("/exportar")
    public void exportar(HttpServletResponse response, HttpSession session) throws IOException {

        Empleado emp = getEmpleadoLogueado(session);
        if (emp == null || emp.getRolId() != 1) {
            response.getWriter().write("No autorizado.");
            return;
        }

        List<Venta> ventas = ventaRepository.findAll();

        if (ventas.isEmpty()) {
            response.getWriter().write("No hay ventas.");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=ventas.xlsx");

        new ExportarExcelVentas(ventas).exportar(response);
    }

   /* ============================
       EXPORTAR PDF FACTURA
   ============================ */
@GetMapping("/exportar-pdf/{id}")
public void exportarPdf(@PathVariable Long id,
                        HttpServletRequest request,
                        HttpServletResponse response) throws Exception {

    Optional<Venta> opt = ventaRepository.findById(id);
    if (opt.isEmpty()) {
        response.sendError(404, "Venta no encontrada");
        return;
    }

    Venta venta = opt.get();

    try {
        /* ============================
             BASE URL PARA IMÁGENES
           ============================ */
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? "" : ":" + request.getServerPort())
                + request.getContextPath();

        Context ctx = new Context();
        ctx.setVariable("venta", venta);
        ctx.setVariable("baseUrl", baseUrl);

        /* ============================
                 DATOS DE EMPRESA
           ============================ */
        ctx.setVariable("empresaNombre", "BJ.BYTES");
        ctx.setVariable("empresaDireccion", "Calle 123 - Bogotá");
        ctx.setVariable("empresaTelefono", "+57 300 000 0000");
        ctx.setVariable("empresaNIT", "NIT 123456789-0");

        /* ============================
              DATOS DEL CLIENTE
           ============================ */
        ctx.setVariable("clienteNombre", 
                venta.getCliente() != null ? venta.getCliente() : "Cliente Final");

        ctx.setVariable("clienteDocumento",
                venta.getClienteDocumento() != null ? venta.getClienteDocumento() : "");

        ctx.setVariable("clienteTelefono",
                venta.getClienteTelefono() != null ? venta.getClienteTelefono() : "");

        ctx.setVariable("clienteDireccion",
                venta.getClienteDireccion() != null ? venta.getClienteDireccion() : "");

        /* ============================
             ARMAR DETALLES DTO
           ============================ */
        List<DetalleFacturaDTO> detallesDto = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal ivaTotal = BigDecimal.ZERO;

        for (VentaDetalle d : venta.getDetalles()) {

            Producto prod = d.getInventario().getProducto();
            boolean exento = prod.getExento() != null && prod.getExento();

            DetalleFacturaDTO dto = new DetalleFacturaDTO();
            dto.setNombre(prod.getNombre());
            dto.setDescripcion(prod.getDescripcion() != null ? prod.getDescripcion() : "Producto vendido");
            dto.setCantidad(d.getCantidad());
            dto.setPrecio(prod.getPrecio());
            dto.setExento(exento);

            BigDecimal precio = prod.getPrecio();
            BigDecimal lineaSubtotal = precio.multiply(BigDecimal.valueOf(d.getCantidad()));

            BigDecimal iva = exento ? BigDecimal.ZERO : lineaSubtotal.multiply(new BigDecimal("0.19"));
            BigDecimal totalLinea = lineaSubtotal.add(iva);

            dto.setIva(iva);
            dto.setTotalLinea(totalLinea);

            subtotal = subtotal.add(lineaSubtotal);
            ivaTotal = ivaTotal.add(iva);

            detallesDto.add(dto);
        }

        ctx.setVariable("detallesDto", detallesDto);
        ctx.setVariable("subtotal", subtotal);
        ctx.setVariable("ivaTotal", ivaTotal);
        ctx.setVariable("totalPagar", subtotal.add(ivaTotal));

        /* ============================
               CARGAR LOGO BASE64
           ============================ */
        String[] rutas = {
                "static/images/logo.png",
                "static/images/logo.jpg",
                "images/logo.png",
                "images/logo.jpg",
                "logo.png",
                "logo.jpg"
        };

        for (String ruta : rutas) {
            try {
                ClassPathResource res = new ClassPathResource(ruta);
                if (res.exists()) {
                    byte[] bytes = res.getInputStream().readAllBytes();
                    String mime = ruta.endsWith(".png") ? "image/png" : "image/jpeg";

                    ctx.setVariable("logoBase64",
                            "data:" + mime + ";base64," +
                                    Base64.getEncoder().encodeToString(bytes));
                    break;
                }
            } catch (Exception ignored) {}
        }

        /* ============================
                GENERAR QR
           ============================ */
        try {
            String codigo = "Factura:" + venta.getId()
                    + "|Fecha:" + venta.getFecha()
                    + "|Total:" + venta.getTotalVenta();

            QRCodeWriter qr = new QRCodeWriter();
            BitMatrix matrix = qr.encode(codigo, BarcodeFormat.QR_CODE, 150, 150);

            ByteArrayOutputStream outQR = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outQR);

            ctx.setVariable("qrBase64",
                    "data:image/png;base64," +
                            Base64.getEncoder().encodeToString(outQR.toByteArray()));

        } catch (Exception e) {
            System.out.println("Error QR: " + e.getMessage());
        }

        /* ============================
               EXPORTAR PDF
           ============================ */
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=factura_" + venta.getId() + ".pdf");

        invoicePdfService.generatePdf(
                "invoice",
                ctx,
                baseUrl,
                response.getOutputStream()
        );

    } catch (Exception e) {

        if (!response.isCommitted()) {
            response.reset();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Error PDF: " + e.getMessage());
        }
    }
}


    /* ============================
       EXPORTAR MIS VENTAS PDF
       ============================ */
    @GetMapping("/exportar-mis-ventas-pdf")
    public void exportarMisVentasPDF(HttpServletResponse response,
                                     HttpSession session) throws IOException {

        Empleado empleado = getEmpleadoLogueado(session);
        if (empleado == null) {
            response.getWriter().write("Debe iniciar sesión.");
            return;
        }

        try {
            pdfService.exportarMisVentasPDF(response, session);
        } catch (Exception e) {
            response.getWriter().write("Error: " + e.getMessage());
        }
    }

    /* ============================
       RESUMEN INDIVIDUAL
       ============================ */
    @GetMapping("/resumen/{id}")
public String resumenVenta(
        @PathVariable Long id,
        Model model,
        RedirectAttributes ra) {

    Optional<Venta> opt = ventaRepository.findById(id);

    if (opt.isEmpty()) {
        ra.addFlashAttribute("error", "Venta no encontrada");
        return "redirect:/ventas/registrar";
    }

    Venta venta = opt.get();
    model.addAttribute("venta", venta);

    // ====== DATOS EMPRESA =======
    model.addAttribute("empresaNombre", "BJ.BYTES");
    model.addAttribute("empresaDireccion", "Calle 123 - Bogotá");
    model.addAttribute("empresaTelefono", "+57 300 000 0000");
    model.addAttribute("empresaNIT", "NIT 123456789-0");

    // ====== DATOS CLIENTE =======
    model.addAttribute("clienteNombre",
            venta.getCliente() != null ? venta.getCliente() : "Cliente Final");

    model.addAttribute("clienteDocumento",
            venta.getClienteDocumento() != null ? venta.getClienteDocumento() : "");

    model.addAttribute("clienteTelefono",
            venta.getClienteTelefono() != null ? venta.getClienteTelefono() : "");

    model.addAttribute("clienteDireccion",
            venta.getClienteDireccion() != null ? venta.getClienteDireccion() : "");

    // ====== LOGO BASE64 =======
    try {
        String[] rutas = {
                "static/images/logo.png",
                "static/images/logo.jpg",
                "images/logo.png",
                "images/logo.jpg",
                "logo.png",
                "logo.jpg"
        };

        for (String ruta : rutas) {
            ClassPathResource res = new ClassPathResource(ruta);
            if (res.exists()) {
                byte[] bytes = res.getInputStream().readAllBytes();
                String mime = ruta.endsWith(".png") ? "image/png" : "image/jpeg";

                model.addAttribute("logoBase64",
                        "data:" + mime + ";base64," +
                                Base64.getEncoder().encodeToString(bytes));
                break;
            }
        }

    } catch (Exception ignored) {}

    // ====== QR BASE64 =======
    try {
        QRCodeWriter qr = new QRCodeWriter();
        BitMatrix matrix = qr.encode(
                "Factura: " + venta.getId() +
                        "|Fecha: " + venta.getFecha() +
                        "|Total: " + venta.getTotalVenta(),
                BarcodeFormat.QR_CODE,
                150, 150);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);

        model.addAttribute("qrBase64",
                "data:image/png;base64," +
                        Base64.getEncoder().encodeToString(output.toByteArray()));

    } catch (Exception ignored) {}

    return "ventas/resumen_venta";
}


}
