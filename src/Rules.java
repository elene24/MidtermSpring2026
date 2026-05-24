
public final class Rules {

    private Rules() {}


    public static boolean isLegal(Card card, Card upCard, String calledColor) {
        if (card.isWild()) return true;

        String cc = card.color();
        String uc = upCard.color();

        if (!calledColor.isEmpty() && cc.equals(calledColor)) return true;
        if (cc.equals(uc)) return true;

        String cr = card.rank();
        String ur = upCard.rank();

        if (cr.equals(ur) && !cr.equals(Card.NUMBER)) return true;
        if (cr.equals(Card.NUMBER) && ur.equals(Card.NUMBER)
                && card.number() == upCard.number()) return true;

        return false;
    }

    public static boolean isLegal(String cardCode, String upCode, String calledColor) {
        return isLegal(new Card(cardCode), new Card(upCode), calledColor);
    }


    public static int scoreHand(Iterable<String> cardCodes) {
        int total = 0;
        for (String code : cardCodes) {
            total += new Card(code).points();
        }
        return total;
    }
}