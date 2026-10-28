package avatar.service;

import avatar.Farm.*;
import avatar.db.DbManager;
import avatar.item.PartManager;
import avatar.lib.KeyValue;
import avatar.model.GameData;
import avatar.model.ImageInfo;
import avatar.constants.Cmd;
import avatar.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Vector;
import avatar.server.Avatar;
import java.io.IOException;
import java.io.DataOutputStream;
import avatar.network.Message;
import avatar.network.Session;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.*;

public class FarmService extends Service {

    private int viewedFarmId = -1;
    private int pendingFriendFarmId = -1;
    private FarmCookingCatalog cookingCatalog;

    public void resetVisit() {
        viewedFarmId = -1;
        pendingFriendFarmId = -1;
    }

    public void prepareFriendVisit(int ownerId) {
        pendingFriendFarmId = ownerId;
    }

    private static final Logger logger = Logger.getLogger(Service.class);

    public FarmService(Session cl) {
        super(cl);
    }


    public void cook(Message request) throws IOException, SQLException {
        if (viewedFarmId != session.user.getId()) {
            serverDialog("Bạn chỉ có thể nấu ở nông trại của mình."); return;
        }
        if (request.reader().available() != 2) throw new IllegalArgumentException("Yêu cầu nấu ăn không hợp lệ.");
        int recipeId = request.reader().readShort();
        if (recipeId == -1) {
            serverDialog("Chưa hỗ trợ hủy món đang nấu."); return;
        }
        if (cookingCatalog == null) cookingCatalog = FarmCookingCatalog.parse(Avatar.getFile("res/data/farm_info.dat"));
        synchronized (session.user) {
            session.user.startCooking(cookingCatalog.recipe(recipeId), System.currentTimeMillis());
            Message result = new Message(Cmd.COOKING);
            result.writer().writeShort(session.user.getCookingRecipe());
            result.writer().writeShort(session.user.cookingRemainingMinutes(System.currentTimeMillis()));
            sendMessage(result);
            getInventory(null);
        }
    }

    public void collectCooking(Message request) throws IOException, SQLException {
        if (viewedFarmId != session.user.getId()) {
            serverDialog("Bạn chỉ có thể thu hoạch món ăn ở nông trại của mình."); return;
        }
        if (request.reader().available() != 0) throw new IllegalArgumentException("Yêu cầu thu hoạch không hợp lệ.");
        synchronized (session.user) {
            if (cookingCatalog == null) cookingCatalog = FarmCookingCatalog.parse(Avatar.getFile("res/data/farm_info.dat"));
            FarmCookingCatalog.Recipe recipe = cookingCatalog.recipe(session.user.getCookingRecipe());
            if (recipe == null || recipe.outputId() != session.user.getCookingOutputId())
                throw new IllegalArgumentException("Công thức đã thay đổi; vui lòng liên hệ quản trị viên.");
            session.user.collectCooking(System.currentTimeMillis());
            // The native decoder credits one output before the authoritative inventory refresh.
            sendMessage(new Message(Cmd.HARVEST_COOK));
            getInventory(null);
        }
    }

    public void starfruitInfo(Message request) throws IOException {
        if (viewedFarmId != session.user.getId()) {
            serverDialog("Bạn chỉ có thể xem thông tin cây khế của mình."); return;
        }
        if (request.reader().available() != 0) throw new IllegalArgumentException("Yêu cầu thông tin khế không hợp lệ.");
        serverDialog("Cây khế: " + session.user.getStarfruitQuantity() + "/180 quả. Khế hồi đầy sau 60 phút kể từ lần thu hoạch.");
    }

    public void harvestStarfruit() throws IOException {
        if (viewedFarmId != session.user.getId()) {
            serverDialog("Bạn chỉ có thể thu hoạch khế ở nông trại của mình.");
            return;
        }
        final int harvested;
        try {
            harvested = session.user.harvestStarfruit();
        } catch (SQLException failure) {
            logger.error("Could not save starfruit harvest", failure);
            serverDialog("Chưa lưu được thu hoạch khế. Vui lòng thử lại sau.");
            return;
        }
        if (harvested > 0) {
            Message result = new Message(Cmd.HARVEST_STARFRUIT);
            result.writer().writeShort(215);
            result.writer().writeShort(harvested);
            sendMessage(result);
        }
        Message refresh = new Message(Cmd.JOIN);
        refresh.writer().writeInt(session.user.getId());
        try {
            joinFarm(new Message(Cmd.JOIN, refresh.getData()));
        } catch (SQLException failure) {
            throw new IOException(failure);
        }
        getInventory(null);
        serverDialog(harvested == 0 ? "Cây hiện không còn khế để thu hoạch."
                : "Đã thu hoạch " + harvested + " quả khế vào kho.");
    }

    public void sellFarmitm(User user,Message ms) throws IOException {
        if (user != session.user || ms.reader().available() != 2) {
            serverDialog("Yêu cầu bán nông sản không hợp lệ."); return;
        }
        int product = ms.reader().readShort();
        java.util.Properties catalog = new java.util.Properties();
        String path = System.getProperty("avatar.farm.sales.catalog");
        if (path == null) { serverDialog("Bán nông sản chưa được bật."); return; }
        try (java.io.Reader file = java.nio.file.Files.newBufferedReader(java.nio.file.Path.of(path))) {
            catalog.load(file);
            if (!"true".equals(catalog.getProperty("enabled")) || catalog.getProperty("approval", "").isBlank()) {
                serverDialog("Bán nông sản chưa được bật hoặc chưa có giá được duyệt."); return;
            }
            int price = Integer.parseInt(catalog.getProperty("price." + product, "0"));
            if (price <= 0) { serverDialog("Sản phẩm chưa có giá bán được duyệt."); return; }
            int[] sale = user.sellFarmProduct(product,price);
            Message response = new Message(Cmd.SELL_ITEM);
            response.writer().writeInt(sale[0]); response.writer().writeInt(sale[1]); response.writer().writeShort(product);
            sendMessage(response);
        } catch (SQLException failure) {
            logger.error("Farm sale database failure, user=" + user.getId(),failure);
            if (user.isFarmSaleUncertain()) session.close();
            else serverDialog("Chưa lưu được giao dịch bán. Vui lòng thử lại sau.");
        } catch (IllegalArgumentException | ArithmeticException failure) {
            if (user.isFarmSaleUncertain()) session.close();
            else serverDialog("Không thể bán: kho, giá hoặc số dư không hợp lệ.");
        } catch (IOException failure) {
            logger.error("Farm sale catalog or response unavailable",failure);
            serverDialog("Chưa đọc được cấu hình bán nông sản hoặc gửi kết quả.");
        }
    }


    //trồng cây
// Phương thức trồng cây
    public void plandSeed(Message ms) throws IOException, SQLException {
        if (ms.reader().available() != 6) throw new IllegalArgumentException("Yêu cầu trồng cây không hợp lệ.");
        int idUser = ms.reader().readInt();
        int indexCell = ms.reader().readByte();
        int idSeed = ms.reader().readByte();

        if (idUser != session.user.getId() || viewedFarmId != idUser) {
            serverDialog("Bạn chỉ có thể trồng cây ở nông trại của mình.");
            return;
        }
        session.user.plantCrop(indexCell, idSeed);

        // Tạo Message và gửi thông tin trồng cây
        ms = new Message(Cmd.PLANT_SEED);
        DataOutputStream ds = ms.writer();
        ds.writeInt(idUser);
        ds.writeByte(indexCell);
        ds.writeByte(idSeed);
        ds.flush();
        this.session.sendMessage(ms);
    }


    //thu hoạch
    public void treeHarvest(Message ms) throws IOException {
        if (ms.reader().available() != 5) throw new IllegalArgumentException("Yêu cầu thu hoạch không hợp lệ.");
        int farmId = ms.reader().readInt();
        int index = ms.reader().readUnsignedByte();
        if (farmId != session.user.getId() || viewedFarmId != farmId) {
            serverDialog("Bạn chỉ có thể thu hoạch ở nông trại của mình.");
            return;
        }
        final int quantity;
        try { quantity = session.user.harvestCrop(index); }
        catch (IllegalArgumentException invalid) { serverDialog(invalid.getMessage()); return; }
        catch (SQLException failure) {
            logger.error("Could not save crop harvest", failure);
            serverDialog("Chưa lưu được thu hoạch. Vui lòng thử lại sau.");
            return;
        }
        Message response = new Message(Cmd.TREE_HARVEST);
        response.writer().writeByte(index);
        response.writer().writeShort(quantity);
        session.sendMessage(response);
        getInventory(null);
    }
   // mở ô đất
    public void doRequestslot(Message ms) throws IOException {
        int id = ms.reader().readInt();//id user
        ms = new Message(Cmd.REQUEST_SLOT);
        DataOutputStream ds = ms.writer();
        ds.writeUTF("Bạn có muốn mở ô đất @ với giá @ xu hoặc @ lượng không ?");
        ds.flush();
        this.session.sendMessage(ms);
    }

//mở ô đất
    public void openLand(Message ms) throws IOException {
    int id = ms.reader().readInt(); // ID của nông trại
    byte typeBuy = ms.reader().readByte(); // Loại giao dịch hoặc mã người dùng

    this.session.user.landItems.add(new LandItem(0, -1,0, 0, false, false, false, LocalDateTime.now())); // Ô đất mặc định

    ms = new Message(Cmd.OPEN_LAND);
    DataOutputStream ds = ms.writer();
    ds.writeInt(id);
    ds.writeInt(1);
    ds.writeByte(typeBuy);     // Thông tin giao dịch hoặc mã người dùng
    ds.writeUTF("Đã mở ô đất thành công"); // Thông báo mở đất thành công

    // Ghi thêm thông tin mô tả cho ô đất vừa được mở
    ds.writeInt((int)this.session.user.getXu());
    ds.writeInt(this.session.user.getLuong());
    ds.writeInt(0);
    ds.flush();

    this.session.sendMessage(ms);
    }


    public void setBigFarm(Message ms) throws IOException {


        ms = new Message(51);
        DataOutputStream ds = ms.writer();
        int[] images = {99, 206};
        ds.writeByte(images.length);
        for (int i = 0; i < images.length; ++i) {
            ds.writeShort(i);
            ds.writeShort(images[i]);
        }
        ds.writeInt(15378);
        ds.writeInt(62724);
        ds.flush();
        this.session.sendMessage(ms);


        //apk goc
//        ms = new Message(51);
//        DataOutputStream ds = ms.writer();
//        short b19 = 2;
//        ds.writeByte(b19);
//        short[] array2 = {0, 1};
//        for (short value : array2) {
//            ds.writeShort(value);
//        }
//        short[] array3 = {66, 6};
//        for (short value : array3) {
//            ds.writeShort(value);
//        }
//        ds.writeInt(15378);
//        ds.writeInt(59669);
//        ds.flush();
//        this.session.sendMessage(ms);
    }

    public void Buy_item_farm(Message ms) throws IOException {
        short id = ms.reader().readShort();
        byte n = ms.reader().readByte();
        byte type = ms.reader().readByte();
        System.out.println("item farm id "+id+" sl "+n+" type sell"+type);

        farmItem itemf = PartManager.getInstance().findFarmitemByID(id);
        if (n <= 0 || n > 99 || (type != 0 && type != 1) || itemf == null || itemf.getSell() <= 0) {
            serverDialog("Vật phẩm nông trại chưa có dữ liệu bán hoặc số lượng không hợp lệ.");
            return;
        }

        long price = (long) itemf.getSell() * n;
        if (price > Integer.MAX_VALUE) {
            serverDialog("Giá vật phẩm vượt giới hạn giao dịch.");
            return;
        }
        if(type == 1){
            if(this.session.user.getXu()<price){
                this.session.user.getAvatarService().serverDialog("bạn không đủ xu");
                return;
            }
        } else{
            if(this.session.user.getLuong()<price){
                this.session.user.getAvatarService().serverDialog("bạn không đủ lượng");
                return;
            }
        }

        HatGiong hdid = this.session.user.findhatgiong(id);
        if(hdid != null){
            if((hdid.getSoluong()+n) >99){
                this.session.user.getAvatarService().serverDialog("số lượng nhiều rồi");
                return;
            }
        }

        ms = new Message(62);
        DataOutputStream ds = ms.writer();
        ds.writeShort(id);
        ds.writeByte(n);
        ds.writeInt((int) this.session.user.getXu());//.newMoney
        if (type == 1) this.session.user.updateXu(-price);
        else this.session.user.updateLuong(-(int) price);
        ds.writeByte(type);
        ds.writeInt((int) this.session.user.getXu());
        ds.writeInt(this.session.user.getLuong());//luong
        ds.writeInt(0);//luongK
        ds.flush();
        this.session.sendMessage(ms);

        if (hdid == null) this.session.user.hatgiong.add(new HatGiong(id,n));
        else hdid.setSoluong(hdid.getSoluong() + n);
    }

    public void Buy_ANIMAL(Message ms) throws IOException {
        byte n = ms.reader().readByte();//loai
        byte type = ms.reader().readByte();//typemua

        ms = new Message(Cmd.BUY_ANIMAL);
        DataOutputStream ds = ms.writer();
        ds.writeByte(n);
        ds.writeInt(0);//newMoney2
        ds.writeByte(type);
        ds.writeInt((int) 0);
        ds.writeInt(0);//luong
        ds.writeInt(0);//luongK
        ds.flush();
        this.session.sendMessage(ms);

        Animal newAnimal = new Animal(n,2000, 100, 0, 20, true, false, true); // Các giá trị mặc định
        this.session.user.Animal.add(newAnimal);
    }

    public void getBigFarm(Message ms) throws IOException {
        short imageID = ms.reader().readShort();
        String folder = this.session.getResourcesPath() + "bigFarm/";
        byte[] dat = Avatar.getFile(folder + imageID + ".png");
        if (dat == null) {
            return;
        }
        ms = new Message(54);
        DataOutputStream ds = ms.writer();
        ds.writeShort(imageID);
        ds.writeShort(dat.length);
        ds.writeShort(dat.length);
        for (int i = 0; i < dat.length; ++i) {
            ds.writeByte(dat[i]);
        }
        ds.flush();
        this.session.sendMessage(ms);
    }

    public void getImageData() {
        try {
            List<ImageInfo> imageInfos = GameData.getInstance().getFarmImageDatas();
            Message ms = new Message(Cmd.GET_IMAGE_FARM);
            DataOutputStream ds = ms.writer();
            ds.writeShort(imageInfos.size());
            for (ImageInfo imageInfo : imageInfos) {
                ds.writeShort(imageInfo.getId());
                ds.writeShort(imageInfo.getBigImageID());
                ds.writeByte(imageInfo.getX());
                ds.writeByte(imageInfo.getY());
                ds.writeByte(imageInfo.getW());
                ds.writeByte(imageInfo.getH());
            }
            ds.flush();
            this.sendMessage(ms);

        } catch (IOException e) {
            logger.debug("getImageData: " + e.getMessage());
        }
    }

    public void getTreeInfo(Message ms) throws IOException {
        byte[] dat = Avatar.getFile("res/data/farm_info.dat");
        if (dat == null) {
            return;
        }
        ms = new Message(Cmd.GET_TREE_INFO);
        DataOutputStream ds = ms.writer();
        ds.write(dat);
        ds.flush();
        this.session.sendMessage(ms);
    }

    public void getInventory(Message ms) throws IOException {
        User us = session.user;
        Vector<KeyValue<Integer, Integer>> nongsandacbiet = new Vector<>();
        for (NongSanDacBiet item : us.NongSanDacBiet) {
            nongsandacbiet.add(new KeyValue(item.getId(), item.getSoluong()));
        }
        ms = new Message(60);
        DataOutputStream ds = ms.writer();
        ds.writeByte(this.session.user.hatgiong.size());
        for (HatGiong hatgiong : this.session.user.hatgiong) {
            ds.writeByte(hatgiong.getId());
            ds.writeShort(hatgiong.getSoluong());
        }

        ds.writeByte(this.session.user.NongSan.size());
        for (NongSan ns : this.session.user.NongSan) {
            ds.writeByte(ns.getId());
            ds.writeShort(ns.getSoluong());
        }

        ds.writeInt(Math.toIntExact(this.session.user.getXu()));
        ds.writeByte(us.getLeverFarm());
        ds.writeByte(us.getLeverPercen());

        ds.writeByte(this.session.user.PhanBon.size());
        for (PhanBon pb : this.session.user.PhanBon) {
            ds.writeShort(pb.getId());
            ds.writeShort(pb.getSoluong());
        }
        /// ///////
        ds.writeByte(nongsandacbiet.size());
        for (KeyValue<Integer, Integer> i : nongsandacbiet) {
            ds.writeShort(i.getKey());
            ds.writeShort(i.getValue());
        }
        ds.writeByte(1);
        ds.writeInt(64000);
        ds.writeBoolean(true);
        ds.writeShort(us.getLeverFarm());
        ds.writeByte(us.getLeverPercen());
        ds.writeByte(this.session.user.NongSan.size());
        for (NongSan ns : this.session.user.NongSan) {
            ds.writeShort(ns.getId());
            ds.writeInt(ns.getSoluong());
        }

        /// //////
        ds.writeByte(nongsandacbiet.size());
        for (KeyValue<Integer, Integer> i : nongsandacbiet) {
            ds.writeShort(i.getKey());
            ds.writeInt(i.getValue());
        }
        ds.flush();
        this.session.sendMessage(ms);
    }

    private void writeInfoCell(DataOutputStream ds, LandItem land) throws IOException {
        ds.writeShort(land.getPlantedTime() == null ? 0 : (int)land.getMinutesSincePlanted());
        ds.writeByte(land.getSucKhoe());
        ds.writeByte(1); //100 la héo
        ds.writeBoolean(land.isWatered());
        ds.writeBoolean(land.isFertilized());
        ds.writeBoolean(land.isHarvestable());
    }

    private void writeInfoAnimal(DataOutputStream ds, Animal animal) throws IOException {
        ds.writeInt(animal.getHealth());
        ds.writeByte(animal.getLevel());
        ds.writeByte(animal.getResourceCount());
        ds.writeByte(animal.getNextProductionTime());
        ds.writeBoolean(animal.isAlive());
        ds.writeBoolean(animal.isReadyForBreeding());
        ds.writeBoolean(animal.isHarvestable());
    }


    public void joinFarm(Message ms) throws IOException, SQLException {
        int userId = ms.reader().readInt();
        int friendFarmId = pendingFriendFarmId;
        pendingFriendFarmId = -1;
        viewedFarmId = -1;
        User owner = userId == session.user.getId() ? session.user
                : avatar.server.UserManager.getInstance().find(userId);
        if (owner == null) {
            try (Connection connection = DbManager.getInstance().getConnection();
                 PreparedStatement query = connection.prepareStatement("SELECT user_id FROM players WHERE user_id = ?")) {
                query.setInt(1, userId);
                try (ResultSet result = query.executeQuery()) {
                    if (!result.next()) { serverDialog("Không tìm thấy nông trại này."); return; }
                }
            }
            owner = new User(); owner.setId(userId); owner.loadFarmData(userId);
        }
        synchronized (owner) {
            viewedFarmId = userId;
            ms = new Message(Cmd.JOIN);
            DataOutputStream ds = ms.writer();
            ds.writeInt(userId);


            int landSize = owner.landItems.size();
            ds.writeByte(landSize);  // Ghi số lượng ô đất

            // Ghi thông tin các ô đất (cây)
            for (int i = 0; i < landSize; ++i) {
                LandItem landItem =  owner.landItems.get(i);
                if (landItem.getType() >= 0) {
                    ds.writeByte(landItem.getType());  //id
                    writeInfoCell(ds, landItem);
                } else {
                    ds.writeByte(-1);  // Không có cây trong ô đất này
                }
            }
            ds.writeByte(owner.Animal.size());  // Ghi số lượng động vật
            for (Animal animal : owner.Animal) {
                ds.writeByte(animal.getId());  // Ghi ID động vật (có thể là `animal.getType()` hoặc ID khác)
                writeInfoAnimal(ds, animal);  // Ghi thông tin về động vật vào DataOutputStream
            }

            // Ghi thông tin khác
            ds.writeByte(10);// chỉnh kích cỡ chuồng bò
            ds.writeByte(10);//chỉnh kích cỡ hồ cá
            ds.writeShort(10);  //cấp độ cây khế
            ds.writeShort(43);//43//img cay khe
            ds.writeShort(46);//46//img qua khe
            ds.writeShort(owner.getStarfruitQuantity());  // Số khế còn trên cây
            ds.writeShort(0);//
            ds.writeShort(0);//0
            ds.writeShort(0);//0

            // Ghi trạng thái các ô đất
            for (int i = 0; i < landSize; ++i) {
                ds.writeByte(1);  // Trạng thái ô đất
            }

            ds.writeShort(owner.getCookingRecipe());
            ds.writeShort(owner.cookingRemainingMinutes(System.currentTimeMillis()));
            ds.flush();

            this.session.sendMessage(ms);
        }
        // Avatar 2.5.8 consumes the first JOIN as preload, then enables farm rendering.
        if (friendFarmId >= 0) {
            Message visit = new Message(Cmd.JOIN);
            visit.writer().writeInt(friendFarmId);
            joinFarm(new Message(Cmd.JOIN, visit.getData()));
        }
    }


    public void getImgFarm(Message ms) throws IOException {
        short imageID = ms.reader().readShort();
        String folder = session.getResourcesPath() + "farm/";
        byte[] dat = Avatar.getFile(folder + imageID + ".png");
        if (dat == null) {
            return;
        }
        ms = new Message(Cmd.GET_IMG_FARM);
        DataOutputStream ds = ms.writer();
        ds.writeShort(imageID);
        ds.writeShort(dat.length);
        ds.write(dat);
        ds.flush();
        this.session.sendMessage(ms);
    }
}
