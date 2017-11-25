/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package htm;

import graph.EdgeBuilder;
import graph.EdgeInterface;
import graph.NodeBuilder;
import graph.NodeInterface;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author farmetta
 */
public class MyNetwork implements Runnable {

    private static final int DENSITE_INPUT_COLUMNS = 8;
    private static final int DESIRED_LOCAL_ACTIVITY = 3;
    private static final int MIN_OVERLAP = 3;
    private static final int NB_ENTRIES = 16;

    private static final double LINEAR_COEF = 6.25; // 0.5/0.008 => boostMax / minDutyCycle
    private static final double PERM_CHANGE = 0.1;
    private static final double INCREMENTAL_COEF = 0.1;

    public int nbIterations;

    private NodeBuilder nb;
    private EdgeBuilder eb;
    
    ArrayList<MyNeuron> lstMN;
    ArrayList<MyColumn> lstMC;
    
    
    public MyNetwork(NodeBuilder _nb, EdgeBuilder _eb) {
        nb = _nb;
        eb = _eb;
    }


    public void buildNetwork(int nbInputs, int nbColumns) {
        // création des entrées
        lstMN = new ArrayList<MyNeuron>();
        for (int i = 0; i < nbInputs; i++) {
            NodeInterface ni = nb.getNewNode();
            MyNeuron n = new MyNeuron(ni);
            n.getNode().setPosition(i, 0);
            ni.setAbstractNetworkNode(n);
            lstMN.add(n);
        }
        // création des colonnes
        lstMC = new ArrayList<MyColumn>();
        for (int i = 0; i < nbColumns; i++) {
            NodeInterface ni = nb.getNewNode();
            MyColumn c = new MyColumn(ni);
            c.getNode().setPosition(i*2, 2);
            ni.setAbstractNetworkNode(c);
            
            lstMC.add(c);
        }
        
        Random rnd = new Random();
        // Connection entre entrées et colonnes
        for (int i = 0; i < DENSITE_INPUT_COLUMNS * lstMC.size(); i++) {
            
            MyNeuron n = lstMN.get(rnd.nextInt(lstMN.size()));
            MyColumn c = lstMC.get(rnd.nextInt(lstMC.size()));
            
            if (!n.getNode().isConnectedTo(c.getNode())) {
                EdgeInterface e = eb.getNewEdge(n.getNode(), c.getNode());
                MySynapse s = new MySynapse(e);
                e.setAbstractNetworkEdge(s);
                
            } else {
                i--;
            }
        }
        
        
    }

    @Override
    public void run() {

        nbIterations = 1;
        int entree = 0; //nos entrées sont des nombres allant de 1 à 16

        GenerateRandomSystem();

        while (true) {

            HashMap<MyColumn, Integer> overlap = new HashMap<>();
            List<MyColumn> activeColumns = new ArrayList<>();
            int minLocalActivity = 0;

            encodeEntry(entree);

            // Calcul de l'overlap
            for (MyColumn c : lstMC) {

                int tempOverlap =0;

                for (EdgeInterface e : c.getNode().getEdgeIn()) {
                    tempOverlap += ((MyNeuron)e.getNodeIn().getAbstractNetworkNode()).getActivation();
                }

                if (tempOverlap < MIN_OVERLAP){
                    tempOverlap = 0;
                } else {
                    tempOverlap = ((Double) (tempOverlap * c.getBoost())).intValue();
                }
                overlap.put(c, tempOverlap);
            }

            // Inhibition
            for (MyColumn c : lstMC) {

                minLocalActivity = kthScore(c, overlap, DESIRED_LOCAL_ACTIVITY);

                if(overlap.get(c) > 0 && overlap.get(c) >= minLocalActivity) {
                    activeColumns.add(c);
                    c.getNode().setState(NodeInterface.State.ACTIVATED);
                } else {
                    c.getNode().setState(NodeInterface.State.DESACTIVATED);
                }
            }

            /*System.out.println("Winners : " );
            for(MyColumn c : activeColumns) {
                System.out.println("- " + c.toString() + "overlap : " + overlap.get(c));
            }*/

            // Learning
            for(MyColumn c : activeColumns) {

                for (EdgeInterface e : c.getNode().getEdgeIn()) {
                    ((MySynapse) e.getAbstractNetworkEdge()).currentValueUdpate(
                            ((MyNeuron) e.getNodeIn().getAbstractNetworkNode()).getActivation() == 1 ?
                                    PERM_CHANGE : -PERM_CHANGE
                    );
                    sleep();
                }
            }

            // Boosting
            for(MyColumn c : lstMC) {

                // Column boost
                double minDutyCycle = 0.1 * getMaxDutyCycle(neighbors(c));
                updateActivatedRate(c, activeColumns.contains(c));
                c.setBoost(getNewBoost(c.getActivatedRate(), minDutyCycle));

                //Synapse boost
                updateOverlapDutyCycle(c, overlap.get(c) > MIN_OVERLAP);
                if(c.getOverlapDutyCycle() < minDutyCycle) {
                    increasePermanences(c, INCREMENTAL_COEF);
                }
                sleep();
            }

            double inhibitionRadius = averageReceptiveFieldSize();

            entree = (entree + 1) % 16;
            nbIterations++;
        }
    }

    /***
     * Permet de rafraichir la vue
     */
    private void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(MyNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /***
     * Code de ML et FA
     * Initialise aléatoirement l'activation des noeuds et synapses
     */
    private void GenerateRandomSystem() {
        for (MyColumn c : lstMC) {

            if (new Random().nextBoolean()) {
                c.getNode().setState(NodeInterface.State.ACTIVATED);
            } else {
                c.getNode().setState(NodeInterface.State.DESACTIVATED);
            }

            for (EdgeInterface e : c.getNode().getEdgeIn()) {


                ((MySynapse) e.getAbstractNetworkEdge()).currentValueUdpate(new Random().nextDouble() - 0.5);

                MyNeuron n = (MyNeuron) e.getNodeIn().getAbstractNetworkNode(); // récupère le neurone d'entrée
                if (new Random().nextBoolean()) {
                    n.setActivation(1);
                    n.getNode().setState(NodeInterface.State.ACTIVATED);
                } else {
                    n.setActivation(0);
                    n.getNode().setState(NodeInterface.State.DESACTIVATED);
                }


                sleep();
            }
        }
    }

    /***
     * Retourne l'overlap de la colonne avec la k'ieme plus grande overlap
     * @param c
     * @param overlap
     * @param k
     * @return
     */
    private int kthScore(MyColumn c, HashMap<MyColumn, Integer> overlap, int k){

        MyColumn bestColumn = null;
        List<MyColumn> neighborsOfC = neighbors(c);
        HashMap<MyColumn, Integer> temp = new HashMap<>();

        for(MyColumn neighbor : neighborsOfC) {
            temp.put(neighbor, overlap.get(neighbor));
        }

        if (temp.size() >= k){
            for (int i = 0; i < k; i++){
                bestColumn = getBestOverlap(temp);
                temp.remove(bestColumn);
            }

            return overlap.get(bestColumn);

        } else {
            return -1;
        }
    }

    /***
     * Retourne le meilleur overlap de la liste
     * @param overlap
     * @return
     */
    private MyColumn getBestOverlap(HashMap<MyColumn, Integer> overlap) {
        MyColumn bestColumn = null;
        int bestValue = -1;

        for (Map.Entry<MyColumn, Integer> entry : overlap.entrySet()){
            if (entry.getValue() > bestValue){
                bestValue = entry.getValue();
                bestColumn = entry.getKey();
            }
        }

        return bestColumn;
    }

    /***
     * Fonction d'encodage correspondant à la permutation circulaire d'un groupe de bits "1" débutant à l'indice i
     * Pour un i compris entre 0 et 15
     * Enregistre directement l'entrée dans les neurones
     * @param i
     */
    private void encodeEntry(int i){

        int nbOnes = 5;
        ArrayList<Integer> entries = new ArrayList<>(NB_ENTRIES);

        for (int k = 0; k < NB_ENTRIES; k++){
            if (k >= i && k < i + nbOnes){
                entries.add(k,1);
            } else {
                entries.add(k,0);
            }
        }

        // cas où le groupe de 1s est coupé
        if (i + nbOnes > NB_ENTRIES){
            for (int k = 0; k < i + nbOnes - NB_ENTRIES; k++){
                entries.set(k,1);
            }
        }

        //mise à jour graphique
        for (int k = 0; k < NB_ENTRIES; k++){
            int bit = entries.get(k);
            lstMN.get(k).setActivation(bit);
            lstMN.get(k).getNode().setState(
                    bit == 1 ? NodeInterface.State.ACTIVATED : NodeInterface.State.DESACTIVATED
            );
            sleep();
        }
    }

    /***
     * Retourne le taux d'activation maximal sur toutes les colonnes
     * @param columns
     * @return
     */
    private double getMaxDutyCycle(List<MyColumn> columns) {
        double maxActiveDuty = 0;
        for(MyColumn c : columns) {
            maxActiveDuty = Math.max(maxActiveDuty, c.getActivatedRate());
        }
        return maxActiveDuty;
    }

    /***
     * Retourne le boost à partir du taux d'activation et du taux d'activation minimal
     * @param activeDutyCycle
     * @param minDutyCycle
     * @return
     */
    private double getNewBoost(double activeDutyCycle, double minDutyCycle) {
        if(activeDutyCycle > minDutyCycle) {
            return 1;
        }
        return 1 + ((minDutyCycle - activeDutyCycle) * LINEAR_COEF);
    }

    /***
     * Augmente la valeur de chaque synapse de la colonne c, par un facteur rate
     * @param c
     * @param rate
     */
    private void increasePermanences(MyColumn c, double rate) {
        for (EdgeInterface e : c.getNode().getEdgeIn()) {

            MySynapse s = ((MySynapse) e.getAbstractNetworkEdge());
            s.currentValueUdpate(rate * s.getTHRESHOLD());

            sleep();
        }
    }

    /***
     * Retourne le nombre moyen de synapses activés par colonne
     * @return
     */
    private double averageReceptiveFieldSize() {
        int nbSynapseActivated = 0;
        for(MyColumn c : lstMC) {
            for (EdgeInterface e : c.getNode().getEdgeIn()) {
                if(((MySynapse) e.getAbstractNetworkEdge()).isActivated()) {
                    nbSynapseActivated++;
                }
            }
        }
        return nbSynapseActivated / lstMC.size();
    }

    /***
     * Met à jour le taux d'activation de la colonne c
     * @param c
     * @param activated
     */
    private void updateActivatedRate(MyColumn c, boolean activated){
        if (activated) {
            c.setIterationsActive(c.getIterationsActive() + 1);
        }
        c.setActivatedRate(c.getIterationsActive() * 1.0 / nbIterations);
    }

    /***
     * Met à jour le overlapDutyCycle de la colonne c
     * @param c
     * @param overlapped
     */
    private void updateOverlapDutyCycle(MyColumn c, boolean overlapped) {
        if (overlapped) {
            c.setIterationsOverlap(c.getIterationsOverlap() + 1);
        }
        c.setOverlapDutyCycle(c.getIterationsOverlap() * 1.0 / nbIterations);
    }

    private List<MyColumn> neighbors(MyColumn c) {
        //TODO
        return lstMC;
    }
}
