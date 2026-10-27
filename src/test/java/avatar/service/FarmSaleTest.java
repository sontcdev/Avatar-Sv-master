package avatar.service;

import avatar.constants.Cmd;
import avatar.message.FarmMsgHandler;
import avatar.network.Message;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmSaleTest {
    @Test public void disabledSalesDoNotDeductInventoryOrCreditMoney() throws Exception {
        String previous = System.getProperty("avatar.farm.sales.catalog");
        java.nio.file.Path catalog = java.nio.file.Files.createTempFile("disabled-farm-sales", ".properties");
        System.clearProperty("avatar.farm.sales.catalog");
        try {
        GameplayFeedbackTest.withSession(session -> {
            session.user.setXu(100);
            session.user.NongSanDacBiet.add(new avatar.Farm.NongSanDacBiet(215, 20));
            Message request = new Message(Cmd.SELL_ITEM);
            request.writer().writeShort(215);
            new FarmMsgHandler(session).onMessage(new Message(Cmd.SELL_ITEM, request.getData()));
            assertEquals(Cmd.SET_MONEY_ERROR, session.messages.get(0).getCommand());
            assertEquals(100, session.user.getXu());
            assertEquals(20, session.user.NongSanDacBiet.get(0).getSoluong());
            System.setProperty("avatar.farm.sales.catalog",catalog.toString());
            for (String config : new String[]{"enabled=false\napproval=TEST_ONLY\nprice.215=7\n",
                    "enabled=true\nprice.215=7\n", "enabled=true\napproval=TEST_ONLY\nprice.215=bad\n"}) {
                java.nio.file.Files.writeString(catalog,config); session.messages.clear();
                new FarmMsgHandler(session).onMessage(new Message(Cmd.SELL_ITEM,request.getData()));
                assertEquals(Cmd.SET_MONEY_ERROR,session.messages.get(0).getCommand());
                assertEquals(100,session.user.getXu());
                assertEquals(20,session.user.NongSanDacBiet.get(0).getSoluong());
            }
        });
        } finally {
            java.nio.file.Files.deleteIfExists(catalog);
            if (previous == null) System.clearProperty("avatar.farm.sales.catalog");
            else System.setProperty("avatar.farm.sales.catalog",previous);
        }
    }
}
