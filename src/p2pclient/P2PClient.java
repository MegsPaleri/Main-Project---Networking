/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.Timer;

/**
 *
 * @author megha
 */
public class P2PClient implements Runnable{

    
    Socket s[]=new Socket[100];       
    BufferedReader[] bf=new BufferedReader[100];
    PrintWriter[] pw=new PrintWriter[100];
    private static DatagramSocket DSoc,DS_file;
    public static final int port=4000;
    private static int serverPort=6000,server=0,i=4,parentIndex=1,headIndex=2,tailIndex=3;
    private static String ServerIP="192.168.1.96",headIP=null,tailIP=null,parentIP="localhost";
    private static boolean brocker=false;
    public static  int DELAY = 60000; // 1 min
    public static long lookupTime,insertTime;
    private static String myFolder;
    public  static String IPaddress;
    public static chieldNode headNode=null,tailNode=null; 
    private  static Hashtable<String,String> hashTable=new Hashtable<String, String>();
    private  static Hashtable<String,String> lookupRequest=new Hashtable<String, String>();
    private  static Hashtable<String,String> insertRequest=new Hashtable<String, String>();
    private  static ArrayList<String> DHT=new ArrayList<String>();
    
    private static Thread threadBrockerListener,fileTransferListener;
    private static JButton searchBtn,browseBtn;
    private static JTextArea txt_search,txt_browse,txt_progress;
    private static JLabel lbl_progress,lbl_header;
    /*  Constructor 
     *  first constructor is using for creating an
     * object for passing it into chield node
     * 
     * The secong constructor is using for creating an
     * object from index.java
     */
    public P2PClient(){
            
    }
    public P2PClient(JButton button,JButton browseB, JTextArea search, JTextArea browse,JTextArea txtprogress, JLabel progress,JLabel header) {
            searchBtn=button; 
            browseBtn=browseB;
            txt_browse=browse;
            txt_search=search;
            txt_progress=txtprogress;
            lbl_progress=progress;
            lbl_header=header;
    }    
    /*
     * the execution of clientnode starts from
     * startClent function
     */
    public void startClient(){
              try{
                    IPaddress= InetAddress.getLocalHost().getHostAddress();
                    log("IPaddress: "+IPaddress);
                    /* create folder for saving multimedia file*/ 
                    myFolder="Multimedia";
                    File tempFile=new File(myFolder);
                    tempFile.mkdir();
                    /*  create a socket for fileSearch acknowledgement*/
                    DSoc=new DatagramSocket(port);
                    /*  create a socket for file transfer */
                    /*file transforing port is
                    *  1000  grater than node port
                    */
                    int filePort=5000;
                    DS_file=new DatagramSocket(filePort);
                    
                                        
                    if(makeTcpConnection(server, ServerIP, serverPort)){                         
                            
                            log("SeverConnected");
                            /*  starting fileTransferListener for sending file*/
                            fileTransferListener=new Thread(this);
                            fileTransferListener.start();
                            
                            /*
                             * create a timer for finding if the online
                             * time exceeds the predefined threshold.
                             * The threshold value is 1 min= 60000 milliseconds
                             */
                            ActionListener listener = new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent event) {

                                            if(!brocker){
                                                    /*  Sending brocker requet to server   */
                                                    log("Sending brocker request to server");
                                                    if(parentIP.equals(ServerIP)){
                                                            if(sendNewBrockerRequest()){
                                                                    brocker=true;                                                           
                                                            }                                                            
                                                    }else{
                                                            makeTcpConnection(server, ServerIP, serverPort);
                                                            log("SeverConnected"); 
                                                            if(sendNewBrockerRequest()){
                                                                    brocker=true;
                                                            }
                                                            log("ServerConnection is clossing");
                                                            sendData(server,"close");
                                                            try{
                                                                   bf[server].close();
                                                                   pw[server].close();
                                                                   s[server].close();
                                                            }catch(Exception e){
                                                                   log(e+" :ServerConnection clossing error");
                                                            }
                                                    }
                                                    DELAY+=DELAY;

                                            }

                                    }               
                            };
                            ActionListener label_listener = new ActionListener() {
                                    public void actionPerformed(ActionEvent event) {
                                            lbl_progress.setText("");
                                    }
                            };


                            // milliseconds between timer ticks
                            Timer t = new Timer(DELAY, listener);
                            t.start();
                            //timer for resetting the label
                            Timer label = new Timer(5000, label_listener);
                            label.start();
                            nodeHandler();
                    }else{
                            log("SeverConnection failed!");
                    }
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    /*  making TCP connection to specfied node*/
    private boolean makeTcpConnection(int index,String IP,int port){
            try{
                    s[index]=new Socket(IP,port);
                    bf[index]=new BufferedReader(new InputStreamReader(s[index].getInputStream()));
                    pw[index]=new PrintWriter(s[index].getOutputStream());
                    return true;
            }catch(Exception e){
                    System.err.println(e);
                    return false;
            }

    }
    private void closeConnection(int index){
            sendData(index, "close");//sending close before closing connection
            try{
                    bf[index].close();
                    pw[index].close();
                    s[index].close();
             }catch(Exception e){
                    System.err.println(e+" :connection closing error: "+index);
             }
    }
    /*
     * nodeHandler find the parent node
     * and request for the connection
     */
    private void nodeHandler(){
            if(sendNewNodeRequest()){
                    log("Connecting to parent: "+ parentIP);
                    /*  Connecting to parent    */
                        if(parentIP.equals(ServerIP)){
                                /*  Server is the parent   */
                                try{
                                        log("server is the parent");
                                        //Enable the search button in the index file
                                        searchBtn.setEnabled(true);  // enable search button in index.java
                                        browseBtn.setEnabled(true);
                                }catch(Exception e){
                                       System.err.println(e);
                                }
                        }else{
                                searchBtn.setEnabled(true); // enable search button in index.java
                                browseBtn.setEnabled(true);
                                /*  Making connection with parent   */
                                makeTcpConnection(parentIndex, parentIP, port);
                                sendData(parentIndex, "ParentRequest");                                
                                log("Parent connected !, ip: "+parentIP);
                                /*  close server connection */
                                log("ServerConnection is clossing");
                                closeConnection(server);                                 
                        }                    
            }else{
                    log("ServerConnection is clossing");
                    closeConnection(server);
            }
            //log("Sending server close");
            //sendData(server,"close"); sendData(server,"close");
    }
    
    /*  Sending brocker request to server when time t exeeds the min brocker time*/
    private boolean sendNewBrockerRequest(){
            log("Sending new brocker request to server");
            sendData(server,"NewBrockerRequest");                        
            try{
                    /*  Reading head and tail brokers */
                    String str=bf[server].readLine();
                    if(str.equals("Head")){
                            headIP=getData(server);
                            str=getData(server);
                            if(str.equals("Tail")){
                                    tailIP=getData(server);
                                    log("Head: "+headIP+" Tail: "+tailIP);
                                    
                                    /*  closing parent connection    */                                    
                                    log("parentConnection is clossing");
                                    int pIndex;
                                    if(parentIP.equals(ServerIP)){
                                            pIndex=server;
                                    }else{
                                            pIndex=parentIndex;
                                    }
                                    /*  closing parent conneciton   */
                                    closeConnection(pIndex);
                                    /*
                                    *  creating a new thread for handling 
                                    *  the clield connection to the brocker
                                    */
                                    threadBrockerListener=new Thread(this);
                                    threadBrockerListener.start();
                                    return true;
                            }else{
                                    log("NewBrockerRequest Faild !");
                                    return false;
                            }
                    }else{
                            log("NewBrockerRequest Faild !");
                            return false;
                    }
                    
            }catch(Exception e){
                    System.err.println("NewBrockerRequest faild !");
                    System.err.println(e);
            }
            return false;
    }
 
    /*  Sending new node requset to the server in the starting of this node*/    
    private  boolean sendNewNodeRequest(){
            sendData(server,"NewNodeRequest");
            try{
                    if(getData(server).equals("Parent")){
                            parentIP=getData(server);
                            return true;
                    }else{
                            log("NewNodeRequest Faild !");
                    }
            }catch(Exception e){
                    log(e);
            }
            return false;
    }
    /*  receiving multimedia content from server*/
    private void receveDataFromServer(String file){
            try{
                    String roundTrip=getData(server);//reading the file size
                    log("fileSize:"+roundTrip);
                    int size = Integer.parseInt(roundTrip.trim());          
                    
                    String myFile=myFolder+"\\"+file;
                    byte[] mybytearray = new byte[size];
                    ObjectInputStream instream = new ObjectInputStream(s[server].getInputStream());
                    DataOutputStream bos = new DataOutputStream(new FileOutputStream(myFile)); 
                                            
                    mybytearray = (byte[])instream.readObject();
                    
                    /*starting player*/
                    WinConsole console=new WinConsole(myFolder+"\\"+file);
                    console.start();
                    bos.write(mybytearray, 0, size);                  
                    bos.close();  
                   log("Data received from server");
            }catch(Exception e){
                    System.err.println(e);
                    log("Data erceiving error");
            }
    }
    /*  Sending data to specified node*/
    private void sendData(int index,String data){
            pw[index].println(data);
            pw[index].flush();
    }
    private String getData(int index) throws IOException{
         return bf[index].readLine().trim();
    }    
    public void log(Object o){
            System.out.println(o);
            lbl_progress.setText((String)o);
            txt_progress.append("\n "+(String)o);
    }
    public void searchProgress(String str){
            txt_search.append("\n "+str);
    }
    public void browseProgress(String str){
            txt_browse.append("\n "+str);
    }
    private void brockerListener(){
            setHeader();
            chieldNode node=null;   
            BufferedReader bfReader;
            try{
                    /*  create a p2pClient object to handle
                     *  the DHT from the chieldNode  object.
                     * 
                     *  pass the P2PClient object to the 
                     *  chieldNode object
                     */
                    P2PClient obj=new P2PClient();
                    ServerSocket ss=new ServerSocket(port);
                    
                    Socket socket;
                    log("------------------------------");
                    log("New Parent Node started");
                    log("Listening for chield...");
                    /*
                     * head and tail are connecting if it is
                     * not null. 
                     */
                    if(!headIP.equals("null")){                            
                            log("Connecting head node");
                            makeTcpConnection(headIndex,headIP,port);
                            sendData(headIndex, "BrockerConnectionRequest");
                            sendData(headIndex, "tailNode"); //for the connected node it is the tail link
                            headNode=new chieldNode(s[headIndex], obj,null);
                            headNode.start();
                    }
                    if(!tailIP.equals("null")){
                            log("Connecting tail node");
                            makeTcpConnection(tailIndex, tailIP, port);
                            sendData(tailIndex, "BrockerConnectionRequest");
                            sendData(tailIndex, "headNode");
                            tailNode=new chieldNode(s[tailIndex], obj,null);
                            tailNode.start();
                    }
                    if(tailNode==null){
                            log("TailNode: null");                            
                    }else{
                            log("TailNode: "+tailNode.getSocket());
                    }                        
                    if(headNode==null){
                            log("TailNode: null");                            
                    }else{
                            log("HeadNode: "+headNode.getSocket());
                    }
                    while(true){
                            socket=ss.accept();
                            bfReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String message=bfReader.readLine().trim();
                            
                            if(message.equals("ParentRequest")){
                                    /*  creating newNode object for each connection */
                                    log("------------------------------");
                                    log("New childNode: "+socket.getInetAddress().getHostAddress());
                                    String chieldIP=socket.getInetAddress().getHostAddress();
                                    addChield(chieldIP);
                                    node=new chieldNode(socket,obj,chieldIP);
                                    node.start();                            
                            }else if(message.equals("BrockerConnectionRequest")){
                                    log("------------------------------");
                                    message=bfReader.readLine().trim();
                                    if(message.equals("tailNode")){
                                        
                                            /*  close the previous tailNode if exists   */
                                            if(!(tailNode==null))
                                                    tailNode.stop();
                                            log("New tailNode: "+socket.getInetAddress().getHostAddress());
                                            tailNode=new chieldNode(socket,obj,null);
                                            tailNode.start();                                                                                      
                                    }else if(message.equals("headNode")){
                                            /*  close the previous headNode if exists   */
                                            if(!(headNode==null))
                                                    headNode.stop();
                                            //log(headNode.toString());
                                            log("New HeadNode: "+socket.getInetAddress().getHostAddress());
                                            headNode=new chieldNode(socket,obj,null);
                                            headNode.start();
                                    }
                                    if(tailNode==null){
                                            log("TailNode: null");                            
                                    }else{
                                            log("TailNode: "+tailNode.getSocket());
                                    }                        
                                    if(headNode==null){
                                            log("TailNode: null");                            
                                    }else{
                                            log("HeadNode: "+headNode.getSocket());
                                    }
                            }else{
                                    log("------------------------------");
                                    System.err.println("Unidentified packet: "+message);
                            }
                    }         
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    public void search(String key){
            searchProgress("searching for "+key);
            log("----------------------------------------------");
            log("searching for "+key);
            if(!brocker){
                    if(parentIP.equals(ServerIP)){
                            /*  directly searching in server*/
                            try{                         
                                    log("Sending lookup("+key+") to server");
                                    sendData(server,"Lookup");
                                    sendData(server, key);
                                    String Acknowledgement=getData(server);
                                    if(Acknowledgement.equals("ACK")){
                                            /*  multimedia content
                                                found in server
                                             */
                                            log("Receving ACK from server");
                                            searchProgress("Search keyword  found in server!");                                            
                                            receveDataFromServer(key);
                                            
                                            //new WinConsole().startConsole(myFolder+"\\"+key);//starting windows console
                                            log("file received!");
                                    }else if(Acknowledgement.equals("NAK")){
                                            log("Receving NAK from server");
                                            log("Please search new keywords");
                                            searchProgress("Search keyword not found !");
                                            lbl_progress.setText("<html><p style='color:red;'>Please search new keywords</p></html>");
                                    }else{
                                        log("Un Identified packet :"+Acknowledgement);
                                    }
                            }catch(Exception e){
                                    System.err.println(e);
                            }
                    }else{
                            try{                           
                                    log("Sending lookup("+key+") to Parent");
                                    sendData(parentIndex,"Lookup");
                                    sendData(parentIndex, key); 
                                    sendData(parentIndex, IPaddress);
                                    
                                    DatagramPacket dp = receiveUDP();                                    
                                    String Acknowledgement = new String(dp.getData()).trim();
                                    if(Acknowledgement.equals("ACK")){
                                            log("Receving ACK from "+dp.getAddress());
                                            /*  Sending insert to parent */
                                            sendData(parentIndex, "Insert");                                            
                                            sendData(parentIndex, key);
                                            sendData(parentIndex, IPaddress);
                                            
                                            dp = receiveUDP();
                                            String nodes = new String(dp.getData()).trim();
                                            log("nodes: "+nodes);                                            
                                            recieveFile(nodes,key);
                                    }else if(Acknowledgement.equals("NAK")){
                                            log("Receving NAK from "+dp.getAddress());
                                            
                                            /* serching in server */    
                                            log("Sending lookup("+key+") to server");
                                            makeTcpConnection(server,ServerIP,serverPort);
                                            log("Server connected");
                                            sendData(server,"Lookup");
                                            sendData(server, key);
                                            Acknowledgement=getData(server);
                                            if(Acknowledgement.equals("ACK")){
                                                    log("Receving ACK from server");                                                    
                                                    searchProgress("Search keyword  found in server!");                                                    
                                                    receveDataFromServer(key);
                                                    
                                                    log("file received!");
                                                    
                                                    /*  Sending insert to parent */
                                                    log("sending insert to parent");
                                                    sendData(parentIndex, "Insert");                                                   
                                                    sendData(parentIndex, key);
                                                    sendData(parentIndex, IPaddress);

                                                    log("Clossing sever Connection");                                                    
                                                    closeConnection(server);                                                    
                                                         
                                            }else if(Acknowledgement.equals("NAK")){
                                                    log("Receving NAK from server");
                                                    
                                                    log("Clossing sever Connection");                                                    
                                                    closeConnection(server);
                                                    
                                                    log("Please search new keywords");
                                                    searchProgress("Search keyword not found !");
                                                    lbl_progress.setText("<html><p style='color:red;'>Please search new keywords</p></html>");
                                            }else{
                                                log("Un Identified packet :"+Acknowledgement);
                                            }
                                            
                                    }else{
                                        log("Un Identified packet :"+Acknowledgement);
                                    }
                            }catch(Exception e){
                                    System.err.println(e);
                            }
                    }
            }else{
                    /* brocker searching its own Hashtable */
                    String nodes= hashTable.get(key);
                    if(nodes==null){
                            log("lookup in brocker");
                            lookup(key,IPaddress);
                            try{
                                    
                                    
                                    DatagramPacket dp = receiveUDP();                                    
                                    String Acknowledgement = new String(dp.getData()).trim();
                                    if(Acknowledgement.equals("ACK")){
                                            log("Receving ACK from "+dp.getAddress());
                                            searchProgress("Search keyword found !");
                                            /* updating DHT */    
                                            insert(key,IPaddress);
                                            dp = receiveUDP();
                                            nodes = new String(dp.getData()).trim();
                                            log("nodes: "+nodes);                                            
                                            recieveFile(nodes,key);
                                    }else if(Acknowledgement.equals("NAK")){
                                            log("Receving NAK from "+dp.getAddress());
                                            
                                             /* serching in server */    
                                            log("Sending lookup("+key+") to server");
                                            makeTcpConnection(server,ServerIP,serverPort);
                                            log("Server connected");
                                            sendData(server,"Lookup");
                                            sendData(server, key);
                                            Acknowledgement=getData(server);
                                            if(Acknowledgement.equals("ACK")){
                                                    log("Receving ACK from server");
                                                    searchProgress("Search keyword  found in server!");                                                    
                                                    receveDataFromServer(key);
                                                    
                                                    log("file received!");
                                                    
                                                    /* updating DHT */   
                                                    insert(key,IPaddress);
                                                    
                                                    log("Clossing sever Connection");                                                    
                                                    closeConnection(server);
                                                    
                                            }else if(Acknowledgement.equals("NAK")){
                                                    log("Receving NAK from server");
                                                    
                                                    log("Clossing sever Connection");                                                    
                                                    closeConnection(server);
                                                    log("Please search new keywords");
                                                    searchProgress("Search keyword not found !");
                                                    lbl_progress.setText("<html><p style='color:red;'>Please search new keywords</p></html>");
                                            }else{
                                                log("Un Identified packet :"+Acknowledgement);
                                            }
                                            
                                    }else{
                                        log("Un Identified packet :"+Acknowledgement);
                                    }
                            }catch(Exception e){
                                    System.err.println(e);
                            }
                    }else{
                            log("Search keyword found !");
                            searchProgress("Search keyword  found !");      
                            String temp_exist[]=nodes.split("\\$");
                            boolean isExist=false;
                            for(int i=0;i<temp_exist.length;i++){
                                    if(temp_exist[i].equals(IPaddress))
                                            isExist=true;
                            }
                            if(isExist){// if ip is present
                                                      
                                    WinConsole console=new WinConsole(myFolder+"\\"+key);
                                    console.start();
                            }else { 
                                    recieveFile(nodes,key);
                            }
                                    
                            
                    }
            }
    }
    private void recieveFile(String nodeStr,String file){           
            try{
                    
                    String nodes[]=nodeStr.split("\\$");
                    int i=0;                      
                    
                    searchProgress("Data receiving from :"+nodes[i]);
                    sendFileTransferACK("fileTransfer", nodes[i]);
                    log("sending file request to port: "+nodes[i]);
                    Thread.sleep(300);
                    sendFileTransferACK(file, nodes[i]);
                    
                    ServerSocket SerS=new ServerSocket(5000);
                    Socket soc=SerS.accept();
                    BufferedReader bfR=new BufferedReader(new InputStreamReader(soc.getInputStream()) );
                    String roundTrip=bfR.readLine().trim();
                    log("fileSize:"+roundTrip);
                    int size = Integer.parseInt(roundTrip.trim());               
                    
                    String myFile=myFolder+"\\"+file;
                    byte[] mybytearray = new byte[size];
                    ObjectInputStream instream = new ObjectInputStream(soc.getInputStream());
                    DataOutputStream bos = new DataOutputStream(new FileOutputStream(myFile));                                
                    
                    mybytearray = (byte[])instream.readObject();   
                    
                    /*starting player*/
                    WinConsole console=new WinConsole(myFolder+"\\"+file);
                    console.start();
                    
                    bos.write(mybytearray, 0, size);                  
                    bos.close();
                    
                    roundTrip=bfR.readLine().trim();
                    if(roundTrip.equals("close")){
                            log("connection is closing");
                            try{
                                    bfR.close();
                                    soc.close();
                                    SerS.close();
                            }catch(Exception e){
                                    System.err.println(e+" connection clossing error");
                            }
                    }
            }catch(Exception e){
                    System.err.println(e+" :File receiving error");
            }
    }
    public void lookup(String key,String requestIP){
            log("----------------------------------------------");
            log("lookup("+key+") :"+requestIP);
            
            String nodes= hashTable.get(key);
            if(nodes==null){
                    if(isLookupReceived(key,requestIP)){
                            if(IPaddress.equals(requestIP)){
                                    //if the requset is from same brocker node
                                    log("lookup receved from same node. Sending NAK");
                                    sendUDP("NAK", requestIP);
                            }else if(checkChieldRequest(requestIP)){
                                    /* if the request is from chield node 
                                     * the routing completed and no result found
                                     */
                                    log("lookup receved from chield node. Sending NAK");
                                    sendUDP("NAK", requestIP);
                            }else{
                                    log("requestedIP: "+requestIP);
                            }
                    }else{
                            if(headNode==null){
                                    log("Key is not found");
                                    sendUDP("NAK", requestIP);
                            }else{
                                    log("key is not found in this node.Requset routing to neighbour nodes");                                   
                                    headNode.search(key,requestIP);
                            }
                    }
            }else{
                    /*  brocker is the data provider.
                     *  Send the port address of the nodes
                     *  containing the requested data
                     */
                    log("Sending ACK to"+requestIP);
                    sendUDP("ACK", requestIP);
                    sendUDP(nodes, requestIP);                       
            }
    }
    public void insert(String key,String object){
            /*   If the key is already in there*/
            log("----------------------------------------------");
            log("insert("+key+","+object+")");

//            Enumeration items = hashTable.keys();
//
//            // Now iterate
//            while(items.hasMoreElements()){
//                 System.out.print(items.nextElement()+"     ");
//            }
//            if(hashTable.elements().hasMoreElements())
//                    log(hashTable.elements().nextElement());
            if(hashTable.containsKey(key)){
                    log("inserting key: "+key);
                    String temp=hashTable.get(key);
                    String temp_exist[]=temp.split("\\$");
                    boolean isExist=false;
                    for(int i=0;i<temp_exist.length;i++){
                            if(temp_exist[i].equals(object))
                                    isExist=true;
                    }
                    if(!isExist){ // if the same object is not already present 
                            hashTable.remove(key);      //Remove key
                            hashTable.put(key,temp+"$"+object); //append the now object with existing value
                    }
            }else if(isInsertReceived(key,object)){  
                    log("Inset already received.. updating DHT..");
                    hashTable.put(key,object);                   
            }else{      
                    if(headNode==null){
                            log("inserting key: "+key);
                            hashTable.put(key,object);
                    }else{
                            log("routing insert to neighbouring nodes");
                            headNode.update(key,object);
                    }
            }
    }
    private boolean isInsertReceived(String key,String object){
            if((System.currentTimeMillis()-insertTime)>3000){
                    log("insert timeout");
                    insertRequest.clear();
                    insertRequest.put(key, object);
                    insertTime=System.currentTimeMillis();
                    return false;
            }
            String savedObject=insertRequest.get(key);
            if(savedObject==null){    // if the requset is not alredy received
                    insertRequest.put(key, object);
                    insertTime=System.currentTimeMillis();
                    return false;
            } if(savedObject.equals(object)){
                    insertRequest.remove(key); // request is already received
                    return true;
            }else{
                    insertRequest.put(key, object);
                    insertTime=System.currentTimeMillis();
                    return false;
            }
    }
    private boolean isLookupReceived(String key,String requestIP){
        if((System.currentTimeMillis()-lookupTime)>3000){ // if requsets time is grater than 3 sec
                log("lookup time out");
                lookupRequest.clear();
                lookupRequest.put(key, requestIP);
                lookupTime=System.currentTimeMillis();
                return false;
        }
        String savedIP=lookupRequest.get(key);
        if(savedIP==null){    // if the requset is not alredy received
                lookupRequest.put(key, requestIP);
                lookupTime=System.currentTimeMillis();
                return false;
        }else if(savedIP.equals(requestIP)){
                lookupRequest.remove(key); // request is already received
                return true;
        }else{
                lookupRequest.put(key, requestIP);
                lookupTime=System.currentTimeMillis();
                return false;
        }
    }
    private boolean checkChieldRequest(String requestIP){
            if(DHT.contains(requestIP)){ //request is from chield node
                    return true;         
            }else{
                    return false;
            }
    }
    public void sendUDP(String data,String requestIP){
            try{
                    byte[] sdata=data.getBytes();                    
                    InetAddress ip = InetAddress.getByName(requestIP);                    
                    DSoc.send(new DatagramPacket(sdata, sdata.length, ip, port));
            }catch(Exception e){
                    System.err.println(e);
            }
            
    }
    public DatagramPacket receiveUDP(){
            byte b[] = new byte[1024];
            DatagramPacket dp = new DatagramPacket(b, b.length);  
            try{
                    DSoc.receive(dp); 
            }catch(Exception e){
                    System.err.println(e);
            }
            return dp;
    }
    public void upladFile(File myFile){
            log("----------------------------------------------");
            log("Transfering file to server");
            browseProgress("uploading file: "+myFile.getName());
            if(s[server].isClosed()){
                    log("connecting to server");
                    makeTcpConnection(server, ServerIP, serverPort);
                    //updating file to server
                    uploader(myFile);
                    
                    log("ServerConnection is clossing");
                    sendData(server,"close");
                    try{
                           bf[server].close();
                           pw[server].close();
                           s[server].close();
                    }catch(Exception e){
                           System.err.println(e+" :ServerConnection clossing error");
                    }
            }else{
                    uploader(myFile);
            }
                        
    }
    private void uploader(File myFile){
            sendData(server, "FileTransfer");
            sendData(server,myFile.getName());
            try{
                    byte[] fileContent = new byte[(int) myFile.length()];                        
                    String fileLength = String.valueOf(myFile.length());
                    System.out.println("file = " + myFile.getName());
                    sendData(server, fileLength);

                    Thread.sleep(1000);
                    
                    DataInputStream dis = new DataInputStream(new FileInputStream(myFile)); 
                    dis.read(fileContent, 0, fileContent.length);
                    dis.close();
                    ObjectOutputStream objectStream = new ObjectOutputStream(s[server].getOutputStream());
                    objectStream.writeObject(fileContent);// -- (fileContent, 0, fileContent.length);
                    objectStream.flush();
                    
                    browseProgress("file upload successfull!");
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    @Override
    public void run(){
            if(Thread.currentThread()==threadBrockerListener){
                brockerListener();
            }else if(Thread.currentThread()==fileTransferListener){
                fileTransferListener();
            }
    }
    public String getIP(){
            return IPaddress;
    }
    public void removeChiled(String chieldIP){
            DHT.remove(chieldIP);
    }
    private void addChield(String chieldIP){
            DHT.add(chieldIP);
    }
    private void fileTransferListener()     {
            try{
                    while(true){
                            DatagramPacket dp = receiveFileTransferACK();                                    
                            String packet = new String(dp.getData()).trim();log(packet);
                            if(packet.equals("fileTransfer")){
                                    log("----------------------------------------------");
                                     String ip=dp.getAddress().getHostAddress();
                                    log("File transfer received from :"+ip);
                                    dp=receiveFileTransferACK();
                                    String key = new String(dp.getData()).trim();
                                   
                                    if(makeTcpConnection(i,ip,5000)){
                                        log("node connected");
                                        sendFile(key,i);
                                        Thread.sleep(1000);
                                        log("Clossing connection ");                                                    
                                        closeConnection(i);
                                        i++;
                                    }else{
                                        log("connection faild");
                                    }
                            }else{
                                    log("Unidentified packet: "+packet);
                            }
                    }
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    private void sendFile(String file,int index){
            try{
                    log("File send in progress...");
                    File myFile=new File(myFolder+"\\"+file);
                    byte[] fileContent = new byte[(int) myFile.length()];                        
                    String fileLength = String.valueOf(myFile.length());
                    System.out.println("file = " + myFile.getName());
                    sendData(index,fileLength);
                    
                    Thread.sleep(200);
                    
                    DataInputStream dis = new DataInputStream(new FileInputStream(myFile)); 
                    dis.read(fileContent, 0, fileContent.length);
                    dis.close();
                    ObjectOutputStream objectStream = new ObjectOutputStream(s[index].getOutputStream());
                    objectStream.writeObject(fileContent);// -- (fileContent, 0, fileContent.length);
                    objectStream.flush();
                    log("file sent completed!");
            }catch(Exception e){
                    System.err.println(e);
            } 
    }
    public DatagramPacket receiveFileTransferACK(){
            byte b[] = new byte[1024];
            DatagramPacket dp = new DatagramPacket(b, b.length);  
            try{
                    DS_file.receive(dp); 
            }catch(Exception e){
                    System.err.println(e);
            }
            return dp;
    }
    public void sendFileTransferACK(String data,String rIP){
            byte[] sdata=data.getBytes();
            try{
                    InetAddress ip = InetAddress.getByName(rIP);                    
                    DS_file.send(new DatagramPacket(sdata, sdata.length, ip, 5000));
            }catch(Exception e){
                    System.err.println(e);
            }
    }
    private void setHeader(){
            lbl_header.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/brockerHeader.jpg")));
    }
}




