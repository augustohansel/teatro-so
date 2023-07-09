package webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import webserver.ProdutoresConsumidores.Pedido;

public class Server {
    private static ArrayList<Teatro> assentos = new ArrayList<>();
    private static ArrayList<Pedido> pedidos = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        //Laço para instanciar os assentos do teatro
        for(int i=1;i<=30;i++){
            Teatro teatro = new Teatro();
            teatro.setIdCadeira(i);//laço vai adicionando o numero
            teatro.setStatus(false);//e o valor dela para falso=livre
            assentos.add(teatro);//adiciona o assento ao array de assentos(vetor)
        }

        ServerSocket ss = new ServerSocket(8000);
        System.out.println("Iniciando server socket...");

        ProdutoresConsumidores p = new ProdutoresConsumidores();
        p.setCadeiras(assentos);
        p.setPedidos(pedidos);

        //1. recebe conexão
        while (true) {
            Socket socket = ss.accept();
            System.out.println("Conexão recebida");
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            //2. recebe requisição
            byte[] buffer = new byte[4096];
            int nBytes = in.read(buffer);
            String str = new String(buffer, 0, nBytes);
            String[] linhas = str.split("\n");
            int i = 1;
            for (String linha : linhas) {
                System.out.println("[LINHA " + i + "] " + linha);
                i++;
            }
            String[] linha1 = linhas[0].split(" ");
            String recurso = linha1[1];
            System.out.println("[RECURSO] " + recurso);

            String idCadeira = null;

            if (recurso.equals("/")) {
                recurso = "/index.html";
            } else if (recurso.contains("/compraingresso")) {
                String[] arr = recurso.split("\\?");
                idCadeira = arr[1].split("=")[1];
                recurso = "/compraingresso.html";
            } else if (recurso.contains("/reservar")) {
                String[] arr = recurso.split("\\?");
                String id = arr[1].split("=")[1].split("&")[0];
                String nome = arr[1].split("&")[1].split("=")[1];

                p.produzir(Integer.parseInt(id), nome, socket.getInetAddress().toString());
                recurso = "/index.html";
            }

            recurso = recurso.replace('/', File.separatorChar);
            String header = "HTTP/1.1 200 OK\n" +
                    "Content-Type: " + getContentType(recurso) + "; charset=utf-8\n\n";
            File f = new File("arquivos_html" + recurso);

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            
            if (!f.exists()) {
                bout.write("404 NOT FOUND\n\n".getBytes(StandardCharsets.UTF_8));
            } else {
                InputStream fileIn = new FileInputStream(f);
                bout.write(header.getBytes(StandardCharsets.UTF_8));
                //escreve arquivo
                nBytes = fileIn.read(buffer);
                do {
                    if (nBytes > 0) {
                        bout.write(buffer, 0, nBytes);
                        nBytes = fileIn.read(buffer);
                    }
                } while (nBytes == 4096);
            }
            String saida = processaVariaveis(bout,recurso,idCadeira);
            out.write(saida.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
            socket.close();
        }
    }

    private static String processaVariaveis(ByteArrayOutputStream bout, String recurso, String idCadeira) {
        String str = new String(bout.toByteArray());

        String texto = "";

        if(idCadeira == null){
            for (Teatro assento : assentos) {
                //false = livre / true = ocupado;
                String situacao = assento.isStatus() ? "Ocupado" : "Livre";
                String estilo = assento.isStatus() ? "ocupado" : "livre";//dependendo do status ele chama a função do css, alterando o background-color da div

                texto+="<div class=\"assento " + estilo + "\">" + //div para manter os elementos da cadeira juntos e para alterar a cor da cadeira
                            "<div class=\"numero centralizarLetra\">" + assento.getIdCadeira() + "<br>" +
                                "<div class=\"situacao\">" + situacao + "</div>" +
                                "<div class=\"reservar-button2\"><a href=\"compraingresso.html?cadeira=" + assento.getIdCadeira() + "\">Reservar</a></div>" +
                            "</div>"+
                        "</div>";


            }

            str = str.replace("${htmlpag}", texto);//altera o texto da pagina html pelo texto acima
        }

        else{
            texto+="<form action='/reservar' method='GET'>"+//formulario para reserva passando as informações pelo metodo get
                        "<input type='hidden' value="+idCadeira+" name='idCadeira'>"+
                        "<h3>Efetuar Reserva:</h3>" +
                        "<input type='text' placeholder='Insira seu nome' name='nome' required>"+ "<br>" +
                        "<button type='submit' class='reservar-button'>Reservar</button>"+
                        "<a href='index.html' class='cancelar-button'>Voltar</a>" +
                    "</form>";
            str = str.replace("${form}", texto);
        }

        return str;
    }

    private static String getContentType(String nomeRecurso) {
        if (nomeRecurso.toLowerCase().endsWith(".css")) {
            return "text/css";
        } else if (nomeRecurso.toLowerCase().endsWith(".jpg")
                || nomeRecurso.toLowerCase().endsWith(".jpeg"))
        {
            return "image/jpeg";
        } else if (nomeRecurso.toLowerCase().endsWith(".png"))
        {
            return "image/png";
        } else if (nomeRecurso.toLowerCase().contains(".js"))
        {
            return "text/javascript";
        } else {
            return "text/html";
        }
    }

}
