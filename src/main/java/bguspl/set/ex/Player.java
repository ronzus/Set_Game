package bguspl.set.ex;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    /**
     * The Dealer.
     */
    private final Dealer dealer;
    /**
     * The Queue for the KeyPresses.
     */
    public BlockingQueue<Integer> ActionsQueue;
    /**
     * Linked list for slots on which the player put tokens on.
     */
    public LinkedList<Integer> tokensPlacement;
    /**
     * A lock for the first loop (Handling Action) in run.
     */
    public final Object actionLock;
    /**
     * A lock for the playerThread while he shouldn't work.
     */
    public final Object resLock;
    /**
     * size of a legal set
     */
    private static final int setSize=3;
    /**
     * a single second in milliseconds
     */
    private long oneSecond;
    /**
     * first element in LinkedList/Array ,index 0 in that object
     */
    private int firstElement;
    /**
     * Boolean for point!
     */
    public boolean point;
    /**
     * Boolean for penalty!
     */
    public boolean penalty;
    /**
     * A lock for waiting for response from dealer.
     */
    public final Object responseLock;
    /**
     * A lock for the AI actions.
     */
    public final Object aiLock;
    /**
     * for aiThread random Keypress.
     */
    public LinkedList<Integer> slotsAi;




    /**
     *
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.ActionsQueue = new LinkedBlockingQueue<Integer>(setSize);
        this.tokensPlacement = new LinkedList<Integer>();
        this.dealer = dealer;
        this.point=false;
        this.penalty=false;
        this.actionLock = new Object();
        this.responseLock = new Object();
        this.aiLock=new Object();
        this.resLock=new Object();
        this.slotsAi=new LinkedList<Integer>();
        this.oneSecond=1000;
        this.firstElement=0;
        for(int i=0;i<env.config.tableSize;i++){
            this.slotsAi.add(i);
        }

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        dealer.ThreadOrder.addFirst(this);
        if (!human) {
            createArtificialIntelligence();
        }
        while (!terminate) {
            // TODO implement main player
                while (!terminate&&(tokensPlacement.size() < setSize | penalty)) {
                    while(!terminate&&dealer.reset){
                        synchronized (resLock){
                            try{
                                resLock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    if(!terminate){
                        synchronized (actionLock) {
                            try {
                                actionLock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        if(!ActionsQueue.isEmpty()) {
                            int slot = ActionsQueue.remove();
                            if (table.slotToCard[slot] != null) {
                                if (tokensPlacement.contains(slot)) {
                                    tokensPlacement.removeLastOccurrence(slot);
                                    table.removeToken(id, slot);
                                    if (penalty) {
                                        penalty = false;
                                    }
                                } else if (penalty) {
                                } else {
                                    tokensPlacement.add(slot);
                                    table.placeToken(id, slot);
                                }
                            }
                        }
                    }
                }

        if(!terminate) {
            dealer.playersQueue.add(this);
            dealer.reset=true;
            synchronized (dealer.dLock) {
                dealer.dLock.notify();
            }
            synchronized (responseLock) {
                try {
                    responseLock.wait();
                } catch (InterruptedException e) {
                }
            }
            if (point) {
                point();
            } else if (penalty) {
                penalty();
            }
            synchronized (aiLock) {
                aiLock.notify();
            }
        }
            }

            if (!human) try {
                synchronized (aiLock) {
                    aiLock.notify();
                }
                aiThread.join();

            } catch (InterruptedException ignored) {
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");

    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                if((tokensPlacement.size()==setSize&!penalty)|dealer.reset){
                    synchronized (aiLock){
                        try{
                            aiLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                    Collections.shuffle(slotsAi);
                    keyPressed(slotsAi.get(firstElement));


            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement

        terminate=true;
        synchronized (resLock){resLock.notify();}
        synchronized (actionLock){actionLock.notify();}
        synchronized (responseLock){responseLock.notify();}
        synchronized (aiLock){aiLock.notify();}

        try {
            playerThread.join();
            if(!human) {
                aiThread.join();
            }
        } catch (InterruptedException e) {
        }





    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement

        if(ActionsQueue.size()<setSize) {
           ActionsQueue.add(slot);
            }
            //Notifying the player about action needed to be taken care of.

        synchronized (actionLock) {
            actionLock.notify();
        }

        }



    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        if(!terminate&&env.config.pointFreezeMillis>0) {
            try {
                long i=env.config.pointFreezeMillis;
                while(!terminate & i>0) {
                    env.ui.setFreeze(id, i);
                    if(i<1000) {
                        synchronized (this) {
                            wait(i);
                        }
                    }
                    else{
                        synchronized (this) {
                            wait(oneSecond);
                        }
                    }
                    i=i-oneSecond;
                }
                env.ui.setFreeze(id,0);

            } catch (InterruptedException e) {

            }
        }
        point = false;
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        if(!terminate&&env.config.penaltyFreezeMillis>0) {
            try {
                long i=env.config.penaltyFreezeMillis;
                while(!terminate &  i>0) {
                    env.ui.setFreeze(id, i);
                    if(i<1000) {
                        synchronized (this) {
                            wait(i);
                        }
                    }
                    else{
                        synchronized (this) {
                            wait(oneSecond);
                        }
                    }
                    i=i-oneSecond;
                }
                env.ui.setFreeze(id,0);

            } catch (InterruptedException e) {
            }
        }
    }

    public int score() {
        return score;
    }
}
