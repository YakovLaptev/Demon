package com.yakovlaptev.demon.chatmessages.waitingtosend;

import java.util.ArrayList;
import java.util.List;

public class WaitingToSendQueue {

    private final List<WaitingToSendListElement> waitingToSend;

    private static final WaitingToSendQueue instance = new WaitingToSendQueue();

    public static WaitingToSendQueue getInstance() {
        return instance;
    }

    private WaitingToSendQueue() {
        waitingToSend = new ArrayList<>();
    }

    public List<String> getWaitingToSendItemsList(int tabNumber) {

        //to remap the tabNumber index to the waitingToSend list's index, this method
        //uses only "(tabNumber - 1)".

        //if tabNumber index in between 0 and size-1
        if ((tabNumber - 1) >= 0 && (tabNumber - 1) <= waitingToSend.size() - 1) {

            //if this element is null i set the WaitingToSendListElement() at tabNumber-1
            if(waitingToSend.get((tabNumber - 1)) == null) {
                waitingToSend.set((tabNumber - 1), new WaitingToSendListElement());
            }

            //if is !=null, do nothing, because i have the list ready and probably with elements
            //and i can't lost this elements
        } else {

            //if the tabNumber index is not available, i add a new WaitingToSendListElement ad the end of the waitingToSend List.
            waitingToSend.add((tabNumber - 1), new WaitingToSendListElement());
        }

        return waitingToSend.get((tabNumber - 1)).getWaitingToSendList();
    }
}
