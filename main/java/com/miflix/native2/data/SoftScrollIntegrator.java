package com.miflix.native2.data;

/** Closed-form critically damped spring, with bounded frame time and speed. */
public final class SoftScrollIntegrator {
    private float velocity;
    public void reset() {velocity=0;}
    public float velocity() {return velocity;}
    public float step(float distance,float seconds,float viewport) {
        if(Float.isNaN(distance) || Float.isInfinite(distance)) {reset();return 0;}
        double dt=Math.max(.001,Math.min(.032,seconds));
        double w=14.0;
        if(distance*velocity<0) velocity=0;
        double limit=Math.min(Math.max(1,viewport)*7,Math.abs(distance)*w*.95);
        velocity=(float)Math.max(-limit,Math.min(limit,velocity));
        double coefficient=velocity-w*distance;
        double decay=Math.exp(-w*dt);
        double delta=distance+(-distance+coefficient*dt)*decay;
        velocity=(float)((velocity-w*coefficient*dt)*decay);
        double speedLimit=Math.max(1,viewport)*7;
        velocity=(float)Math.max(-speedLimit,Math.min(speedLimit,velocity));
        double maxDelta=speedLimit*dt;
        return (float)Math.max(-maxDelta,Math.min(maxDelta,delta));
    }
}
