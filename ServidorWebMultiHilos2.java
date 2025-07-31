import java.io.*;
import java.net.*;
import java.util.*;

public final class ServidorWebMultiHilos2 {
    public static void main(String argv[]) throws Exception {
    
        int puerto = 6789;
        ServerSocket servidor = new ServerSocket(puerto);

       
        while (true) {
            Socket socketDeConexion = servidor.accept();

           
            SolicitudHttp solicitud = new SolicitudHttp(socketDeConexion);
            Thread hilo = new Thread(solicitud);

            hilo.start();
        }
    }
}

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    public SolicitudHttp(Socket socket) throws Exception {
        this.socket = socket;
    }
    public void run() {
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println("Error procesando solicitud: " + e);
        }
    }

    private void proceseSolicitud() throws Exception {
     
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

       
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String lineaDeSolicitud = br.readLine();

        if (lineaDeSolicitud == null) {
            
            os.close();
            br.close();
            socket.close();
            return;
        }

      
        System.out.println();
        System.out.println(lineaDeSolicitud);


        String lineaDelHeader = null;
        while ((lineaDelHeader = br.readLine()) != null && lineaDelHeader.length() != 0) {
            System.out.println(lineaDelHeader);
        }

        //---------------------------------------------- - - --

        // Extrae el nombre del archivo de la línea de solicitud.
        StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
        partesLinea.nextToken();  // "salta" el método (se asume "GET")
        String nombreArchivo = partesLinea.nextToken();

        // Anexa un ".", de tal forma que el archivo solicitado debe estar en el directorio actual.
        nombreArchivo = "." + nombreArchivo;

        // Abre el archivo seleccionado.
        FileInputStream fis = null;
        boolean existeArchivo = true;
        try {
            fis = new FileInputStream(nombreArchivo);
        } catch (FileNotFoundException e) {
            existeArchivo = false;
        }

        // Construye el mensaje de respuesta.
        String lineaDeEstado = null;
        String lineaDeTipoContenido = null;
        String cuerpoMensaje = null;

        if (existeArchivo) {
            lineaDeEstado = "HTTP/1.1 200 OK" + CRLF;
            lineaDeTipoContenido = "Content-type: " + contentType(nombreArchivo) + CRLF;
        } else {
            lineaDeEstado = "HTTP/1.1 404 Not Found" + CRLF;
            lineaDeTipoContenido = "Content-type: text/html" + CRLF;
            cuerpoMensaje = "<HTML>"
                    + "<HEAD><TITLE>404 Not Found</TITLE></HEAD>"
                    + "<BODY><h1>404 Not Found</h1><p>El recurso solicitado no existe.</p></BODY></HTML>";
        }

        // Envia la línea de estado.
        os.writeBytes(lineaDeEstado);

        // Envía el contenido de la línea content-type.
        os.writeBytes(lineaDeTipoContenido);

        // Envía una línea en blanco para indicar el final de las líneas de header.
        os.writeBytes(CRLF);

        // Envía el cuerpo del mensaje.
        if (existeArchivo) {
            enviarBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(cuerpoMensaje);
        }

        // Cierra los streams y el socket.
        os.close();
        br.close();
        socket.close();
    }

    private static void enviarBytes(FileInputStream fis, OutputStream os) throws Exception {
        // Construye un buffer de 1KB para guardar los bytes cuando van hacia el socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia el archivo solicitado hacia el output stream del socket.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String nombreArchivo) {
        if (nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
            return "text/html";
        }
        if (nombreArchivo.endsWith(".gif")) {
            return "image/gif";
        }
        if (nombreArchivo.endsWith(".jpg") || nombreArchivo.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        

        return "application/octet-stream";
    }
}
