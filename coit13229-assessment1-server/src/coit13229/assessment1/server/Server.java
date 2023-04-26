/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package coit13229.assessment1.server;

import model.Drone;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Fire;
/**
 *
 * @author thainguyen
 */
public class Server {
    
    private final int SERVER_PORT = 8887;
    private ServerSocket listenSocket;
    private static ServerGUI serverGUI = new ServerGUI();
    private HashMap<Integer, Drone> activeDrones = new HashMap();
    ArrayList<Connection> connections = new ArrayList<>();
    Connection connection;
    
    private void runServer() {
        serverGUI.setServer(this);
        loadFire();
        
        try {
            listenSocket = new ServerSocket(SERVER_PORT);
            
            while (true) {
                Socket clientSocket = listenSocket.accept();
                connection = new Connection(clientSocket, serverGUI, activeDrones);
                connections.add(connection);
                
            }
        } catch (IOException ex) {
            serverGUI.generateMessage("!!!Error: " + ex.getMessage());
        }
    }
    
    
    private void loadFire() {
        
    }
    
    private void deleteFireReport() {
        
    }
    
    public void recallDrones() {
        for(int i =0;i<connections.size();i++){
            connections.get(i).recallDrones();
        }
    }
    
    public void moveDrone(int id, int x, int y) {
        for(int i =0;i<connections.size();i++){
            connections.get(i).moveDrone(id, x, y);
        }
       
        
    }
    
    private void ShutDownServer() {
        //recall drones before shut down the server
        recallDrones();
    }
    
    public static void main(String args[]) {
        
        //Run Server GUI
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                serverGUI.setVisible(true);
                
            }
        });
        new Server().runServer();
        
        
    }
}

class Connection extends Thread {
    
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket clientSocket;
    ServerGUI serverGUI;
    
    private int MAX_X = 872;
    private int MAX_Y = 642;
    HashMap<Integer, Drone> activeDrones;
    
    public Connection(Socket aClientSocket, ServerGUI serverGUI, HashMap<Integer,Drone> activeDrones) {
        this.serverGUI = serverGUI;
        this.activeDrones = activeDrones;
        
        try {
            clientSocket = aClientSocket;
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.start();
        } catch (IOException e) {
            new ServerGUI().generateMessage("Connection: " + e.getMessage());
        }
    }
    
    public void run() {
        try {
            String message = (String) in.readObject();
            while(!message.equals("registerDone")){
                if (message.equals("register")) {
                    out.writeObject("registerApproved");
                    Drone drone = (Drone) in.readObject();
                    if (activeDrones.containsKey(drone.getId())) {
                        out.writeObject("duplicated");
                    } else {
                        activeDrones.put(drone.getId(), drone);
                        serverGUI.addDroneToMap(drone);
                        serverGUI.generateMessage("Successfully added drone " + drone.getDroneName() + "\n");
                        out.writeObject("registerSuccessfully");
                        
                    }
                    message = (String)in.readObject();
                }
            }
            
            while (true) {
                TimeUnit.SECONDS.sleep(10);
                updateDronesLocation();
            }
            
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {/*close failed*/
            }
        }
    }
    
    private void checkFireLocation(Fire fire) {
        
        int x = fire.getX();
        int y = fire.getY();
        if (x > 0 || y > 0 || x < MAX_X || y < MAX_Y) {
            serverGUI.generateMessage("Fire detected at " + fire.getX() + "X and " + fire.getY() + "Y\n");
            serverGUI.addFireToMap(fire);
            
        }
    }
    
    private void updateDronesLocation() {
        try {
            out.writeObject("getLocation");
            Drone drone = (Drone) in.readObject();
            int x = drone.getX();
            int y = drone.getY();
            if (x < 0 || y < 0 || x > MAX_X || y > MAX_Y) {
                returnToSafeZone();
            } else {
                serverGUI.moveDrone(drone);
                saveDroneDetails(drone);
                
                List<Fire> fires = drone.getFires();
                
                if (fires != null) {
                    for (int i = 0; i < fires.size(); i++) {
                        checkFireLocation(fires.get(i));
                        
                    }
                    fires.clear();
                }
                
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void moveDrone(int droneId, int x, int y){
        
        try {
            out.writeObject("move");
            String message = (String)in.readObject();
            if(message.equals("getDrone")){
                Drone drone = activeDrones.get(droneId);
                drone.setX(x);
                drone.setY(y);
                
                out.writeObject(drone);
                String updateMessage = (String)in.readObject();
                if(updateMessage.equals("updateLocation")){
                    serverGUI.moveDrone(drone);
                }else if(updateMessage.equals("stayStill"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void saveDroneDetails(Drone drone) {
        
    }
    
    private void returnToSafeZone() {
        System.out.println("returning to safe zone");
    }
    
    public void recallDrones(){
        serverGUI.generateMessage("Recalling drones to base");
        try {
            //edit csv file
            
            //send recall messages to drones
            out.writeObject("recall");
            
            //update position on map
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
