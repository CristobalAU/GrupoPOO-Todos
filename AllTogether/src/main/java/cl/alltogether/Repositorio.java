package cl.alltogether;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Repositorio {
    private final Gson gson=new GsonBuilder().setPrettyPrinting().create();
    private final Path carpeta; // <--- 1. Ya no se inicializa aquí
    private final Type usuariosTipo=new TypeToken<ArrayList<Usuario>>(){}.getType();
    private final Type solicitudesTipo=new TypeToken<ArrayList<Solicitud>>(){}.getType();
    private final Type eventosTipo=new TypeToken<ArrayList<Evento>>(){}.getType();
    public final List<Usuario> usuarios;
    public final List<Solicitud> solicitudes;
    public final List<Evento> eventos;


    // 2. Este es tu constructor original. Ahora simplemente llama al nuevo constructor.
    public Repositorio() {
        this(Paths.get("data"));
    }

    // 3. Este es el NUEVO constructor que usaremos en las pruebas
    public Repositorio(Path carpeta) {
        this.carpeta = carpeta;
        try { Files.createDirectories(carpeta); } catch(IOException e) { throw new IllegalStateException(e); }
        usuarios=leer("usuarios.json",usuariosTipo);
        solicitudes=leer("solicitudes.json",solicitudesTipo);
        eventos=leer("historial.json",eventosTipo);
    }

    // ... El resto de tus métodos (leer, escribir, guardarUsuarios, etc.) se quedan EXACTAMENTE IGUAL
    private <T> List<T> leer(String nombre,Type tipo) {
        Path ruta=carpeta.resolve(nombre);
        if (!Files.exists(ruta)) return new ArrayList<>();
        try(Reader r=Files.newBufferedReader(ruta,StandardCharsets.UTF_8)) {
            List<T> resultado=gson.fromJson(r,tipo);
            return resultado==null?new ArrayList<>():resultado;
        } catch(Exception e) { throw new IllegalStateException("No se pudo leer "+ruta+": "+e.getMessage(),e); }
    }
    private void escribir(String nombre,Object datos) {
        Path destino=carpeta.resolve(nombre);
        try {
            Path temporal=Files.createTempFile(carpeta,"all-together-",".tmp");
            try(Writer w=Files.newBufferedWriter(temporal,StandardCharsets.UTF_8)) { gson.toJson(datos,w); }
            try { Files.move(temporal,destino,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temporal,destino,StandardCopyOption.REPLACE_EXISTING); }
        } catch(IOException e) { throw new IllegalStateException("Error al guardar "+nombre,e); }
    }
    public void guardarUsuarios(){ escribir("usuarios.json",usuarios); }
    public void guardarSolicitudes(){ escribir("solicitudes.json",solicitudes); }
    public void guardarEventos(){ escribir("historial.json",eventos); }

}
