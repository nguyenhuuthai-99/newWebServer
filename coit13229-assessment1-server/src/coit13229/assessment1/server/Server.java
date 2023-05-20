/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package coit13229.assessment1.server;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.ByteArrayOutputStream;
import model.Drone;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
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

    private final String path = System.getProperty("user.dir");
    private final int SERVER_PORT = 8886;
    private ServerSocket listenSocket;
    private static ServerGUI serverGUI = new ServerGUI();
    private HashMap<Integer, Drone> activeDrones = new HashMap();
    ArrayList<Connection> connections = new ArrayList<>();
    private HashMap<Integer, Fire> activeFires = new HashMap<>();
    Connection connection;

    private void runServer() {
        serverGUI.setServer(this);
        serverGUI.generateMessage("Adding existed fire to map\n");
        loadFire();

        try {
            listenSocket = new ServerSocket(SERVER_PORT);

            
            new Thread(){
                public void run(){
                    while (true) {
                        try {
                            Socket clientSocket = listenSocket.accept();
                            connection = new Connection(clientSocket, serverGUI, activeDrones, activeFires);
                            connections.add(connection);
                        } catch (IOException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                
            }
                }
            }.start();
            
//            while(true){
//                TimeUnit.SECONDS.sleep(10);
//                updateLocation();
//            }
            
        } catch (IOException ex) {
            serverGUI.generateMessage("!!!Error: " + ex.getMessage());
        } 
    }

    private void loadFire() {

        try {
            CSVReader reader = new CSVReader(new FileReader(path + "/src/coit13229/assessment1/server/resources/fires.csv"));
            String[] nextLine = reader.readNext();
            //reads one line at a time  
            while ((nextLine = reader.readNext()) != null) {
                int x = Integer.parseInt(nextLine[1]);
                int y = Integer.parseInt(nextLine[2]);
                int id = Integer.parseInt(nextLine[0]);
                int reportedDroneId = Integer.parseInt(nextLine[3]);
                int severity = Integer.parseInt(nextLine[4]);

                Fire fire = new Fire(x, y);
                fire.setId(id);
                fire.setReportedDroneId(reportedDroneId);
                fire.setServerity(severity);

                activeFires.put(fire.getId(), fire);

                serverGUI.addFireToMap(fire);

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateLocation(){
        for(Connection c: connections){
            c.updateDronesLocation();
        }
    }
    
    public void deleteFireReport(int fireId) {
        serverGUI.generateMessage("deleting fire " + fireId + "\n");
        activeFires.remove(fireId);

        try {
            CSVReader reader = new CSVReader(new FileReader(path + "/src/coit13229/assessment1/server/resources/fires.csv"));
            List<String[]> allFires = reader.readAll();

            //remove fire from the list
            String[] foundFire = null;
            for (String[] fireData : allFires) {
                if (fireData[0].equals("FireId")) {
                    continue;
                }
                if (Integer.parseInt(fireData[0]) == fireId) {
                    foundFire = fireData;
                }
            }
            allFires.remove(foundFire);

            //update csv file
            FileWriter fw = new FileWriter(path + "/src/coit13229/assessment1/server/resources/fires.csv");
            CSVWriter writer = new CSVWriter(fw, ',', CSVWriter.NO_QUOTE_CHARACTER);
            writer.writeAll(allFires);
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void recallDrone(int id) {
        for (int i = 0; i < connections.size(); i++) {
            connections.get(i).recallDrone(id);
        }
    }

    public void moveDrone(int id, int x, int y) {
        for (int i = 0; i < connections.size(); i++) {
            connections.get(i).moveDrone(id, x, y);
        }

    }

    public void shotDownServer() {
        //recall drones before shut down the server
        for (int id : activeDrones.keySet()) {
            recallDrone(id);
        }

        try {
            for (int i = 0; i < connections.size(); i++) {
                connections.get(i).shotDownServer();
            }
            listenSocket.close();
            serverGUI.generateMessage("The server is now shot\n");
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    private final String PATH = System.getProperty("user.dir");
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket clientSocket;
    ServerGUI serverGUI;

    private int MAX_X = 872;
    private int MAX_Y = 642;
    HashMap<Integer, Fire> activeFires;
    HashMap<Integer, Drone> activeDrones;

    public Connection(Socket aClientSocket, ServerGUI serverGUI, HashMap<Integer, Drone> activeDrones, HashMap<Integer, Fire> activeFires) {
        this.serverGUI = serverGUI;
        this.activeDrones = activeDrones;
        this.activeFires = activeFires;

        try {
            clientSocket = aClientSocket;
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.start();
        } catch (IOException e) {
            new ServerGUI().generateMessage("Connection: " + e.getMessage() + "\n");
        }
    }

    public void run() {
        try {
            String message = (String) in.readObject();
            while (!message.equals("registerDone")) {
                if (message.equals("register")) {
                    out.writeObject("registerApproved");
                    Drone drone = (Drone) in.readObject();
                    if (activeDrones.containsKey(drone.getId())) {
                        out.writeObject("duplicated");
                    } else {
                        activeDrones.put(drone.getId(), drone);
                        serverGUI.addDroneToMap(drone);
                        saveDroneDetails(drone);
                        serverGUI.generateMessage("Successfully added drone " + drone.getDroneName() + "\n");
                        out.writeObject("registerSuccessfully");

                    }
                    message = (String) in.readObject();
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
            activeFires.put(fire.getId(), fire);

            saveFire(fire);
        }
    }

    private void saveFire(Fire fire) {
        File fireFile = new File(PATH + "/src/coit13229/assessment1/server/resources/fires.csv");
        try {
            FileWriter outputfile = new FileWriter(fireFile, true);

            String fireData = fire.getId() + "," + fire.getX() + "," + fire.getY() + "," + fire.getReportedDroneId() + "," + fire.getServerity() + "\n";

            outputfile.write(fireData);
            outputfile.close();

        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateDronesLocation() {
        try {
            out.writeObject("getLocation");
            Drone drone = (Drone) in.readObject();
            int x = drone.getX();
            int y = drone.getY();
            if (x < 0 || y < 0 || x > MAX_X || y > MAX_Y) {
                returnToSafeZone(drone.getId());
            } else {
                serverGUI.moveDrone(drone);

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

    public void moveDrone(int droneId, int x, int y) {

        try {
            out.writeObject("move");
            String message = (String) in.readObject();
            if (message.equals("getDrone")) {
                Drone drone = activeDrones.get(droneId);
                drone.setX(x);
                drone.setY(y);

                out.writeObject(drone);
                String updateMessage = (String) in.readObject();
                if (updateMessage.equals("updateLocation")) {
                    serverGUI.moveDrone(drone);
                } else if (updateMessage.equals("stayStill"));
            }
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveDroneDetails(Drone drone) {
        File droneFile = new File(PATH + "/src/coit13229/assessment1/server/resources/droneDetails.bin");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try (FileOutputStream fos = new FileOutputStream(droneFile, true)){
            out = new ObjectOutputStream(bos);
            out.writeObject(drone);
            out.flush();
            byte[] data = bos.toByteArray();
            
            //write drone data to binary file
            fos.write(data);

        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    private void returnToSafeZone(int id) {
        Drone currentDrone = activeDrones.get(id);
        serverGUI.generateMessage("the drone" + id + " has crossed the boundary. Returning the drone\n");
        moveDrone(id, currentDrone.getX(), currentDrone.getY());
    }

    public void recallDrone(int id) {
        try {
            //edit csv file

            //send recall messages to drones
            out.writeObject("recall");
            String message = (String) in.readObject();
            if (message.equals("recalling")) {
                out.writeInt(id);
                serverGUI.generateMessage("Drone " + id + " is returning to bay\n");
            }

        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void shotDownServer() {
        try {

            out.writeObject("shotDownServer");
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
