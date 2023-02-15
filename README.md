# SPL_Assignment_2  
In this assignment we were required to implement a simple version of the game **“Set”** in java    
and the main focuses of this Assignment were:**Java Concurrency**, **Synchronization** and **Testing**.  
(The assignment PDF is in the files)   

## What have I learned from this assignment 🤔    

### - How to use MultiThreading in Java     
  
 **how** , **when** and **why** to **or why not** to use each one of   
 the functions of the Java Thread Class ,threads management and more.   
   
### - How to recognize and deal with MultiThreading problems such as deadlocks,livelocks and more   
  
While working on the project , we faced a lot of different problems ,some were **easily detectable**,      
for example when unwanted results appeared when we firstly used a queue   
(for the actionQueue-Queue for incoming actions where actions are adding/removing a token from the table)   
and a few threads tried inserting non-stop into it,We fixed it by using the wait&notify mechanism and a Blocking Queue.

And some were a bit **harder to detect** ,like a problem     
we encountered midway ,during the implementation          
we noticed that in 1 in a few runs of the game with the "AI" players ,there was a deadlock (thread went into a blocking state       
and never got out of it).  
   
 **(a part of the program flow is the following :player claims set->goes into wait->dealer checks->wakes the player up)**         
It took us a bit of time to figure out what was the source of the problem ,but at the end ,It turned out to be    
a situation where 1 player claimed his set ,it was a set ,the dealer removed it ,but a player 2 had 1 of the   
cards which were removed in his set and they were removed after he claimed the set.    
And when the dealer checked player 2 set ,he saw that he didn't have 3 cards in his set so player 2 didn't even enter the    
main loop of the function where the player is notified.  
We've dealt with that by making sure the player is notified even if he  doesn't enter the main loop.
  
### - How to use different synchronization tools to ensure thread safety 🛠️           
### - Event Handling using different tools     
    
### - Experience in Program flow planning     
by designing the program flow and planing the work strategy beforehand         
we managed to finish the assignment with all of the available bonus requirements    
a week before the due date and without adding any additional functions to the original skeleton files given to us.  
To top it off,we got 100 on this assignment.          
