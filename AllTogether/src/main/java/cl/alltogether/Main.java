package cl.alltogether;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

public class Main {
    private final Servicio servicio=new Servicio(new Repositorio());
    private final JFrame ventana=new JFrame("All Together");
    private Usuario actual;
    private final Color azul=new Color(35,91,164);
    public static void main(String[] args) { SwingUtilities.invokeLater(()->{
        try {new Main().iniciar();} catch(Exception e){JOptionPane.showMessageDialog(null,e.getMessage(),"Error al iniciar",JOptionPane.ERROR_MESSAGE);}
    });}
    private void iniciar(){ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);ventana.setSize(750,530);ventana.setMinimumSize(new Dimension(630,450));ventana.setLocationRelativeTo(null);bienvenida();ventana.setVisible(true);}
    private JPanel panel(){JPanel p=new JPanel();p.setBorder(BorderFactory.createEmptyBorder(25,35,25,35));p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBackground(Color.WHITE);return p;}
    private JLabel titulo(String texto){JLabel l=new JLabel(texto);l.setFont(new Font("SansSerif",Font.BOLD,23));l.setForeground(azul);l.setAlignmentX(Component.CENTER_ALIGNMENT);return l;}
    private JButton boton(String texto,Runnable accion){JButton b=new JButton(texto);b.setFont(new Font("SansSerif",Font.PLAIN,16));b.setMaximumSize(new Dimension(380,44));b.setAlignmentX(Component.CENTER_ALIGNMENT);b.addActionListener(e->accion.run());return b;}
    private void mostrar(JPanel p){ventana.setContentPane(p);ventana.revalidate();ventana.repaint();}
    private void error(Exception ex){JOptionPane.showMessageDialog(ventana,ex.getMessage(),"Aviso",JOptionPane.WARNING_MESSAGE);}
    private void bienvenida(){JPanel p=panel();p.add(Box.createVerticalGlue());p.add(titulo("ALL TOGETHER"));p.add(Box.createVerticalStrut(18));p.add(boton("Iniciar sesión",this::login));p.add(Box.createVerticalStrut(12));p.add(boton("Registrarse",this::registro));p.add(Box.createVerticalGlue());mostrar(p);}
    private JPanel formulario(String nombre){JPanel p=panel();p.add(titulo(nombre));p.add(Box.createVerticalStrut(18));return p;}
    private void campo(JPanel p,String etiqueta,JComponent c){JLabel l=new JLabel(etiqueta);l.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(l);c.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));c.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(c);p.add(Box.createVerticalStrut(9));}
    private void registro(){JPanel p=formulario("Crear cuenta");JTextField nombre=new JTextField(),correo=new JTextField();JPasswordField clave=new JPasswordField(),confirmar=new JPasswordField();JComboBox<String> rol=new JComboBox<>(new String[]{"Familiar","Cuidador"});
        campo(p,"Nombre completo",nombre);campo(p,"Correo electrónico",correo);campo(p,"Contraseña (mínimo 8 caracteres)",clave);campo(p,"Confirmar contraseña",confirmar);campo(p,"Tipo de cuenta",rol);
        p.add(boton("Crear cuenta",()->{char[] a=clave.getPassword(),b=confirmar.getPassword();try{
            if(!Arrays.equals(a,b))throw new IllegalArgumentException("Las contraseñas no coinciden.");
            Usuario u=servicio.registrar(nombre.getText(),correo.getText(),a,(String)rol.getSelectedItem());
            JOptionPane.showMessageDialog(ventana,"Cuenta creada. "+(u.rol.equals("Cuidador")?"Tu código de vinculación es: "+u.codigoVinculacion:"Ya puedes iniciar sesión."));login();
        }catch(Exception ex){error(ex);}finally{Arrays.fill(a,'\0');Arrays.fill(b,'\0');}}));
        p.add(Box.createVerticalStrut(10));p.add(boton("Volver",this::bienvenida));mostrar(p);
    }
    private void login(){JPanel p=formulario("Iniciar sesión");JTextField correo=new JTextField();JPasswordField clave=new JPasswordField();campo(p,"Correo electrónico",correo);campo(p,"Contraseña",clave);
        p.add(boton("Ingresar",()->{char[] a=clave.getPassword();try{actual=servicio.entrar(correo.getText(),a);inicioUsuario();}catch(Exception ex){error(ex);}finally{Arrays.fill(a,'\0');}}));
        p.add(Box.createVerticalStrut(12));p.add(boton("Crear cuenta",this::registro));p.add(boton("Volver",this::bienvenida));mostrar(p);
    }
    private void inicioUsuario(){JPanel p=formulario("Hola, "+actual.nombre);JLabel rol=new JLabel("Rol: "+actual.rol);rol.setAlignmentX(Component.CENTER_ALIGNMENT);p.add(rol);p.add(Box.createVerticalStrut(20));
        if(actual.rol.equals("Familiar")){
            p.add(boton("Vincular cuidador",this::vincular));p.add(Box.createVerticalStrut(10));p.add(boton("Crear solicitud",this::nuevaSolicitud));
        }else{
            JLabel codigo=new JLabel("Tu código para familiares: "+actual.codigoVinculacion);codigo.setAlignmentX(Component.CENTER_ALIGNMENT);p.add(codigo);p.add(Box.createVerticalStrut(12));
        }
        p.add(Box.createVerticalStrut(10));p.add(boton("Ver solicitudes e historial",this::solicitudes));p.add(Box.createVerticalStrut(15));p.add(boton("Cerrar sesión",()->{actual=null;bienvenida();}));mostrar(p);
    }
    private void vincular(){String codigo=JOptionPane.showInputDialog(ventana,"Introduce el código de 6 dígitos del cuidador:");if(codigo==null)return;try{servicio.vincular(actual,codigo);JOptionPane.showMessageDialog(ventana,"Cuidador vinculado.");}catch(Exception e){error(e);}}
    private void nuevaSolicitud(){JPanel p=formulario("Nueva solicitud");JComboBox<String> tipo=new JComboBox<>(new String[]{"Compras","Medicamentos","Transporte","Compañía","Otros"});JTextArea descripcion=new JTextArea(5,35);descripcion.setLineWrap(true);descripcion.setWrapStyleWord(true);JComboBox<String> prioridad=new JComboBox<>(new String[]{"Baja","Media","Alta"});
        campo(p,"Tipo de ayuda",tipo);JLabel l=new JLabel("Descripción");l.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(l);JScrollPane scroll=new JScrollPane(descripcion);scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE,130));p.add(scroll);p.add(Box.createVerticalStrut(12));campo(p,"Prioridad",prioridad);
        p.add(boton("Enviar solicitud",()->{try{servicio.crear(actual,(String)tipo.getSelectedItem(),descripcion.getText(),(String)prioridad.getSelectedItem());JOptionPane.showMessageDialog(ventana,"Solicitud creada.");inicioUsuario();}catch(Exception e){error(e);}}));
        p.add(Box.createVerticalStrut(10));p.add(boton("Volver",this::inicioUsuario));mostrar(p);
    }
    private void solicitudes(){JPanel p=formulario("Solicitudes");java.util.List<Solicitud> lista=servicio.visibles(actual);DefaultTableModel modelo=new DefaultTableModel(new String[]{"ID","Tipo","Prioridad","Estado","Fecha"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        for(Solicitud s:lista)modelo.addRow(new Object[]{s.id,s.tipo,s.prioridad,s.estado,s.creada});JTable tabla=new JTable(modelo);tabla.setRowHeight(27);tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);JScrollPane sc=new JScrollPane(tabla);p.add(sc);p.add(Box.createVerticalStrut(10));
        p.add(boton("Ver detalle e historial",()->{int i=tabla.getSelectedRow();if(i<0){JOptionPane.showMessageDialog(ventana,"Selecciona una solicitud.");return;}Solicitud s=lista.get(tabla.convertRowIndexToModel(i));detalle(s);}));
        p.add(Box.createVerticalStrut(10));p.add(boton("Volver",this::inicioUsuario));mostrar(p);
    }
    private void detalle(Solicitud s){JPanel p=formulario("Solicitud #"+s.id);JTextArea info=new JTextArea("Tipo: "+s.tipo+"\nDescripción: "+s.descripcion+"\nPrioridad: "+s.prioridad+"\nEstado: "+s.estado+"\nCreada: "+s.creada+"\nÚltima actualización: "+s.actualizada);info.setEditable(false);info.setLineWrap(true);info.setWrapStyleWord(true);p.add(new JScrollPane(info));p.add(Box.createVerticalStrut(10));
        StringBuilder eventos=new StringBuilder("HISTORIAL\n");for(Evento e:servicio.historial(actual,s))eventos.append(e.fecha).append(" - ").append(e.accion).append("\n");JTextArea historia=new JTextArea(eventos.toString(),6,30);historia.setEditable(false);p.add(new JScrollPane(historia));p.add(Box.createVerticalStrut(10));
        if(actual.rol.equals("Cuidador")&&!s.estado.equals("Atendida")){String siguiente=s.estado.equals("Pendiente")?"Recibida":"Atendida";p.add(boton("Marcar como "+siguiente,()->{try{servicio.cambiarEstado(actual,s,siguiente);detalle(s);}catch(Exception e){error(e);}}));p.add(Box.createVerticalStrut(10));}
        p.add(boton("Volver a solicitudes",this::solicitudes));mostrar(p);
    }
}
