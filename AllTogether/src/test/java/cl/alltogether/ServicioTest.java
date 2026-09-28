package cl.alltogether;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ServicioTest {

    private Servicio servicio;
    private Repositorio repo;

    // @TempDir crea una carpeta temporal que se borra al terminar las pruebas para no tocar datos reales.
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // se inicia el repositorio apuntando a la carpeta temporal
        repo = new Repositorio(tempDir);
        servicio = new Servicio(repo);
    }


    // PRUEBAS DE REGISTRO Y SEGURIDAD

    @Test
    @DisplayName("Registro exitoso de un Cuidador y generación de código")
    void testRegistroExitoso() {
        Usuario cuidador = servicio.registrar("Juan Perez", "juan@correo.cl", "password123".toCharArray(), "Cuidador");

        assertNotNull(cuidador);
        assertEquals("Juan Perez", cuidador.nombre);
        assertEquals("Cuidador", cuidador.rol);
        assertNotNull(cuidador.codigoVinculacion); // Los cuidadores deben tener código
        assertEquals(1, repo.usuarios.size()); // Se guardó en el repositorio
    }

    @Test
    @DisplayName("Falla el registro si el correo ya existe")
    void testRegistroCorreoDuplicado() {
        servicio.registrar("Usuario 1", "test@correo.cl", "password123".toCharArray(), "Familiar");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            servicio.registrar("Usuario 2", "test@correo.cl", "otraclave123".toCharArray(), "Cuidador");
        });

        assertTrue(exception.getMessage().contains("correo ya está registrado"));
    }

    @Test
    @DisplayName("Falla el registro si la contraseña tiene menos de 8 caracteres")
    void testRegistroClaveCorta() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            servicio.registrar("Pedro", "pedro@correo.cl", "corta".toCharArray(), "Familiar");
        });
        assertTrue(exception.getMessage().contains("al menos 8 caracteres"));
    }

    @Test
    @DisplayName("Falla el registro si el correo tiene formato inválido")
    void testRegistroCorreoInvalido() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            servicio.registrar("Ana", "correo-sin-arroba", "password123".toCharArray(), "Familiar");
        });
        assertTrue(exception.getMessage().contains("correo válidos"));
    }

    @Test
    @DisplayName("Inicio de sesión exitoso y fallido")
    void testLogin() {
        servicio.registrar("Maria", "maria@correo.cl", "claveSegura123".toCharArray(), "Familiar");

        // Login correcto
        Usuario logueado = servicio.entrar("maria@correo.cl", "claveSegura123".toCharArray());
        assertNotNull(logueado);
        assertEquals("Maria", logueado.nombre);

        // Login incorrecto
        assertThrows(IllegalArgumentException.class, () -> {
            servicio.entrar("maria@correo.cl", "claveIncorrecta".toCharArray());
        });
    }

    // ==========================================
    // PRUEBAS DE VINCULACIÓN Y SOLICITUDES
    // ==========================================

    @Test
    @DisplayName("Vincular Familiar con Cuidador usando código")
    void testVincularCuidador() {
        Usuario cuidador = servicio.registrar("Carlos", "carlos@correo.cl", "password123".toCharArray(), "Cuidador");
        Usuario familiar = servicio.registrar("Lucia", "lucia@correo.cl", "password123".toCharArray(), "Familiar");

        servicio.vincular(familiar, cuidador.codigoVinculacion);

        assertNotNull(familiar.cuidadorId);
        assertEquals(cuidador.id, familiar.cuidadorId);
    }

    @Test
    @DisplayName("Falla al crear solicitud si el familiar no tiene cuidador vinculado")
    void testCrearSolicitudSinVinculacion() {
        Usuario familiar = servicio.registrar("Lucia", "lucia@correo.cl", "password123".toCharArray(), "Familiar");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            servicio.crear(familiar, "Compras", "Comprar pan", "Media");
        });
        assertTrue(exception.getMessage().contains("Primero vincula un cuidador"));
    }

    @Test
    @DisplayName("Crear solicitud exitosamente cuando hay vinculación")
    void testCrearSolicitudExitosa() {
        Usuario cuidador = servicio.registrar("Carlos", "carlos@correo.cl", "password123".toCharArray(), "Cuidador");
        Usuario familiar = servicio.registrar("Lucia", "lucia@correo.cl", "password123".toCharArray(), "Familiar");
        servicio.vincular(familiar, cuidador.codigoVinculacion);

        Solicitud s = servicio.crear(familiar, "Medicamentos", "Comprar remedios", "Alta");

        assertNotNull(s);
        assertEquals("Pendiente", s.estado); // Regla de negocio: siempre nace Pendiente
        assertEquals(familiar.id, s.familiarId);
        assertEquals(cuidador.id, s.cuidadorId);
        assertEquals(1, repo.solicitudes.size());
    }


    // PRUEBAS DE TRANSICIÓN DE ESTADOS (Maquina de estados)

    @Test
    @DisplayName("Flujo completo de estados: Pendiente -> Recibida -> Atendida")
    void testFlujoEstadosExitoso() {
        Usuario cuidador = servicio.registrar("Carlos", "carlos@correo.cl", "password123".toCharArray(), "Cuidador");
        Usuario familiar = servicio.registrar("Lucia", "lucia@correo.cl", "password123".toCharArray(), "Familiar");
        servicio.vincular(familiar, cuidador.codigoVinculacion);
        Solicitud s = servicio.crear(familiar, "Transporte", "Llevar al médico", "Alta");

        // 1. El cuidador la recibe
        servicio.cambiarEstado(cuidador, s, "Recibida");
        assertEquals("Recibida", s.estado);

        // 2. El cuidador la atiende
        servicio.cambiarEstado(cuidador, s, "Atendida");
        assertEquals("Atendida", s.estado);

        // 3. Verificar que se generó el historial
        List<Evento> historial = servicio.historial(familiar, s);
        assertEquals(3, historial.size()); // Creación + Recibida + Atendida
    }

    @Test
    @DisplayName("Falla al intentar saltar de Pendiente a Atendida directamente")
    void testTransicionEstadoInvalida() {
        Usuario cuidador = servicio.registrar("Carlos", "carlos@correo.cl", "password123".toCharArray(), "Cuidador");
        Usuario familiar = servicio.registrar("Lucia", "lucia@correo.cl", "password123".toCharArray(), "Familiar");
        servicio.vincular(familiar, cuidador.codigoVinculacion);
        Solicitud s = servicio.crear(familiar, "Otros", "Ayuda", "Baja");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            servicio.cambiarEstado(cuidador, s, "Atendida"); // ¡Error! Debe pasar primero por "Recibida"
        });

        assertTrue(exception.getMessage().contains("Transición inválida"));
    }

    @Test
    @DisplayName("Un cuidador no puede cambiar el estado de una solicitud ajena")
    void testPermisosCambioEstado() {
        Usuario cuidador1 = servicio.registrar("Carlos", "carlos@correo.cl", "password123".toCharArray(), "Cuidador");
        Usuario cuidador2 = servicio.registrar("Ana", "ana@correo.cl", "password123".toCharArray(), "Cuidador");
        Usuario familiar = servicio.registrar("Lucia", "lucia@correo.cl", "password123".toCharArray(), "Familiar");

        servicio.vincular(familiar, cuidador1.codigoVinculacion);
        Solicitud s = servicio.crear(familiar, "Compras", "Pan", "Media");

        // El cuidador 2 intenta cambiar el estado de la solicitud del cuidador 1
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            servicio.cambiarEstado(cuidador2, s, "Recibida");
        });

        assertTrue(exception.getMessage().contains("Sin permiso"));
    }
}