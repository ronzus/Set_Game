package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    /**
     * Queue for set claims.
     */
   public Queue<int[]> setClaims;
    /**
     * timer for timeout of dealer thread.
     */
    public long timeoutSleep;
    /**
     * time of timour of dealer when we want him to hurry during a reset(reshuffle and reset of the clock)
     * so he won't skip a second (normal timout is 1 second) ,in comparison this is 10 milliseconds.
     */
    public long fasterTimeoutSleepVal;
    /**
     * standard timeout for dealer .
     */
    public long normalTimeoutSleepVal;

    /**
     * Queue of Players which are claiming a set.
     */
    public Queue<Player> playersQueue;
    /**
     * Lock for the Dealer Thread.
     */
    public final Object dLock;
    /**
     * wait for all cards to be placed - for AI player, so it couldn't continue pressing - boolean condition.
     */
    public volatile boolean reset;
    /**
     * array of winners.
     */
    public LinkedList<Integer> Winners;
    /**
     * size of a legal set
     */
    private final static int setSize=3;
    /**
     * first element in LinkedList/Array ,index 0 in that object
     */
    private int firstElement;
    /**
     * For indication of the Game modes.
     */
    public int gMode;
    /**
     * size of a set
     */
    private final int gameMode1;
    /**
     * size of a set
     */
    private final int gameMode2;
    /**
     * size of a set
     */
    private final int gameMode3;
    /**
     * For Game mode 2:
     * timerG2-timer since the last action(point or reshuffle)
     */
    public long timerG2;

    /**
     * For the Bonus!!
     */
    public LinkedList<Player> ThreadOrder;
    public boolean buttonx;





    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.setClaims=new LinkedList<int[]>();
        this.gameMode1=1;
        this.gameMode2=2;
        this.gameMode3=3;
        this.fasterTimeoutSleepVal=10;
        this.normalTimeoutSleepVal=1000;
        this.firstElement=0;
        this.ThreadOrder=new LinkedList<Player>();
        if(env.config.turnTimeoutMillis<0){
            this.gMode=gameMode1;
        }
        else if(env.config.turnTimeoutMillis==0){
            this.gMode=gameMode2;
            timerG2 = env.config.turnTimeoutMillis + System.currentTimeMillis();
        }
        else {
            this.gMode=gameMode3;
        }
        this.timeoutSleep=fasterTimeoutSleepVal;//we shuffle and put cards in the start .
        this.playersQueue = new LinkedList<Player>();
        this.dLock=new Object();
        this.reset =true;
        this.Winners = new LinkedList<Integer>();
        this.buttonx=false;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        for(Player player:players){
            Thread playerT =new Thread(player,"player"+player.id);
            playerT.start();
        }
        while (!shouldFinish()) {
            placeCardsOnTable();
            if(terminate){
                break;
            }
            timerLoop();
            if(terminate&buttonx){
                break;
            }
            updateTimerDisplay(true);
            if(terminate&buttonx){
                break;
            }
            removeAllCardsFromTable();
        }
        if(!buttonx) {
            announceWinners();
        }
        for(Player p:ThreadOrder){
            p.terminate();
        }

        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reset =false;
        if(gMode==gameMode3) {
            reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
        }
        if(gMode==gameMode2){
            timerG2 = env.config.turnTimeoutMillis + System.currentTimeMillis();
        }
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            if(terminate){
                break;
            }
            updateTimerDisplay(false);
            if(terminate){
                break;
            }
            removeCardsFromTable();
            if(terminate){
                break;
            }
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {//terminating in reverse order in which we initialized them
        // TODO implement
        terminate=true;
        buttonx=true;






    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    public void removeCardsFromTable() {
        // TODO implement
        if (!terminate&&!playersQueue.isEmpty()) {
            Player player = playersQueue.poll();
            while (player == null) {
                player = playersQueue.poll();
            }
            if (!terminate&&player.tokensPlacement != null) {
                if (player.tokensPlacement.size() == setSize) {
                    int[] arr = player.tokensPlacement.stream().mapToInt(Integer::intValue).toArray();
                    LinkedList<Integer> cardsOfPlayer = new LinkedList<>();
                    for (int i = firstElement; i < setSize; i++) {
                        if (table.slotToCard[arr[i]] != null)
                            cardsOfPlayer.add(table.slotToCard[arr[i]]);
                        else {
                            player.tokensPlacement.removeFirstOccurrence(arr[i]);
                        }
                    }
                    if (!terminate&&cardsOfPlayer.size() == setSize) {
                        if (env.util.testSet(cardsOfPlayer.stream().mapToInt(Integer::intValue).toArray())) {
                            player.point = true;
                            timeoutSleep = fasterTimeoutSleepVal;
                            if (gMode == gameMode3) {
                                reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
                            } else if (gMode == gameMode2) {
                                timerG2 = env.config.turnTimeoutMillis + System.currentTimeMillis();
                            }

                            for (int slot : player.tokensPlacement) {
                                for(Player player1:players){
                                    if(player.id!=player1.id&player1.tokensPlacement.contains(slot)){
                                        player1.tokensPlacement.removeFirstOccurrence(slot);
                                    }
                                }
                                env.ui.removeTokens(slot);
                                table.removeCard(slot);
                            }
                            player.tokensPlacement.clear();
                        }
                        else {
                            player.penalty = true;
                            timeoutSleep = fasterTimeoutSleepVal;
                        }
                    }
                }
                synchronized (player.responseLock) {
                    player.responseLock.notify();
                }
            }
        }
        if(shouldFinish()){
        LinkedList<Integer> check=new LinkedList<Integer>();
        check.addAll(Arrays.asList(table.slotToCard));
        check.addAll(deck);
        check.removeIf(Objects::isNull);
                if (env.util.findSets(check, 1).size()==0) {
                    terminate = true;
                }
            }
    }




    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    void placeCardsOnTable() {
        // TODO implement
        Collections.shuffle(deck);
        for (int i = 0; i < env.config.tableSize; i++) {
            if (table.slotToCard[i] == null) {
                if (!deck.isEmpty()) {
                    table.placeCard(deck.remove(firstElement), i);
                }
            }
        }
        if (gMode == gameMode2 || gMode == gameMode1) {
            while (!deck.isEmpty() && env.util.findSets(Arrays.asList(table.slotToCard), 1).size() == 0&&!terminate) {
                removeAllCardsFromTable();
                Collections.shuffle(deck);
                for (int i = 0; i < env.config.tableSize; i++) {
                    if (table.slotToCard[i] == null) {
                        if (!deck.isEmpty()) {
                            table.placeCard(deck.remove(firstElement), i);
                        }
                    }
                }
                if (gMode == gameMode2) {
                    timerG2 = env.config.turnTimeoutMillis + System.currentTimeMillis();
                }
            }
        }
        if (env.config.hints) {
            table.hints();
        }

        reset = false;
        for (Player p : players) {
            synchronized (p.aiLock) {
                p.aiLock.notify();
            }
            synchronized (p.resLock) {
                p.resLock.notify();
            }
        }
    }


    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        if(!terminate) {
            synchronized (dLock) {
                try {
                    dLock.wait(timeoutSleep);
                } catch (InterruptedException ignored) {
                }
            }
        }

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement

        if(!terminate&&gMode==gameMode2) {
            if (reset) {
                this.reset = true;
                timeoutSleep = fasterTimeoutSleepVal;
                env.ui.setElapsed(env.config.turnTimeoutMillis);
                timerG2 = env.config.turnTimeoutMillis + System.currentTimeMillis();
            }
            else{
                env.ui.setElapsed(System.currentTimeMillis()-timerG2);
                timeoutSleep=normalTimeoutSleepVal;
            }

        }
        else if(!terminate&&gMode==gameMode3) {
            if (reset) {
                this.reset = true;

                if (reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis) {
                    timeoutSleep=fasterTimeoutSleepVal;
                    env.ui.setCountdown(env.config.turnTimeoutMillis, true);
                } else {
                    timeoutSleep = fasterTimeoutSleepVal;
                    env.ui.setCountdown(env.config.turnTimeoutMillis, false);
                }
                reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
            } else if (reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis) {
                timeoutSleep = fasterTimeoutSleepVal;
                env.ui.setCountdown(((reshuffleTime - System.currentTimeMillis())), true);
            } else {
                timeoutSleep = normalTimeoutSleepVal;
                env.ui.setCountdown((reshuffleTime - System.currentTimeMillis()), false);
            }
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    void removeAllCardsFromTable() {

        // TODO implement
        for(int i=0;i<env.config.tableSize;i++){
            if(table.slotToCard[i]!=null) {
                deck.add(table.slotToCard[i]);
                table.removeCard(i);
            }
        }
        for(Player player : players){
            player.ActionsQueue.clear();
            player.tokensPlacement.clear();
            player.penalty=false;
        }
        if(shouldFinish()){
            terminate=true;
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        int highestScore = 0;
        for(Player player : players){
            if(player.score()>highestScore)
                highestScore = player.score();
        }
        for(Player player : players){
            if(player.score()==highestScore) {
                Winners.add(player.id);
            }
        }
        env.ui.announceWinner(Winners.stream().mapToInt(Integer::intValue).toArray());

    }
}
