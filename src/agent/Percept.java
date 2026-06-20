package agent;

public class Percept {
    public final boolean stench, breeze, glitter, bump, scream;
    
    public Percept(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        this.stench = stench; 
        this.breeze = breeze; 
        this.glitter = glitter;
        this.bump = bump; 
        this.scream = scream;
    }
    
    @Override
    public String toString() {
        return String.format(
          "(냄새: %b, 바람: %b, 반짝임: %b, 부딪힘: %b, 비명: %b)",
          stench, breeze, glitter, bump, scream
        );
    }
}