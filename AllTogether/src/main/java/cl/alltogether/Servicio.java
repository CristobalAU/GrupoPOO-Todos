package cl.alltogether;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Servicio {
    public final Repositorio repo;
    private final SecureRandom random=new SecureRandom();
    private final DateTimeFormatter formato=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public Servicio(Repositorio repo) { this.repo=repo; }
    private String ahora(){return LocalDateTime.now().format(formato);}
    private String hash(char[] clave,byte[] sal) {
        PBEKeySpec spec=new PBEKeySpec(clave,sal,210_000,256);
        try {
            byte[] h=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch(Exception e){throw new IllegalStateException(e);} finally {spec.clearPassword();}
    }
    public Usuario registrar(String nombre,String correo,char[] clave,String rol) {
        nombre=nombre.trim(); correo=correo.trim().toLowerCase(Locale.ROOT);
        if(nombre.isBlank()||!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Ingresa nombre y correo válidos.");
        if(clave.length<8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        if(!rol.equals("Familiar")&&!rol.equals("Cuidador")) throw new IllegalArgumentException("Rol inválido.");
        for(Usuario u:repo.usuarios) if(u.correo.equalsIgnoreCase(correo)) throw new IllegalArgumentException("Ese correo ya está registrado.");
        byte[] sal=new byte[16]; random.nextBytes(sal);
        int id=repo.usuarios.stream().mapToInt(u->u.id).max().orElse(0)+1;
        String codigo=rol.equals("Cuidador")?String.format("%06d",random.nextInt(1_000_000)):null;
        Usuario u=new Usuario(id,nombre,correo,hash(clave,sal),Base64.getEncoder().encodeToString(sal),rol,codigo);
        repo.usuarios.add(u); repo.guardarUsuarios(); return u;
    }
    public Usuario entrar(String correo,char[] clave) {
        for(Usuario u:repo.usuarios) if(u.correo.equalsIgnoreCase(correo.trim())) {
            byte[] sal=Base64.getDecoder().decode(u.sal);
            String calculado=hash(clave,sal);
            if(java.security.MessageDigest.isEqual(calculado.getBytes(java.nio.charset.StandardCharsets.UTF_8),u.claveHash.getBytes(java.nio.charset.StandardCharsets.UTF_8))) return u;
        }
        throw new IllegalArgumentException("Correo o contraseña incorrectos.");
    }
    public void vincular(Usuario familiar,String codigo) {
        if(!familiar.rol.equals("Familiar")) throw new IllegalArgumentException("Solo un familiar puede vincularse.");
        Usuario cuidador=repo.usuarios.stream().filter(u->u.rol.equals("Cuidador")&&u.codigoVinculacion.equals(codigo.trim())).findFirst().orElseThrow(()->new IllegalArgumentException("Código no encontrado."));
        familiar.cuidadorId=cuidador.id;
        repo.guardarUsuarios();
    }
    public Solicitud crear(Usuario familiar,String tipo,String descripcion,String prioridad) {
        if(!familiar.rol.equals("Familiar")) throw new IllegalArgumentException("Solo familiares crean solicitudes.");
        if(familiar.cuidadorId==null) throw new IllegalArgumentException("Primero vincula un cuidador.");
        if(descripcion.trim().isBlank()) throw new IllegalArgumentException("Escribe una descripción.");
        int id=repo.solicitudes.stream().mapToInt(s->s.id).max().orElse(0)+1;
        Solicitud s=new Solicitud(id,familiar.id,familiar.cuidadorId,tipo,descripcion.trim(),prioridad,ahora());
        repo.solicitudes.add(s); repo.guardarSolicitudes();
        repo.eventos.add(new Evento(s.id,familiar.id,ahora(),"Solicitud creada")); repo.guardarEventos(); return s;
    }
    public List<Solicitud> visibles(Usuario usuario) {
        List<Solicitud> lista=new ArrayList<>();
        for(Solicitud s:repo.solicitudes) if(usuario.rol.equals("Familiar")?s.familiarId==usuario.id:Objects.equals(s.cuidadorId,usuario.id)) lista.add(s);
        lista.sort(Comparator.comparingInt((Solicitud s)->s.id).reversed());return lista;
    }
    public void cambiarEstado(Usuario cuidador,Solicitud s,String nuevo) {
        if(!cuidador.rol.equals("Cuidador")||!Objects.equals(s.cuidadorId,cuidador.id)) throw new IllegalArgumentException("Sin permiso.");
        if(!((s.estado.equals("Pendiente")&&nuevo.equals("Recibida"))||(s.estado.equals("Recibida")&&nuevo.equals("Atendida"))))
            throw new IllegalArgumentException("Transición inválida: primero recibir y luego atender.");
        s.estado=nuevo; s.actualizada=ahora();repo.guardarSolicitudes();
        repo.eventos.add(new Evento(s.id,cuidador.id,ahora(),"Estado actualizado: "+nuevo));repo.guardarEventos();
    }
    public List<Evento> historial(Usuario usuario,Solicitud s) {
        if(!visibles(usuario).contains(s)) throw new IllegalArgumentException("Sin permiso.");
        List<Evento> lista=new ArrayList<>();
        for(Evento e:repo.eventos) if(e.solicitudId==s.id) lista.add(e);
        return lista;
    }
}
