/**
 * Convierte formato colombiano (1.234,56) a número decimal (1234.56)
 */
function parseColombianPrice(value) {
    if (!value) return 0;
    // Elimina puntos (miles) y reemplaza coma por punto
    return parseFloat(value.replace(/\./g, '').replace(',', '.'));
}

/**
 * Formatea número a formato colombiano (1234.56 -> 1.234,56)
 */
function formatColombianPrice(value) {
    const num = parseFloat(value);
    if (isNaN(num)) return '0,00';
    return num.toLocaleString('es-CO', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

/**
 * Valida que solo se ingresen números, puntos y comas
 */
function validatePriceInput(event) {
    const input = event.target;
    let value = input.value;
    
    // Solo permite dígitos, puntos y comas
    value = value.replace(/[^0-9.,]/g, '');
    
    // Evita múltiples puntos o comas
    const lastDot = value.lastIndexOf('.');
    const lastComma = value.lastIndexOf(',');
    
    if (lastDot > lastComma && lastDot !== -1) {
        // Hay más de un punto después de la coma, eliminar extras
        value = value.substring(0, lastDot + 4); // máximo 3 decimales después del punto
    } else if (lastComma > lastDot && lastComma !== -1) {
        // Hay más de una coma después del punto, eliminar extras
        value = value.substring(0, lastComma + 3); // máximo 2 decimales después de la coma
    }
    
    input.value = value;
}

/**
 * Limpia y formatea el precio al salir del input
 */
function formatPriceOnBlur(event) {
    const input = event.target;
    const colombianPrice = parseColombianPrice(input.value);
    input.value = formatColombianPrice(colombianPrice);
}