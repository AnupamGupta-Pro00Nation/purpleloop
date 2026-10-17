package com.purpleloop.core;

import com.purpleloop.core.model.LoginEvent;

public class PurpleLoopApp {
    public static void main(String[] args) {

        // Create the first login event
        LoginEvent event1 = new LoginEvent();
        event1.ip = "192.168.1.10";
        event1.success = false;

        // Create the second login event
        LoginEvent event2 = new LoginEvent();
        event2.ip = "192.168.1.10";
        event2.success = false;

        // Create the third login event
        LoginEvent event3 = new LoginEvent();
        event3.ip = "192.168.1.20";
        event3.success = true;

        // Create the fourth login event
        LoginEvent event4 = new LoginEvent();
        event4.ip = "192.168.1.10";
        event4.success = false;

        // Store all events in an array
        LoginEvent[] events = {event1, event2, event3, event4};

        int count = 0;

        // Examine every login event
        for (int i = 0; i < events.length; i++) {

            if (events[i].success == false) {
                count++;

                System.out.println(
                    "Failed login from IP: " + events[i].ip
                );
            } else {
                System.out.println(
                    "Successful login from IP: " + events[i].ip
                );
            }
        }
        System.out.println("-----------------------------");
        System.out.println("Total login attempts: " + events.length);
        System.out.println("Failed login attempts: " + count);
    }
}