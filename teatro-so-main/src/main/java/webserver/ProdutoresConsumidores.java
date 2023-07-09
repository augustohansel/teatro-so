package webserver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

public class ProdutoresConsumidores {

    private ArrayList<Teatro> cadeiras = new ArrayList<>();
    public void setCadeiras(ArrayList<Teatro> c) {
        cadeiras = c;
    }

    public ArrayList<Teatro> getCadeiras() {
        return cadeiras;
    }


    private ArrayList<Pedido> pedidos = new ArrayList<>();

    public ArrayList<Pedido> getPedidos() {
        return pedidos;
    }

    public void setPedidos(ArrayList<Pedido> pedidos) {
        this.pedidos = pedidos;
    }


    //instancia a classe produtores e consumidores e ja inicia uma thread
    public ProdutoresConsumidores(){
        new Thread(new Consumidor()).start();
    }

    public void produzir(int idCadeira, String nome, String ip){
        synchronized(pedidos){//garante o acesso unico a lista de pedidos
            Teatro cadeira = new Teatro();
            //laço usado para identificar a cadeira com o Id correspondente
            for(Teatro c:cadeiras){
                if(c.getIdCadeira() == idCadeira){
                    cadeira = c;
                    break;
                }
            }

            //verificação da disponibilidade da cadeira
            if(cadeira.isStatus() == false){//se for false=livre é permitido a alteração das informações da cadeira
                cadeira.setStatus(true);
                Pedido p = new Pedido(idCadeira,nome,ip);
                pedidos.add(p);
                pedidos.notify();
            }
        }
    }


    public class Consumidor implements Runnable{
        @Override
        public void run() {
            while (true){
                synchronized (pedidos) {
                    if (pedidos.size() == 0) {//confere se a lista de pedidos esta vazia
                        try{
                            pedidos.wait();//thread fica em espera até ser notificada sobre alguma alteração feita na lista pedidos
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    //salva as informações dos pedidos no arquivo de log
                    try {
                        OutputStream logs = new FileOutputStream("log.txt",true);

                        for(Pedido p:pedidos){
                            String linha = "cadeira:"+p.idCadeira+",nome:"+p.nome+",ip:"+p.ip+",data:"+new Date()+"\n";
                            logs.write(linha.getBytes(StandardCharsets.UTF_8));
                        }

                        logs.close();
                        logs.flush();
                        pedidos.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
            }
        }
    }

    public class Pedido{
        int idCadeira;
        String nome;
        String ip;
        public Pedido(int idCadeira, String nome, String ip) {
            this.idCadeira = idCadeira;
            this.nome = nome;
            this.ip = ip;
        }

    }

}
