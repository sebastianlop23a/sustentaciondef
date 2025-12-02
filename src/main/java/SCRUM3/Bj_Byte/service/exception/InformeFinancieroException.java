package SCRUM3.Bj_Byte.service.exception;

/**
 * Excepción personalizada para errores en la generación de informes financieros.
 * Hereda de RuntimeException para permitir manejo flexible en la capa de aplicación.
 */
public class InformeFinancieroException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String codigo;
    private final String detalles;

    public InformeFinancieroException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = null;
    }

    public InformeFinancieroException(String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
        this.detalles = null;
    }

    public InformeFinancieroException(String codigo, String mensaje, String detalles, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDetalles() {
        return detalles;
    }
}
