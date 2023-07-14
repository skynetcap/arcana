package com.mmorrell.arcana.controller;

import com.mmorrell.arcana.background.ArcanaBackgroundCache;
import com.mmorrell.arcana.strategies.BotManager;
import com.mmorrell.arcana.strategies.OpenBookBot;
import com.mmorrell.arcana.strategies.openbook.OpenBookSplUsdc;
import com.mmorrell.serum.manager.SerumManager;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

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
        model.addAttribute("tradingAccountPubkey", botManager.getTradingAccount().getPublicKey().toBase58());

        return "settings";
    }

    // Adds and starts a new SPL/USDC trading strategy.
    @PostMapping("/bots/add/post")
    public String arcanaBotAdd(@ModelAttribute("newBot") OpenBookBot newBot) {
        // Adds new strategy to list.
        OpenBookSplUsdc openBookSplUsdc = new OpenBookSplUsdc(serumManager, rpcClient);
        openBookSplUsdc.setMarketId(newBot.getMarketId());
        openBookSplUsdc.setMarketOoa(newBot.getOoa());
        openBookSplUsdc.setBaseWallet(newBot.getBaseWallet());
        openBookSplUsdc.setUsdcWallet(newBot.getQuoteWallet());
        openBookSplUsdc.setMmAccount(botManager.getTradingAccount());
        openBookSplUsdc.setBaseAskAmount((float) newBot.getAmountAsk());
        openBookSplUsdc.setUsdcBidAmount((float) newBot.getAmountBid());

        // bid + ask multiplier
        float bidMultiplier = (10000.0f - (float) newBot.getBpsSpread()) / 10000.0f;
        float askMultiplier = (10000.0f + (float) newBot.getBpsSpread()) / 10000.0f;

        openBookSplUsdc.setBidSpreadMultiplier(bidMultiplier);
        openBookSplUsdc.setAskSpreadMultiplier(askMultiplier);

        newBot.setStrategy(openBookSplUsdc);

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

        // Last bids / asks
        if (bot.getStrategy() instanceof OpenBookSplUsdc) {
            model.addAttribute("lastBidOrder", ((OpenBookSplUsdc) bot.getStrategy()).getLastBidOrder().toString());
            model.addAttribute("lastAskOrder", ((OpenBookSplUsdc) bot.getStrategy()).getLastAskOrder().toString());
        }

        return "view_bot";
    }

    @PostMapping("/privateKeyUpload")
    public String privateKeyUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            byte[] bytes = file.getBytes();
            botManager.setTradingAccount(Account.fromJson(new String(bytes)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/settings";
    }

}
