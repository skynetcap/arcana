package com.mmorrell.arcana.strategies;

import lombok.Data;
import org.p2p.solanaj.core.PublicKey;

@Data
public class OpenBookBot {

    private Strategy strategy;
    private PublicKey marketId = new PublicKey("9Lyhks5bQQxb9EyyX55NtgKQzpM4WK7JCmeaWuQ5MoXD");
    private double bpsSpread = 20.0;
    private double amountBid = 0.1, amountAsk = 0.1;

    private PublicKey ooa = new PublicKey("7hM4pmTbyfAUoxU9p8KCqdFfdPTLXc5xFijXsbumaqAa");;
    private PublicKey baseWallet = new PublicKey("3UrEoG5UeE214PYQUA487oJRN89bg6fmt3ejkavmvZ81");
    private PublicKey quoteWallet = new PublicKey("A6Jcj1XV6QqDpdimmL7jm1gQtSP62j8BWbyqkdhe4eLe");;

    private String priceStrategy = "jupiter";

    // marketId, bps spread, amount to bid, amount to ask
    // ooa pubkey, base wallet pubkey, quote pubkey

}
