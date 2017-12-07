/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package htm;

import graph.AbstractNetworkNode;
import graph.NodeInterface;


/**
 *
 * @author farmetta
 */
public class MyColumn extends AbstractNetworkNode {

    /**
     * TODO : Au cours de l'apprentissage, chaque colonne doit atteindre un taux d'activation.
     * Une colonnne est activée si elle reçoit suffisament de retours positif de ses synapses 
     * (le retour est positif si la synapse est active et que son entrée associée l'est également).
     * 
     * Pour l'apprentissage, parcourir les synapses en entrée, et faire évoluer les poids synaptiques adéquatement.
     * 
     */

    private double activatedRate;
    private double overlapDutyCycle;
    private int iterationsActive;
    private int iterationsOverlap;
    private double boost;
    
    public MyColumn(NodeInterface _node) {
        super(_node);
        activatedRate = 0.0;
        boost = 1.0;
        overlapDutyCycle = 0.0;
    }

    public double getOverlapDutyCycle() {
        return overlapDutyCycle;
    }

    public double getBoost() {
        return boost;
    }

    public void setBoost(double boost) {
        this.boost = boost;
    }

    public double getActivatedRate() {
        return activatedRate;
    }

    public void setActivatedRate(double a) {
        activatedRate = a;
    }

    public int getIterationsActive() {
        return iterationsActive;
    }

    public void setIterationsActive(int iterationsActive) {
        this.iterationsActive = iterationsActive;
    }

    public void setOverlapDutyCycle(double overlapDutyCycle) {
        this.overlapDutyCycle = overlapDutyCycle;
    }

    public int getIterationsOverlap() {
        return iterationsOverlap;
    }

    public void setIterationsOverlap(int iterationsOverlap) {
        this.iterationsOverlap = iterationsOverlap;
    }
}


