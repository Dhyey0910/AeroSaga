package com.aerosaga;

public class Step {
    public String name;
    public String status; // PENDING | ACTIVE | COMPLETED | FAILED

    public Step() {}
    public Step(String name, String status) { this.name = name; this.status = status; }
}
