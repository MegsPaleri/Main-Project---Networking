/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pserver;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author megha
 */
public class P2PServer {

    private static ServerSocket ss;
    private Socket s;
    private static final int port=6000;        
    private P2PServer(){                        //creating server socket port for listening
            try{                                //server port is 6000
                    log("Server started");                
                    ss=new ServerSocket(port);
                    listen();                    
                    
            }catch(Exception e){
                    System.err.println(e);
            }
    }    
    private void listen(){
            
            newNode node=null;
            try{
                    while(true){
                            s=ss.accept();
                            /*  creating newNode object for each connection */
                            node=new newNode(s);
                            node.start();    
                                                        
                    }         
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    public static void main(String[] args) {
            new P2PServer();
    }
    private void log(Object o){
            System.out.println(o);
    }
}
/*  Creating instance for each node*/
class newNode extends Thread{
    private  Socket s;
    private  PrintWriter pw;
    private  BufferedReader bf;
    private  static String serverIP;
    private  static String head=null,tail=null;
    private  static Hashtable<String,String> hashTable=new Hashtable<String, String>(); //declaring hash table
    private  static ArrayList<String> DHT=new ArrayList<String>();      //declaring DHT
    private  static final String myFolder="Multimedia";
    /*  Constructor receving the socket connection and neighbour nodes*/
    public newNode(Socket ss){
            s=ss;
            
            try{
                    /* create folder for saving multimedia file*/ 
                    String myFolder="Multimedia";
                    File tempFile=new File(myFolder);
                    tempFile.mkdir();
                    serverIP=s.getLocalAddress().getHostAddress();
                    log("server IP: "+serverIP); 
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    @Override
    public void run(){
            newConnection();
            String packet="",brockerIP="";
            try{
                    do{
                                packet=getData();
                                /*  New node request    */
                                if(packet.equals("NewNodeRequest")){
                                        log("----------------------------------------------");
                                        log("NewNodeRequest");
                                        if(isAnyBrocker()){
                                                /*  Assigning new node to the random brocker*/
                                                int max=getBrockerCount();
                                                int randomBrocker=(int)(Math.random()*(max-1));
                                                String Parent=DHT.get(randomBrocker);
                                                log("sending the parent: "+Parent);
                                                sendData("Parent");
                                                sendData(Parent);
                                        }else{
                                                /*  Server handling the connection*/
                                                log("sending Server as the parent");
                                                sendData("Parent");
                                                sendData(serverIP);
                                        }
                                }else if(packet.equals("NewBrockerRequest")){// New brocker request
                                        log("----------------------------------------------");
                                        log("NewBrockerRequest received "+s.getInetAddress().getHostAddress());
                                        /*  Adding node to DHT  */
                                        

                                                brockerIP=s.getInetAddress().getHostAddress();
                                                DHT.add(brockerIP);

                                                /*  Sending head and tail to node*/
                                                sendData("Head");
                                                sendData(head);                                            
                                                sendData("Tail");
                                                sendData(tail);

                                                if(head==null){
                                                        /*  set head as the connected node ip address*/
                                                        head=brockerIP;
                                                }
                                                /*  set tail as the connected node ip address*/
                                                tail=brockerIP;
                                        
                                }else if(packet.equals("Lookup")){
                                        log("----------------------------------------------");
                                        log("Lookup received from "+s.getInetAddress().getHostAddress());
                                        String key=getData();
                                        log("Lookup("+key+")");
                                        /* if data is present in server sending ACK*/
                                        String nodes=lookup(key);
                                        if(nodes==null){
                                                /* data is not in server sending NAK */
                                                log("Sending NAK to "+s.getInetAddress().getHostAddress());
                                                sendData("NAK");
                                        }else{
                                                log("Sending ACK to "+s.getInetAddress().getHostAddress());
                                                sendData("ACK");                                                
                                                /*  Sending data to node */
                                                sendFile(key);
                                        }
                                }else if(packet.equals("FileTransfer")){    //file transfer condition
                                        log("----------------------------------------------");
                                        log("FileTransfer from "+s.getInetAddress().getHostAddress());
                                        String fileName=getData();
                                        log("File: "+fileName);
                                        fileUpload(fileName);
                                        insert(fileName, "SERVER");     //uploading file to server condition
                                }else if(packet.equals("close")){
                                        log("----------------------------------------------");
                                        log("Node "+s.getInetAddress().getHostAddress() +" is clossing");
                                        break;
                                }else{
                                        System.err.println("Un Identified packet :"+packet);
                                }
                    }while(!packet.equals("close"));
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    private void fileUpload(String file){
            DataOutputStream bos;
            ObjectInputStream instream;     //files transfered as object streams
            try{

                    String roundTrip=getData();
                    log("fileSize:"+roundTrip);
                    int size = Integer.parseInt(roundTrip.trim());               
                    String myFile=myFolder+"\\"+file;
                    byte[] mybytearray = new byte[size];
                    instream = new ObjectInputStream(s.getInputStream());
                    bos = new DataOutputStream(new FileOutputStream(myFile));
                    mybytearray = (byte[])instream.readObject();                   
                    bos.write(mybytearray, 0, size);                  
                    bos.close(); 
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    private void sendFile(String key){
            try{
                    File myFile=new File(myFolder+"\\"+key);
                    byte[] fileContent = new byte[(int) myFile.length()];                        
                    String fileLength = String.valueOf(myFile.length());
                    System.out.println("file = " + myFile.getName());
                    sendData(fileLength);
                    
                    Thread.sleep(200);
                    
                    DataInputStream dis = new DataInputStream(new FileInputStream(myFile)); 
                    dis.read(fileContent, 0, fileContent.length);
                    dis.close();
                    ObjectOutputStream objectStream = new ObjectOutputStream(s.getOutputStream());
                    objectStream.writeObject(fileContent);// -- (fileContent, 0, fileContent.length);
                    objectStream.flush();
                    log("file sent completed!");
            }catch(Exception e){
                    System.err.println(e);
            } 
    }
    private void insert(String key,String object){
            /*   If the key is already in there*/
            if(hashTable.containsKey(key)){
                    String temp=hashTable.get(key);
                    hashTable.remove(key);      //Remove key
                    hashTable.put(key,temp+"$"+object); //append the now object with existing value
            }else{  
                    
                    hashTable.put(key,object);
            }
    }
    private String lookup(String key){
            return hashTable.get(key);
    }
    private String getData() {
        try{
          return bf.readLine().trim();
        }catch(Exception e){
                System.err.println(e+" :Get data error");
        }
        return null;
    }
    private boolean isAnyBrocker(){
            return (!DHT.isEmpty());
    }
    private void sendData(String data){
            pw.println(data);
            pw.flush();
    }
    private int getBrockerCount(){
        /*  return the number of brockers persently connected to server*/
            return DHT.size();
    }
    private void newConnection(){
            try{
                    bf=new BufferedReader(new InputStreamReader(s.getInputStream()));
                    pw=new PrintWriter(s.getOutputStream());
                    log("new node "+s.getRemoteSocketAddress()+" connected");
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    private void log(Object o){
            System.out.println(o);
    }
}
