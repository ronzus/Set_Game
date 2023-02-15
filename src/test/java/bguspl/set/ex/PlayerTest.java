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
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        // Purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        player = new Player(env, dealer, table, 0, false);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    //The test that was given.
    @Test
    void pointScore() {

        // force table.countCards to return 3
        when(table.countCards()).thenReturn(3); // this part is just for demonstration

        // calculate the expected score for later
        int expectedScore = player.score() + 1;

        // call the method we are testing
        player.point();

        // check that the score was increased correctly
        assertEquals(expectedScore, player.score());

        // check that ui.setScore was called with the player's id and the correct score
        verify(ui).setScore(eq(player.id), eq(expectedScore));
    }

    //Our tests:

    //Checks that penalty don't remove the tokens in the ui, placed by the player
    @Test
    void testPenalty() {

        // Load tokens to players and to table
        player.tokensPlacement.add(1);
        player.tokensPlacement.add(6);
        player.tokensPlacement.add(8);
        ui.placeToken(0,1);
        ui.placeToken(0,6);
        ui.placeToken(0,8);

        // Checks that the tokens placed by the player is not a valid set.
        assertFalse(util.testSet(player.tokensPlacement.stream().mapToInt(Integer::intValue).toArray()));

        // Call the method we are testing
        player.penalty();

        // Verify that the penalty method did not change the tokens placed before.

        verifyNoInteractions(table);
    }


   @Test
    void testKeyPressed() {

       // Load tokens to players and to table
       int slot = 0;
       int newslot = 1;
       player.ActionsQueue.add(slot);

       // Checks that the tokens placed by the player is not a valid set.
       assertFalse(player.ActionsQueue.isEmpty());

       // Call the method we are testing
       player.keyPressed(newslot);

       // Verify that the penalty method did not change the tokens placed before.
       assertTrue(player.ActionsQueue.contains(newslot));
    }

}