package model;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author thainguyen
 */
public class Drone implements Serializable{
    private int Id;
    private String droneName;
    private ArrayList<Fire> fires;
    private int x;
    private int y;

    public Drone(String roneName, int x, int y) {
        this.droneName = roneName;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }    

    public String getDroneName() {
        return droneName;
    }

    public void setDroneName(String droneName) {
        this.droneName = droneName;
    }

    public ArrayList<Fire> getFires() {
        return fires;
    }

    public void setFires(ArrayList<Fire> fires) {
        this.fires = fires;
    }
    
    
}
