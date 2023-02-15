package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealerTest {

    Dealer dealer;
    @Mock
    private List<Integer> deck;

    Player[] players;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;

    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(table.countCards()>= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).

        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        slotToCard=new Integer[env.config.tableSize];
        cardToSlot=new Integer[env.config.deckSize];
        table=new Table(env,slotToCard,cardToSlot);
        players=new Player[env.config.players];
        dealer = new Dealer(env, table,players);
        for(int i=0;i<env.config.players;i++){
            players[i]=new Player(env,dealer,table,i,i < env.config.humanPlayers);
        }




        Properties properties = new Properties();
        properties.put(deck, IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList()));

        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void placeCards() {

        // calculate the expected score for later
        int expectedScore = 12;

        // call the method we are testing
        try{
            dealer.placeCardsOnTable();
        }
        catch(NullPointerException irrelevant){
        }

        // check that the score was increased correctly
        assertEquals(expectedScore, table.countCards());


    }
    @Test
    void removeAllCards(){
        table.slotToCard[0]=0;
        table.slotToCard[1]=10;
        table.placeToken(0,0);
        table.placeToken(0,1);


        // calculate the expected score for later
        int expectedScore = 0;

        // call the method we are testing
        try {
            dealer.removeAllCardsFromTable();
        }
        catch (NullPointerException irrelevant){}

        // check that the count was decreased correctly
        assertEquals(expectedScore, table.countCards());
        verify(ui).removeTokens(0);
        verify(ui).removeTokens(1);


    }



}