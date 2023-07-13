package com.mmorrell.arcana.controller;

import com.mmorrell.arcana.background.ArcanaBackgroundCache;
import com.mmorrell.arcana.strategies.BotManager;
import com.mmorrell.arcana.strategies.OpenBookBot;
import com.mmorrell.arcana.strategies.openbook.OpenBookSplUsdc;
import com.mmorrell.serum.manager.SerumManager;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.rpc.RpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class ArcanaController {

    private RpcClient rpcClient;
    private ArcanaBackgroundCache arcanaBackgroundCache;
    private final BotManager botManager;
    private final SerumManager serumManager;

    public ArcanaController(RpcClient rpcClient, ArcanaBackgroundCache arcanaBackgroundCache, BotManager botManager,
                            SerumManager serumManager) {
        this.rpcClient = rpcClient;
        this.arcanaBackgroundCache = arcanaBackgroundCache;
        this.botManager = botManager;
        this.serumManager = serumManager;
    }

    @RequestMapping("/")
    public String arcanaIndex(Model model) {
        model.addAttribute("rpcEndpoint", rpcClient.getEndpoint());
        model.addAttribute("botList", botManager.getBotList());
        return "index";
    }

    @RequestMapping("/settings")
    public String arcanaSettings(Model model, @RequestParam(required = false) String rpc) {
        if (rpc != null && rpc.length() > 10) {
            // set RPC host
            rpcClient = new RpcClient(rpc);
            log.info("New RPC Host: " + rpc);
        }

        model.addAttribute("rpcEndpoint", rpcClient.getEndpoint());

        return "settings";
    }

    // Adds and starts a new SPL/USDC trading strategy.
    @PostMapping("/bots/add/post")
    public String arcanaBotAdd(@ModelAttribute("newBot") OpenBookBot newBot) {
        // Add new strategy to list.
        // TODO if no private key loaded, bail out
        OpenBookSplUsdc openBookSplUsdc = new OpenBookSplUsdc(serumManager, rpcClient);
        openBookSplUsdc.setMarketId(newBot.getMarketId());
        openBookSplUsdc.setMmAccount(new Account());


        newBot.setStrategy(openBookSplUsdc);

        // TODO configure strategy using our private key and info
        botManager.addBot(newBot);
        log.info("New strategy created/started: " + newBot);

        return "redirect:/";
    }

    @RequestMapping("/openbook")
    public String openbookMarkets(Model model) {
        model.addAttribute("rpcEndpoint", rpcClient.getEndpoint());
        model.addAttribute("markets", arcanaBackgroundCache.getCachedMarkets());
        return "openbook";
    }

    @RequestMapping("/bots/add")
    public String arcanaBotWizard(Model model) {
        model.addAttribute("rpcEndpoint", rpcClient.getEndpoint());
        model.addAttribute("markets", arcanaBackgroundCache.getCachedMarkets());

        model.addAttribute("newBot", new OpenBookBot());

        return "add_bot";
    }

    @RequestMapping("/bots/view/{id}")
    public String arcanaBotWizard(Model model, @PathVariable("id") long botId) {
        model.addAttribute("rpcEndpoint", rpcClient.getEndpoint());
        model.addAttribute("markets", arcanaBackgroundCache.getCachedMarkets());

        model.addAttribute("botId", --botId);

        OpenBookBot bot = botManager.getBotList().get((int) botId);
        model.addAttribute("bot", bot.toString());
        model.addAttribute("botUuid", bot.getStrategy().uuid.toString());
        model.addAttribute("botMarketId", bot.getMarketId().toBase58());
        model.addAttribute("botBpsSpread", bot.getBpsSpread());
        model.addAttribute("botAmountBid", bot.getAmountBid());
        model.addAttribute("botAmountAsk", bot.getAmountAsk());
        model.addAttribute("botOoa", bot.getOoa().toBase58());
        model.addAttribute("botBaseWallet", bot.getBaseWallet().toBase58());
        model.addAttribute("botQuoteWallet", bot.getQuoteWallet().toBase58());

        // Strategy
        model.addAttribute("strategyName", bot.getStrategy().getClass().getSimpleName());

        return "view_bot";
    }

}
