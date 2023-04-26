/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.io.Serializable;

/**
 *
 * @author thainguyen
 */
public class Fire implements Serializable{
    private int Id;
    private int x;
    private int y;
    private int reportedDroneId;
    private int serverity;

    public Fire(int x, int y) {
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

    public int getReportedDroneId() {
        return reportedDroneId;
    }

    public void setReportedDroneId(int reportedDroneId) {
        this.reportedDroneId = reportedDroneId;
    }

    public int getServerity() {
        return serverity;
    }

    public void setServerity(int serverity) {
        this.serverity = serverity;
    }
    
    
    
}
