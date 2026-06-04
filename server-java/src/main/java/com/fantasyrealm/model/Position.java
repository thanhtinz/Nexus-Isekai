package com.fantasyrealm.model;
public record Position(float x, float y, int zoneId) {
    public static Position of(float x,float y,int z){return new Position(x,y,z);}
    public double distanceTo(Position o){
        float dx=x-o.x,dy=y-o.y; return Math.sqrt(dx*dx+dy*dy);
    }
}
