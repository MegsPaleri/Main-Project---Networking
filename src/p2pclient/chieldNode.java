/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


/**
 *
 * @author megha
 */

class chieldNode extends Thread{
    public Socket s;
    private  PrintWriter pw;
    private  BufferedReader bf;
    private static P2PClient object;
    private static String chieldIP;
    public chieldNode(Socket socket,P2PClient obj,String ip){
            s=socket;     
            object=obj; 
            chieldIP=ip;
            log("New chieldNode started @: "+s.getInetAddress().getHostAddress());
    }
    public void run(){
            newConnection();
            String packet="",key;
            try{    
                    do{
                                packet=getData();
                                if(packet.equals("Lookup")){
                                        log("\nLookup received from "+s.getInetAddress().getHostAddress());
                                        key=getData();                                        
                                        String requestIP=getData();                                        
                                        /* if data is present in server sending ACK*/
                                        lookup(key,requestIP);                                        
                                }else if(packet.equals("Insert")){
                                        log("\nInsert received from "+s.getInetAddress().getHostAddress());
                                        key=getData();
                                        String ip=getData();
                                        insert(key,ip);                                     
                                }else if(packet.equals("close")){
                                        log("\nNode "+s.getInetAddress().getHostAddress() +" is clossing");
                                        if(!(chieldIP==null)){
                                                log("Removing chield");
                                                object.removeChiled(chieldIP);
                                        }                                            
                                        break;
                                }else{
                                        System.err.println("Un Identified packet :"+packet);
                                }
                    }while(!packet.equals("close"));
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    /*  if it is a hed node conneciton search
     *  for the key in neighbour connection
     */
    public void search(String key,String requestPort){
            log("\nconnecting to neighbour node..");
            sendData("Lookup");
            sendData(key); 
            sendData(requestPort);
    }
    private void lookup(String key,String requestIP){
            object.lookup(key,requestIP);
    }
    private void insert(String key,String ip){
            object.insert(key, ip);
    }
    public void update(String key,String ip){
            sendData("Insert");
            sendData(key); // sending ip address
            sendData(ip);
    }
    public String getSocket(){
            return s.getInetAddress().getHostAddress();
    }
    private String getData() throws IOException{
          return bf.readLine().trim();
    }
    private void sendData(String data){
            pw.println(data);
            pw.flush();
    }
    private void newConnection(){
            try{
                    bf=new BufferedReader(new InputStreamReader(s.getInputStream()));
                    pw=new PrintWriter(s.getOutputStream());                    
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    private void log(Object o){
            System.out.println(o);
    }
      
}
