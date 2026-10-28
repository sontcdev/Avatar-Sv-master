package avatar.service;

import avatar.constants.Cmd;
import avatar.db.DbManager;
import avatar.item.Item;
import avatar.lib.RandomCollection;
import avatar.lucky.GiftBox;
import avatar.message.ParkMsgHandler;
import avatar.model.Fish;
import avatar.model.User;
import avatar.network.Message;
import avatar.network.Session;
import avatar.server.UserManager;
import avatar.server.Utils;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class ParkService extends Service {
    private final ConcurrentMap<Integer, Long> friendInvitations = new ConcurrentHashMap<>();
    private List<Integer> displayedFriends = List.of();
    private int friendListId;
    private final RandomCollection<Integer> randomItemList1 = new RandomCollection<>();
    short time;
    private static final ScheduledThreadPoolExecutor fishingTimer = new ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "avatar-fishing");
        thread.setDaemon(true);
        return thread;
    });
    static { fishingTimer.setRemoveOnCancelPolicy(true); }
    private volatile ScheduledFuture<?> pendingBite;

    public void cancelFishing() {
        ScheduledFuture<?> pending = pendingBite;
        if (pending != null) pending.cancel(false);
        pendingBite = null;
    }

    private static final Logger logger = Logger.getLogger(AvatarService.class);
    private static final Fish a = new Fish();
    public ParkService(Session cl) {
        super(cl);
        randomItemList1.add(80, 2383);//nro
        randomItemList1.add(20, 2384);
    }
///le duong
    public void WEDDING_BIGINHanlder(User user, Message ms) {
        try {
            Message ms1 = new Message(Cmd.WEDDING_BIGIN);
            DataOutputStream ds = ms1.writer();
            ds.writeInt(7);
            ds.writeInt(1);//id girl
            ds.flush();
            user.getZone().getPlayers().forEach(u -> {
                u.session.sendMessage(ms1);
            });
            accceptMarry(user.getId());
            user.setLevelMarry(1
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void accceptMarry(int IDuserNam){

        String updateQuery = "UPDATE marry SET level = ?, perLevel = ? WHERE idNam = ?";
        try (Connection conn = DbManager.getInstance().getConnection();
             PreparedStatement psUpdate = conn.prepareStatement(updateQuery)) {
            psUpdate.setInt(1, 1);
            psUpdate.setInt(2, 0);
            psUpdate.setInt(3, IDuserNam);
            psUpdate.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void handleAddFriendRequest(Message ms) {
        try {
            int userId = ms.reader().readInt(); // id người nhận
            User user = UserManager.getInstance().find(userId);
            if (user == null || userId == session.user.getId()) {
                serverDialog("Người nhận lời mời không hợp lệ hoặc hiện không online.");
                return;
            }
            ConcurrentMap<Integer, Long> invitations = user.session.getParkService().friendInvitations;
            long now = System.currentTimeMillis();
            invitations.entrySet().removeIf(entry -> now - entry.getValue() > 300000);
            if (invitations.size() >= 32) { serverDialog("Người nhận đang có quá nhiều lời mời."); return; }
            invitations.put(session.user.getId(),now);
            ms = new Message(Cmd.AVATAR_REQUEST_ADD_FRIEND);
            DataOutputStream ds = ms.writer();
            ds.writeInt(session.user.getId());
            ds.writeUTF(session.user.getUsername());
            ds.writeUTF(session.user.getUsername() + " muốn kết bạn với bạn.");
            ds.flush();
            user.session.sendMessage(ms);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void answerFriendInvitation(Message request) throws IOException, SQLException {
        if (request.reader().available() != 5) { serverDialog("Yêu cầu kết bạn không hợp lệ."); return; }
        int inviterId = request.reader().readInt(); boolean accept = request.reader().readBoolean();
        Long invitedAt = friendInvitations.remove(inviterId);
        User inviter = UserManager.getInstance().find(inviterId);
        if (invitedAt == null || System.currentTimeMillis() - invitedAt > 300000 || inviter == null || inviterId == session.user.getId()) {
            serverDialog("Lời mời không tồn tại hoặc đã hết hạn."); return;
        }
        if (accept) {
            try (Connection c = DbManager.getInstance().getConnection(); PreparedStatement p = c.prepareStatement(
                    "INSERT IGNORE INTO gameplay_friendships(user_low,user_high) VALUES (?,?)")) {
                p.setInt(1,Math.min(inviterId,session.user.getId())); p.setInt(2,Math.max(inviterId,session.user.getId()));
                p.executeUpdate();
            }
        }
        friendAnswer(inviter,session.user,accept);
        friendAnswer(session.user,inviter,accept);
    }

    private void friendAnswer(User recipient, User other, boolean accepted) throws IOException {
        Message response = new Message(Cmd.ADD_FRIEND);
        response.writer().writeInt(other.getId()); response.writer().writeUTF(other.getUsername());
        response.writer().writeBoolean(accepted);
        response.writer().writeUTF(accepted ? "Đã kết bạn với " + other.getUsername() : "Lời mời kết bạn đã bị từ chối.");
        recipient.session.sendMessage(response);
    }

    public synchronized void sendFriendList() throws IOException, SQLException {
        // shortcut: display the first 100 friends; add pagination before supporting larger lists.
        List<Integer> ids = new ArrayList<>(); List<String> names = new ArrayList<>();
        try (Connection c = DbManager.getInstance().getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT u.id,u.username FROM gameplay_friendships f JOIN users u ON u.id=" +
                "CASE WHEN f.user_low=? THEN f.user_high ELSE f.user_low END " +
                "WHERE f.user_low=? OR f.user_high=? ORDER BY u.id LIMIT 100")) {
            for (int i=1;i<=3;i++) p.setInt(i,session.user.getId());
            try (ResultSet r = p.executeQuery()) { while(r.next()) { ids.add(r.getInt(1)); names.add(r.getString(2)); } }
        }
        displayedFriends = List.copyOf(ids); friendListId = Math.incrementExact(friendListId);
        Message response = new Message(Cmd.CUSTOM_LIST); DataOutputStream out = response.writer();
        out.writeUTF("friendlist"); out.writeUTF("Bạn bè"); out.writeInt(friendListId);
        out.writeByte(1); out.writeByte(1); out.writeShort(ids.size());
        for (int i=0;i<ids.size();i++) {
            out.writeByte(0); out.writeInt(ids.get(i)); out.writeShort(-1); out.writeShort(-1); out.writeShort(-1);
            out.writeUTF(names.get(i)); out.writeUTF(UserManager.getInstance().find(ids.get(i)) == null ? "Offline" : "Online");
        }
        out.writeByte(2); out.writeByte(1); out.writeUTF("Xóa bạn"); out.writeByte(2); out.writeUTF("Thăm nông trại");
        sendMessage(response);
    }

    public synchronized void friendListAction(Message request) throws IOException, SQLException {
        if (request.reader().available() != 8) { serverDialog("Lựa chọn bạn bè không hợp lệ."); return; }
        int listId = request.reader().readInt(); int page = request.reader().readUnsignedByte();
        int index = request.reader().readShort(); int action = request.reader().readUnsignedByte();
        if (listId != friendListId || page != 1 || index < 0 || index >= displayedFriends.size() || (action != 1 && action != 2)) {
            serverDialog("Danh sách bạn bè đã thay đổi hoặc lựa chọn không hợp lệ."); return;
        }
        int other = displayedFriends.get(index);
        if (action == 1) {
            try (Connection c = DbManager.getInstance().getConnection(); PreparedStatement p = c.prepareStatement(
                    "DELETE FROM gameplay_friendships WHERE user_low=? AND user_high=?")) {
                p.setInt(1,Math.min(other,session.user.getId())); p.setInt(2,Math.max(other,session.user.getId())); p.executeUpdate();
            }
            sendFriendList();
        } else {
            Message context = new Message(Cmd.GET_HANDLER); context.writer().writeByte(10);
            session.getHandler(new Message(Cmd.GET_HANDLER,context.getData()));
            session.getFarmService().prepareFriendVisit(other);
        }
    }

    public void handleStartFishing(Message ms) {
        try {
            if(!CheckItemAreaFish(460,"bạn phải vé câu cá mập")){
                return;
            }
            if(!CheckItemAreaFish(446,"bạn phải có cần câu vip")){
                return;
            }

            if(this.session.user.AutoFish){
                ByteArrayOutputStream sendFish = new ByteArrayOutputStream();
                try (DataOutputStream dos2 = new DataOutputStream(sendFish)) {

                    dos2.flush();
                    byte[] dataQuangCau = sendFish.toByteArray();
                    ParkMsgHandler parkMsgHandler1 = new ParkMsgHandler(this.session);
                    parkMsgHandler1.onMessage(new Message(Cmd.QUANG_CAU, dataQuangCau));
                }
            }



//            if(!CheckItemAreaFish(448,"bạn phải có mồi câu cá")){
//                return;
//            }
//            Item MoiCau = this.session.user.findItemInChests(448);
//            if(MoiCau!=null){
//                this.session.user.removeItemFromChests(MoiCau);
//            }


        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }

    private boolean CheckItemAreaFish(int ItemID,String messenger) throws IOException {
        Message response = new Message(Cmd.START_CAU_CA);
        DataOutputStream ds = response.writer();
        boolean isSuccess = true;
        Item item= null;
        if(ItemID == 446){
            item = this.session.user.findItemInWearing(ItemID);
        }else {
            item = this.session.user.findItemInChests(ItemID);
        }
        if(item==null){
            isSuccess = false;
            ds.writeBoolean(isSuccess);
            ds.writeUTF(messenger);
            ds.flush();
            this.sendMessage(response);
            return false;
        }
        ds.writeBoolean(isSuccess);
        ds.writeUTF("");
        ds.flush();
        this.sendMessage(response);
        return true;
    }

    public synchronized void handleQuangCau(Message ms) {
        try {

            if (pendingBite != null && !pendingBite.isDone()) {
                serverDialog("Bạn đang chờ cá cắn câu.");
                return;
            }
            Item item = this.session.user.findItemInChests(448);
            if(item==null){
                if(this.session.user.AutoFish)
                {
                    if (this.session.user.getXu() < 30) {
                        serverDialog("Bạn không đủ 30 xu để mua mồi câu.");
                        return;
                    }
                    this.session.user.updateXu(-30);
                    this.session.user.getAvatarService().updateMoney(0);
                }else {
                    this.session.user.getAvatarService().serverDialog("Hết mồi rồi sếp");
                    return;
                }
            }
            if (item != null) this.session.user.removeItemFromChests(item);
            int userID = this.session.user.getId();
            Message response = new Message(Cmd.QUANG_CAU);
            DataOutputStream ds = response.writer();
            ds.writeInt(userID);
            ds.flush();
            this.sendMessage(response);
            User fishingUser = session.user;
            avatar.play.Zone fishingZone = fishingUser.getZone();
            pendingBite = fishingTimer.schedule(() -> {
                synchronized (ParkService.this) {
                    pendingBite = null;
                    if (session.isConnected() && session.user == fishingUser && fishingUser.getZone() == fishingZone) {
                        try { onCanCau(); }
                        catch (RuntimeException failure) {
                            logger.error("Fishing bite failed", failure);
                            serverDialog("Không thực hiện được câu cá. Vui lòng thử lại sau.");
                        }
                    }
                }
            }, Utils.nextInt(12000, 18000), TimeUnit.MILLISECONDS);

        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }

    public void onStatusFish() {
        try {
            int userID = this.session.user.getId();
            Message ms = new Message(Cmd.STATUS_FISH);
            DataOutputStream ds = ms.writer();
            ds.writeInt(userID);
            ds.writeByte(1);//ca can cau
            ds.flush();
            this.sendMessage(ms);

        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }

    public void onCanCau() {
        try {

            //us = UserManager.getInstance().find(this.session.user.getId());
            short idFish = (short) a.getRandomFishID();
            this.session.user.setIdFish(idFish);
            time = 3000;
            if(this.session.user.AutoFish){
                time = 20;
            }
            if(idFish<0)
            {
                time = -1;
                this.session.user.setIdFish(idFish);
            }
            Random random = new Random();
            Message ms = new Message(Cmd.CAN_CAU);
            DataOutputStream ds = ms.writer();
            ds.writeInt(this.session.user.getId());
            ds.writeShort(this.session.user.getIdFish());
            ds.writeShort(time);
            int randomNumber = random.nextInt((12 - 6) + 1) + 4;
            ds.writeByte((byte) randomNumber);
            for (int i = 0; i < randomNumber; i++) {
                int randomIndex = random.nextInt(a.images.length);
                byte[] randomImage = a.images[randomIndex];
                ds.writeShort(randomImage.length);
                ds.write(randomImage);
            }
            ds.flush();
            this.sendMessage(ms);

        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }

    public void CauThanhCong() {
        try {

            int userID = this.session.user.getId();
            Message ms = new Message(Cmd.CAU_THANH_CONG);
            DataOutputStream ds = ms.writer();
            ds.writeInt(userID);
            ds.writeShort(this.session.user.getIdFish());
            int IDFISH = this.session.user.getIdFish();
            if(IDFISH>0){
                Item item = new Item(IDFISH,-1,1);
                this.session.user.addItemToChests(item);
                this.session.user.getAvatarService().sellFish(this.session.user,item.getId());
                if(IDFISH == 457) {
                    Item keoAcMa = new Item(6822,-1,1);
                    if(this.session.user.findItemInChests(6822) !=null){
                        int quantity = this.session.user.findItemInChests(6822).getQuantity();
                        this.session.user.findItemInChests(6822).setQuantity(quantity+1);
                    }else {
                        this.session.user.addItemToChests(keoAcMa);
                    }
                    UserManager.users.forEach(user -> {
                        user.getAvatarService().serverInfo("Chúc mừng bạn : " + this.session.user.getUsername()+" đã câu được 1 Cá Mập");
                    });
                    Utils.writeLogCaMap(this.session.user,"bú 1 cá mập");
                }
                addVatPhamSuKienFish(this.session.user);
            }
            ds.flush();
            this.sendMessage(ms);

            if(this.session.user.AutoFish)
            {
                CauCaXong();
                ByteArrayOutputStream sendFish = new ByteArrayOutputStream();
                try (DataOutputStream dos2 = new DataOutputStream(sendFish)) {

                    dos2.flush();
                    byte[] dataQuangCau = sendFish.toByteArray();
                    ParkMsgHandler parkMsgHandler1 = new ParkMsgHandler(this.session);
                    parkMsgHandler1.onMessage(new Message(Cmd.QUANG_CAU, dataQuangCau));
                }
            }
        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }

    private void addVatPhamSuKienFish(User us) throws IOException {

        if(this.session.user.getCrazy() >=1000){
            return;
        }
        int ok =  (Utils.nextInt(100) < 70) ? 1 : 0;
        if(ok==1){

            RandomCollection<Integer> chosenItemCollection = chooseItemCollection();
            int idItems = chosenItemCollection.next();
            Item Nro = new Item(idItems,-1,1);
            if(us.findItemInChests(idItems) !=null){
                int quantity = us.findItemInChests(idItems).getQuantity();
                us.findItemInChests(idItems).setQuantity(quantity+1);
            }else {
                us.addItemToChests(Nro);
                us.updateCrazy(+1);
            }
            us.getAvatarService().SendTabmsg("Bạn vừa nhận được 1 "+ " " + Nro.getPart().getName());

        }
    }

    private RandomCollection<Integer> chooseItemCollection() {
        RandomCollection<RandomCollection<Integer>> itemCollections = new RandomCollection<>();
        itemCollections.add(100, randomItemList1);
        return itemCollections.next();
    }



    public void onInfoFish() {
        try {
            Message ms = new Message(Cmd.INFO_FISH);
            DataOutputStream ds = ms.writer();
            ds.writeInt(this.session.user.getId());
            ds.writeByte(1);
            ds.writeByte(1);
            ds.writeInt(1);
            ds.writeShort(457);
            ds.flush();
            this.sendMessage(ms);

        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }


    public void CauCaXong() {
        cancelFishing();
        try {
            int userID = this.session.user.getId();
            Message ms = new Message(Cmd.CAU_CA_XONG);
            int IDFISH = this.session.user.getIdFish();
            DataOutputStream ds = ms.writer();
            ds.writeInt(userID);
            ds.writeInt(IDFISH);
            ds.flush();
            this.sendMessage(ms);
        } catch (IOException ex) {
            logger.error("handleStartFishing() ", ex);
        }
    }

}
