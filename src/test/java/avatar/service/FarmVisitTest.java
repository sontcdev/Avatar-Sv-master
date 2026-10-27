package avatar.service;

import avatar.Farm.LandItem;
import avatar.constants.Cmd;
import avatar.network.Message;
import avatar.server.UserManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmVisitTest {
    @Test public void visitShowsOwnersFarmAndCannotHarvestVisitorsFruit() throws Exception {
        GameplayFeedbackTest.withSession(visitor -> GameplayFeedbackTest.withSession(owner -> {
            visitor.user.setId(71); owner.user.setId(92);
            visitor.user.landItems.add(new LandItem(0, -1, 0, 0, false, false, false, null));
            owner.user.setStarfruitQuantity(12);
            UserManager.getInstance().add(owner.user);
            try {
                FarmService service = new FarmService(visitor);
                Message request = new Message(Cmd.JOIN); request.writer().writeInt(92);
                service.joinFarm(new Message(Cmd.JOIN, request.getData()));
                Message response = new Message(Cmd.JOIN, visitor.messages.get(0).getData());
                assertEquals(92, response.reader().readInt());
                assertEquals(0, response.reader().readUnsignedByte());
                assertEquals(0, response.reader().readUnsignedByte());
                response.reader().skipBytes(8);
                assertEquals(12, response.reader().readShort());
                service.harvestStarfruit();
                assertEquals(180, visitor.user.getStarfruitQuantity());
                assertTrue(visitor.user.NongSanDacBiet.isEmpty());
                assertEquals(Cmd.SET_MONEY_ERROR, visitor.messages.get(1).getCommand());
            } finally { UserManager.getInstance().remove(owner.user); }
        }));
    }
}
