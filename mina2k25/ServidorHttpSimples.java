import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ServidorHttpSimples {
    public static void main(String[] args) throws Exception {
        // Cria servidor HTTP escutando na porta 8080
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);

        /* INICIO DO CÓDIGO */
        servidor.createContext("/", troca -> {
            enviarArquivo(troca, "index.html", "text/html");
        });

        servidor.createContext("/estilo.css", troca -> {
            enviarArquivo(troca, "estilo.css", "text/css");
        });

        servidor.createContext("/comum", troca -> {
            enviarArquivo(troca, "comum.html", "text/html");
        });

        servidor.createContext("/adm", troca -> {
            enviarArquivo(troca, "adm.html", "text/html");
        });

        servidor.createContext("/perfil", troca -> {
            enviarArquivo(troca, "perfil.html", "text/html");
        });

        servidor.createContext("/login", troca -> {
            String query = troca.getRequestURI().getQuery() ;//usuario=Ana&senha=123
            String partes[]=query.split("&");
            String usuario= partes[0].replaceAll("usuario=", "") ;
            String senha= partes[1].replaceAll("senha=", "");
            String perfil= partes[2].replaceAll("perfil=", "");
            System.out.println(usuario);
            System.out.println(senha);
            System.out.println(query);

            if(usuario.equals("carol") && senha.equals("123") && perfil.equals("Adm")){
                System.out.println("Acesso autorizado");
                troca.getResponseHeaders().set("location", "/adm");
                troca.sendResponseHeaders(302, -1);
            }

            else if (usuario.equals("carol") && senha.equals("123") && perfil.equals("Usuario")){
                System.out.println("Acesso autorizado");
                troca.getResponseHeaders().set("location", "/comum");
                troca.sendResponseHeaders(302, -1);}
            else{
                System.out.println("Acesso negado");
            } });



        /* FIM DO CÓDIGO */

        servidor.start();
        System.out.println("Servidor rodando em http://localhost:8080/");
    }

    // Envia um arquivo (HTML ou CSS)
    private static void enviarArquivo(com.sun.net.httpserver.HttpExchange troca, String caminho, String tipo) throws IOException {
        File arquivo = new File("src/" + caminho);
        if (!arquivo.exists()) {
            System.out.println("Arquivo não encontrado: " + arquivo.getAbsolutePath());
        }
        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        troca.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = troca.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Envia resposta HTML gerada no código
    private static void enviarTexto(com.sun.net.httpserver.HttpExchange troca, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = troca.getResponseBody()) {
            os.write(bytes);
        }
    }
}