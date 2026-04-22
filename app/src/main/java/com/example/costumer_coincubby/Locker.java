package com.example.costumer_coincubby;

public class Locker {
    public enum Status {
        AVAILABLE, PAYMENT_REQUIRED, OCCUPIED, MAINTENANCE
    }

    private String id;
    private String size;
    private Status status;
    private double rate;

    public Locker(String id, String size, Status status, double rate) {
        this.id = id;
        this.size = size;
        this.status = status;
        this.rate = rate;
    }

    public String getId() { return id; }
    public String getSize() { return size; }
    public Status getStatus() { return status; }
    public double getRate() { return rate; }
}