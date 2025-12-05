package java;// Importações de bibliotecas necessárias para o funcionamento do servidor HTTP, leitura e gravação de dados
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;                                       // Para manipulação de arquivos
import java.net.InetSocketAddress;                      // Para definir o endereço do servidor
import java.net.URLDecoder;                             // Para decodificar parâmetros URL
import java.nio.charset.StandardCharsets;               // Para trabalhar com codificação de caracteres
import java.sql.*;                                      // Para manipulação do banco de dados

public class Servidor {

    private static Connection con;

    public static void main(String[] args) throws Exception {

        // Conectar ao SQLite (arquivo conteudo.db na pasta do projeto)
        con = DriverManager.getConnection("jdbc:sqlite:conteudo.db");

        // Criar tabela no Banco de dados (se não existir)
        String sql = "CREATE TABLE IF NOT EXISTS dados (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nome TEXT," +
                "descricao TEXT," +
                "data TEXT," +
                "curtida TEXT" +
                ")";
        con.createStatement().execute(sql);

        // Criar servidor HTTP
        HttpServer s = HttpServer.create(new InetSocketAddress(8082), 0);

        // Rotas básicas
        s.createContext("/", t -> enviar(t, "java/telainicial.html"));
        s.createContext("/login",  Servidor::login);     // cadastro de atividades
        s.createContext("/cadastro", Servidor::cadastro);     // cadastro de atividades
        s.createContext("/telaaluno.html", t -> enviar(t, "telaaluno.html"));
        s.createContext("/java/telaprof.html", t -> enviar(t, "java/telaprof.html"));
        s.createContext("/java/sobrenos.html", t -> enviar(t, "java/sobrenos.html"));
        s.createContext("/concluido-aluno", Servidor::concluido_aluno);       // Concluido
        s.createContext("/concluido_professor", Servidor::concluido_professor);       //Não concluido
        s.createContext("/excluir", Servidor::excluir);       //excluir atividades
        //s.createContext("/acesso-professor", t -> enviar(t, "acesso-professor.html"));   // Professor

        //Rotas de estilo
        s.createContext("/css/style.css", t -> enviarCSS(t, "css/style.css")); // CSS
        s.createContext("/css/login.css", t -> enviarCSS(t, "css/login.css")); // CSS
        s.createContext("/css/geral.css", t -> enviarCSS(t, "css/geral.css")); // CSS
        s.createContext("/css/atividades.css", t -> enviarCSS(t, "css/atividades.css")); // CSS
        s.createContext("/css/listar.css", t -> enviarCSS(t, "css/listar.css")); // CSS

        s.start();
        System.out.println("Servidor Iniciado");
        System.out.println("Rodando em http://localhost:8082/");
    }


    // -------------------- Função de cadastro de atividades --------------------
    private static void login(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getQuery();

        if (query == null) {
            System.out.println("Sem parâmetros na URL");
            enviar(t, "/erro.html");
            return;
        }

        String[] partes = query.split("&");

        String usuario = partes[0].split("=")[1];
        String senha   = partes[1].split("=")[1];
        String perfil  = partes[2].split("=")[1];

        usuario = URLDecoder.decode(usuario, "UTF-8");
        senha   = URLDecoder.decode(senha, "UTF-8");
        perfil  = URLDecoder.decode(perfil, "UTF-8");

        System.out.println(usuario);
        System.out.println(senha);
        System.out.println(perfil);

        if (usuario.equals("carol") && senha.equals("123") && perfil.equals("professor")) {
            enviar(t, "/java/telaprof.html");
        } else if (usuario.equals("carol") && senha.equals("123") && perfil.equals("aluno")) {
            enviar(t, "/aluno");
        } else {
            System.out.println("Acesso negado");
            enviar(t, "/");
        }
    }



    private static void cadastro(HttpExchange t) throws IOException {

        if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
            enviar(t, "/acesso-professor.html"); //envia aquivo do professor
            return;
        }

        String c = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);

        String nome = pega(c, "nome"); //pega nome escrito
        String desc = pega(c, "descricao"); //pega descrição da atividade
        String data = pega(c, "data"); //pega data da atividade

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO dados (nome, descricao, data, curtida) VALUES (?,?,?,?)")) { //insere dados no banco de dados

            ps.setString(1, nome);
            ps.setString(2, desc);
            ps.setString(3, data);
            ps.setString(4, "Não Concluido"); // ainda não curtido
            ps.executeUpdate(); //atualiza o banco de dados

        } catch (SQLException e) {
            e.printStackTrace();
        }

        redirecionar(t, "/cadastro");
        System.out.println("-------------------------------------");
        System.out.println("Cadastro realizado");
        System.out.println(" ");
        System.out.println("Atividade: "+nome);
        System.out.println("Descrição: "+desc);
        System.out.println("Data:: "+data);
        System.out.println("-------------------------------------");
    }

    // -------------------- Aluno (lista todos os cards) --------------------

    private static void aluno(HttpExchange t) throws IOException {
        StringBuilder html = new StringBuilder();

        //Insere arquivo HTMl do aluno (mesma coisa que HTML padrão soq colocado a partir do JAVA)

        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>AcademyFlow | Mural de Atividades</title>");
        html.append("<link rel=\"stylesheet\" href=\"/css/atividades.css\">");
        html.append("<link rel=\"stylesheet\" href=\"/css/geral.css\">");
        html.append("<link rel=\"icon\" href=\"/img/academyflow-logo.png\">");
        html.append("</head><body>");
        html.append("<header>");
        html.append("<div class=\"logo-header\">");
        html.append("<a href=\"#\">");
        html.append("<img src=\"/academyflow-nome.png\" alt=\"Logo AcademyFlow\">");
        html.append("</a>");
        html.append("</div>");

        html.append("<div class=\"nav\">");
        html.append("<div class=\"container-btn-header\">");
        html.append("<h4>Bem-Vindo Aluno</h4>");
        html.append("<p id=\"nome-usuario\"></p>");
        html.append("</div>");

        html.append("<div class=\"buttons\">");
        html.append("<button class=\"dark-mode-button\" id=\"toggle-theme\" aria-label=\"Alternar modo escuro\">");
        html.append("<img class=\"dark-theme-img\" src=\"/img/light-theme.svg\" alt=\"Ícone Claro\">");
        html.append("<img class=\"light-theme-img\" src=\"/img/dark-theme.svg\" alt=\"Ícone Escuro\">");
        html.append("</button>");
        html.append("</div>");
        html.append("</div>");
        html.append("</header>");

        html.append("<main>");
        html.append("<section class=\"titulo\">");
        html.append("<div class=\"tituloh1\">");
        html.append("<h1>Mural de Atividades</h1>");
        html.append("</div>");
        html.append("</section>");

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nome, descricao, data, curtida FROM dados ORDER BY id DESC")) {
            //Organiza os arquivos a partir do ID

            boolean vazio = true;

            while (rs.next()) {
                vazio = false;

                //Insere o conteudo do Banco de Dados em variaveis
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String desc = rs.getString("descricao");
                String data = rs.getString("data");
                String curtida = rs.getString("curtida");

                // Trocar a cor se concluido ou não
                String classeExtra = "bloco";
                if ("Concluido".equals(curtida)) {
                    classeExtra = "card-curtido";
                } else if ("nao".equals(curtida)) {
                    classeExtra = "bloco";
                }
                //Insere o conteudo do Banco de dados em blocos no HTML
                html.append("<div class=\"")
                        .append(classeExtra)
                        .append("\">");
                html.append("<div class=\"bloco-content\">");
                html.append("<h2>").append(nome).append("</h2>");
                html.append("<p><strong>Data:</strong> ").append(data).append("</p>");
                html.append("<p><strong>Status:</strong> ").append(curtida).append("</p>");
                html.append("<p>").append(desc).append("</p>");
                html.append("</div>");
                html.append("<div class=\"botoes\">");

                // Botão CONCLUIDO
                html.append("<form method=\"POST\" action=\"/concluido_aluno\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                html.append("<input type=\"hidden\" name=\"acao\" value=\"Concluido\">");
                html.append("<button type=\"submit\">Concluído</button>");
                html.append("</form>");

                // Botão NÃO CONCLUIDO
                html.append("<form method=\"POST\" action=\"/concluido_aluno\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                html.append("<input type=\"hidden\" name=\"acao\" value=\"Não Concluido\">");
                html.append("<button type=\"submit\">Não concluído</button>");
                html.append("</form>");


                html.append("</div>");
                html.append("</div>");
            }

            if (vazio) {
                html.append("<p class=\"Vazio\">Nenhuma atividade cadastrada ainda.</p>");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            html.append("<p>Erro ao carregar atividades.</p>");
        }
        html.append("<script src=\"/js/script.js\"></script>");
        html.append("</body></html>");

        // Enviar HTML gerado
        byte[] b = html.toString().getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        t.sendResponseHeaders(200, b.length);
        t.getResponseBody().write(b);
        t.close();
    }

    // -------------------- Professor (lista, adiciona e exclui atividades) --------------------

    private static void acesso_professor(HttpExchange t) throws IOException {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>AcademyFlow | Mural de Atividades</title>");
        html.append("<link rel=\"stylesheet\" href=\"/css/atividades.css\">");
        html.append("<link rel=\"stylesheet\" href=\"/css/geral.css\">");
        html.append("<link rel=\"icon\" href=\"/img/academyflow-logo.png\">");
        html.append("</head><body>");
        html.append("<header>");
        html.append("<div class=\"logo-header\">");
        html.append("<a href=\"#\">");
        html.append("<img src=\"/academyflow-nome.png\" alt=\"Logo AcademyFlow\">");
        html.append("</a>");
        html.append("</div>");

        html.append("<div class=\"nav\">");
        html.append("<div class=\"container-btn-header\">");
        html.append("<h4>Bem-Vindo Professor</h4>");
        html.append("<p id=\"nome-usuario\"></p>");
        html.append("</div>");

        html.append("<div class=\"buttons\">");
        html.append("<button class=\"dark-mode-button\" id=\"toggle-theme\" aria-label=\"Alternar modo escuro\">");
        html.append("<img class=\"dark-theme-img\" src=\"/img/light-theme.svg\" alt=\"Ícone Claro\">");
        html.append("<img class=\"light-theme-img\" src=\"/img/dark-theme.svg\" alt=\"Ícone Escuro\">");
        html.append("</button>");
        html.append("</div>");
        html.append("</div>");
        html.append("</header>");

        html.append("<main>");
        html.append("<section class=\"titulo\">");
        html.append("<div class=\"tituloh1\">");
        html.append("<h1>Mural de Atividades</h1>");
        html.append("</div>");
        html.append("<button class=\"editar\" id=\"mostrar-bloco\">✏️ Editar Mural</button>");
        html.append("<div id=\"overlay\"></div>");
        html.append("<div class=\"bloco-atividade\" id=\"bloco-atividade\">");
        html.append("<h2>Cadastrar Atividade</h2>");
        html.append("<form method=\"POST\" action=\"/cadastro\">");
        html.append("<div class=\"input\">");
        html.append("<label>Nome:</label><br>");
        html.append("<input type=\"text\" name=\"nome\" placeholder=\"Insira o nome da atividade\" required><br>");
        html.append("</div>");
        html.append("<div class=\"input\">");
        html.append("<label>Descrição:</label><br>");
        html.append("<input type=\"text\" name=\"descricao\" maxlength=\"50\" placeholder=\"Insira uma breve descrição da atividade\" required><br>");
        html.append("</div>");
        html.append("<div class=\"input\">");
        html.append("<label>Data:</label><br>");
        html.append("<input type=\"date\" name=\"data\" required><br>");
        html.append("</div>");
        html.append("<button type=\"submit\">Cadastrar</button>");
        html.append("</form>");
        html.append("<button id=\"voltar-bloco\">Voltar</button>");
        html.append("</div>");
        html.append("</section>");

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nome, descricao, data, curtida FROM dados ORDER BY id DESC")) {

            boolean vazio = true;

            while (rs.next()) {
                vazio = false;

                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String desc = rs.getString("descricao");
                String data = rs.getString("data");
                String curtida = rs.getString("curtida");

                // Classe extra para cor do card
                String classeExtra = "bloco";
                if ("Concluido".equals(curtida)) {
                    classeExtra = "card-curtido";
                } else if ("nao".equals(curtida)) {
                    classeExtra = "bloco";
                }

                html.append("<div class=\"")
                        .append(classeExtra)
                        .append("\">");
                html.append("<div class=\"bloco-content\">");
                html.append("<h2>").append(nome).append("</h2>");
                html.append("<p><strong>Data:</strong> ").append(data).append("</p>");
                html.append("<p><strong>Status:</strong> ").append(curtida).append("</p>");
                html.append("<p>").append(desc).append("</p>");
                html.append("</div>");
                html.append("<div class=\"botoes\">");

                // Botão CONCLUIDO
                html.append("<form method=\"POST\" action=\"/concluido_professor\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                html.append("<input type=\"hidden\" name=\"acao\" value=\"Concluido\">");
                html.append("<button type=\"submit\">Concluído</button>");
                html.append("</form>");

                // Botão NÃO CONCLUIDO
                html.append("<form method=\"POST\" action=\"/concluido_professor\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                html.append("<input type=\"hidden\" name=\"acao\" value=\"Não Concluido\">");
                html.append("<button type=\"submit\">Não concluído</button>");
                html.append("</form>");

                // Botão EXCLUIR
                html.append("<div class=\"overlay\"></div>");
                html.append(("<button class=\"excluir\" onclick=\"mostrarConfirmacao()\">Excluir</button>"));
                html.append("<div class=\"confirmacao\" id=\"confirmação\">");
                html.append("<h2>Você tem certeza que deseja excluir esta atividade?");
                html.append("<form method=\"POST\" action=\"/excluir\">");
                html.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\">");
                html.append("<input type=\"hidden\" name=\"acao\" value=\"excluir\">");
                html.append("<button class=\"excluir\" type=\"submit\">Excluir</button>");
                html.append("</form>");
                html.append(("<button onclick=\"fecharConfirmacao()\">Não Excluir</button>"));
                html.append("</div>");

                html.append("</div>");
                html.append("</div>"); // fecha div com classeExtra
            }

            if (vazio) {
                html.append("<p>Nenhuma atividade cadastrada ainda.</p>");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            html.append("<p>Erro ao carregar atividades.</p>");
        }
        html.append("<script src=\"/js/script.js\"></script>");
        html.append("</body></html>");

        // Enviar HTML gerado
        byte[] b = html.toString().getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        t.sendResponseHeaders(200, b.length);
        t.getResponseBody().write(b);
        t.close();
    }

    // -------------------- Conclusão (Concluido / Não concluido) --------------------

    private static void concluido_aluno(HttpExchange t) throws IOException {

        if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
            redirecionar(t, "/aluno"); //Redireciona para apagina do aluno
            return;
        }

        String corpo = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);
        String acao = pega(corpo, "acao"); // "concluido" ou "nao"
        String idStr = pega(corpo, "id");

        try {
            int id = Integer.parseInt(idStr);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE dados SET curtida = ? WHERE id = ?")) {  //atualiza o banco de dados
                ps.setString(1, acao);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        redirecionar(t, "/aluno");
    }

    // -------------------- Conclusão (Concluido / Não concluido) --------------------

    private static void concluido_professor(HttpExchange t) throws IOException {

        if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
            redirecionar(t, "/acesso-professor");
            return;
        }

        String corpo = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);
        String acao = pega(corpo, "acao"); // "curtir" ou "nao"
        String idStr = pega(corpo, "id");

        try {
            int id = Integer.parseInt(idStr);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE dados SET curtida = ? WHERE id = ?")) {
                ps.setString(1, acao);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        redirecionar(t, "/acesso-professor");
    }

    // -------------------- Função de excluir atividades --------------------

    private static void excluir(HttpExchange t) throws IOException {

        if (!t.getRequestMethod().equalsIgnoreCase("POST")) {
            redirecionar(t, "/acesso-professor"); //ao excluir redireciona para:
            return;
        }

        String corpo = URLDecoder.decode(ler(t), StandardCharsets.UTF_8);
        String idStr = pega(corpo, "id"); //pega o id da atividade a ser excluida

        try {
            int id = Integer.parseInt(idStr);

            // Usando apenas o ID para realizar a exclusão
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM dados WHERE id = ?")) { //deleta apenas a atividade com o id especifico
                ps.setInt(1, id);  // Passando apenas o ID para a PreparedStatement
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Atividade excluida, ID: "+idStr);

        redirecionar(t, "/acesso-professor");
    }

    // -------------------- Funções auxiliares --------------------

    private static String pega(String corpo, String campo) {
        // corpo no formato: campo1=valor1&campo2=valor2...
        for (String s : corpo.split("&")) {
            String[] p = s.split("=");
            if (p.length == 2 && p[0].equals(campo)) return p[1];
        }
        return "";
    }

    // -------------------- ENVIAR IMAGEM --------------------

    private static void enviarImagem(HttpExchange t, String arquivo) throws IOException {
        File f = new File("src/main/java/" + arquivo);

        if (!f.exists()) {
            t.sendResponseHeaders(404, -1);
            return;
        }

        // Detecta o MIME type pela extensão
        String contentType;
        if (arquivo.endsWith(".png")) {
            contentType = "image/png";
        } else if (arquivo.endsWith(".svg")) {
            contentType = "image/svg+xml";
        } else if (arquivo.endsWith(".jpg") || arquivo.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else {
            contentType = "application/octet-stream"; // fallback
        }

        byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
        t.getResponseHeaders().set("Content-Type", contentType);
        t.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = t.getResponseBody()) {
            os.write(bytes);
        }
    }

    // -------------------- Função para ler dados --------------------

    private static String ler(HttpExchange t) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8)
        );
        String linha = br.readLine();
        return (linha == null) ? "" : linha;
    }

    // -------------------- Função para enviar arquivos (html e JS) --------------------

    private static void enviar(HttpExchange t, String arq) throws IOException {
        File f = new File("src/main/java/" + arq);
        byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
        t.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        t.sendResponseHeaders(200, b.length);
        t.getResponseBody().write(b);
        t.close();
    }

    // -------------------- Função para enviar arquivos CSS --------------------

    private static void enviarCSS(HttpExchange t, String arq) throws IOException {
        File f = new File("src/main/java/" + arq);
        byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
        t.getResponseHeaders().add("Content-Type", "text/css; charset=UTF-8");
        t.sendResponseHeaders(200, b.length);
        t.getResponseBody().write(b);
        t.close();
    }

    // -------------------- Função para redirecionar rotas --------------------

    private static void redirecionar(HttpExchange t, String rota) throws IOException {
        t.getResponseHeaders().add("Location", rota);
        t.sendResponseHeaders(302, -1);
        t.close();
    }
}