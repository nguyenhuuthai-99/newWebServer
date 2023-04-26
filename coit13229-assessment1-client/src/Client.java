
import model.Drone;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Fire;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
/**
 *
 * @author thainguyen
 */
public class Client {
    
    public final int SERVER_PORT = 8887;
    ObjectInputStream in = null;
    ObjectOutputStream out = null;
    Drone drone;
    ArrayList<Fire> fires;   
    private final int BASE_X = 436;
    private final int BASE_Y = 321;
    int droneX = 436;
    int droneY = 321;
    
    private void runClient() {
        
        fires = new ArrayList();
        
        Socket socket = null;
        try {
            socket = new Socket("localhost", SERVER_PORT);
            
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            boolean isRegistered = registerDrone();
            while (isRegistered==false) {
                isRegistered = registerDrone();
            }
            new Thread() {
                public void run() {
                    while (true) {
                        try {
                            String message = (String) in.readObject();
                            switch (message) {
                                case "recall":
                                    recallDrone();
                                    break;
                                case "move":
                                    moveDrone();
                                    break;
                                case "getLocation":
                                    
                                    System.out.println("Getting Drone's location");
                                    //I don't know why i can't use drone object directly, so i did this one instead, and it works
                                    Drone d = new Drone(drone.getDroneName(), drone.getX(), drone.getY());
                                    d.setId(drone.getId());
                                    d.setFires(drone.getFires());
                                    out.writeObject(d);
                                    fires = new ArrayList();
                                    System.out.println("Location sent");
                                    break;
                                default:
                                
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                }
            }.start();
            
            while (true) {
                Random random = new Random();
                int randomMove = random.nextInt(4);
                switch (randomMove) {
                    case 0:
                        moveDown();
                        break;
                    case 1:
                        moveUp();
                        break;
                    case 2:
                        moveLeft();
                        break;
                    case 3:
                        moveRight();
                        break;
                    default:
                        throw new AssertionError();
                }
                TimeUnit.SECONDS.sleep(1);
                //System.out.println(droneX+" "+droneY);
            }
            
        } catch (UnknownHostException e) {
            System.out.println("Socket:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        } catch (InterruptedException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (socket != null) try {
                socket.close();
            } catch (IOException e) {
                System.out.println("close:" + e.getMessage());
            }
        }
    }
    
    private void moveDrone() {
        try {
            //String message = (String)in.readObject();
            out.writeObject("getDrone");
            Drone drone = (Drone)in.readObject();
            if(drone.getId()== this.drone.getId()){
                System.out.println("moving to new location");
                droneX = drone.getX();
                droneY = drone.getY();
                this.drone.setX(drone.getX());
                this.drone.setY(drone.getY());
                out.writeObject("updateLocation");
            }else{
                out.writeObject("stayStill");
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    private boolean registerDrone() {
        boolean isRegistered = false;
        //get drone details
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Drone Name:");
        String droneName = scanner.nextLine();
        System.out.println("Enter Drone ID:");
        int droneID = scanner.nextInt();
        drone = new Drone(droneName, droneX, droneY);
        drone.setId(droneID);
        
        try {
            out.writeObject("register");
            String message = (String) in.readObject();
            if (message.equals("registerApproved")) {
                out.writeObject(drone);
                String confirmMessage = (String) in.readObject();
                if (confirmMessage.equals("registerSuccessfully")) {
                    out.writeObject("registerDone");
                    isRegistered = true;
                } else if (confirmMessage.equals("duplicated")) {
                    System.out.println("The details you have entered was duplicated. Please try again with new details.");
                    isRegistered = false;
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isRegistered;
    }
    
    private void recallDrone() {
        System.out.println("returning to base");
        droneX = BASE_X;
        droneY = BASE_Y;
        drone.setX(BASE_X);
        drone.setY(BASE_Y);
        
    }
    
    private void moveRight() {
        droneX += 10;
        drone.setX(droneX);
        checkFire();
    }
    
    private void moveLeft() {
        droneX -= 10;
        drone.setX(droneX);
        checkFire();
    }
    
    private void moveUp() {
        droneY -= 10;
        drone.setY(droneY);
        checkFire();
    }
    
    private void moveDown() {
        droneY += 10;
        drone.setY(droneY);
        checkFire();
    }
    
    private void checkFire() {
        
        Random random = new Random();
        int hasFire = random.nextInt(30);
        
        if (hasFire == 1) {
            int fireSeverity = random.nextInt(10);
            System.out.println("Fire detected");
            Fire fire = new Fire(droneX, droneY);
            fire.setServerity(fireSeverity);
            fire.setReportedDroneId(drone.getId());
            fire.setId(drone.getX() * drone.getY());
            
            fires.add(fire);
            drone.setFires(fires);
        }
    }
    
    public static void main(String[] args) {
        new Client().runClient();
    }
    
}
