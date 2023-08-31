package com.mmorrell.arcana.strategies;

import lombok.Data;
import org.p2p.solanaj.core.Account;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Data
public class BotManager {

    private Account tradingAccount = new Account();
    private final List<OpenBookBot> botList = new ArrayList<>();

    public void addBot(OpenBookBot bot) {
        bot.getStrategy().start();
        botList.add(bot);
    }

    public List<OpenBookBot> getBotList() {
        return botList;
    }

    public void stopBot(long botId) {
        OpenBookBot bot = botList.get((int) botId);
        bot.getStrategy().stop();
        botList.remove((int) botId);
    }
}
