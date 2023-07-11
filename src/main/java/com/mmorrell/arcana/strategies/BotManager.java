package com.mmorrell.arcana.strategies;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BotManager {

    private final List<OpenBookBot> botList = new ArrayList<>();

    public void createNewBot(OpenBookBot bot) {
        // bot.getStrategy().start();
        botList.add(bot);
    }

    public List<OpenBookBot> getBotList() {
        return botList;
    }
}
