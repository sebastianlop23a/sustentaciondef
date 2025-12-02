package SCRUM3.Bj_Byte.factory;

import SCRUM3.Bj_Byte.model.Producto;
import java.math.BigDecimal;

public class ProductoFactory {
    public static Producto crearProducto(String nombre, String descripcion, BigDecimal precio, String cvv) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(precio); 
        producto.setCvv(cvv);
        return producto;
    }
    
    // Sobrecarga para carga masiva desde CSV con precioBase
    public static Producto crearProductoConPrecioBase(String nombre, String descripcion, BigDecimal precio, BigDecimal precioBase) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(precio);
        producto.setPrecioBase(precioBase);
        return producto;
    }
}
