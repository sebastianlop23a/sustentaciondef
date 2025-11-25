package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer {

    @Autowired
    private ProductoRepository productoRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Buscar productos con activo NULL o false y ponerlos en true
        List<Producto> productos = productoRepository.findAll();
        boolean changed = false;
        for (Producto p : productos) {
            if (p.getActivo() == null || p.getActivo().equals(Boolean.FALSE)) {
                p.setActivo(Boolean.TRUE);
                changed = true;
            }
        }
        if (changed) {
            productoRepository.saveAll(productos);
            System.out.println("DataInitializer: productos corregidos a activo=true");
        } else {
            System.out.println("DataInitializer: no se requieren cambios");
        }
    }
}
