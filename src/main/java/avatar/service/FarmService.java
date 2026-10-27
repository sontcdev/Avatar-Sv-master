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

    private static final Logger logger = Logger.getLogger(Service.class);

    public FarmService(Session cl) {
        super(cl);
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
        serverDialog("Bán nông sản chưa được hỗ trợ trên máy chủ.");
    }


    //trồng cây
// Phương thức trồng cây
    public void plandSeed(Message ms) throws IOException, SQLException {
        int idUser = ms.reader().readInt();
        int indexCell = ms.reader().readByte();
        int idSeed = ms.reader().readByte();

        if (idUser != session.user.getId() || viewedFarmId != idUser) {
            serverDialog("Bạn chỉ có thể trồng cây ở nông trại của mình.");
            return;
        }
        HatGiong hd = this.session.user.findhatgiong(idSeed);
        if (hd == null || hd.getSoluong() <= 0 || indexCell < 0 || indexCell >= session.user.landItems.size()) {
            serverDialog("Không có hạt giống hoặc ô đất không hợp lệ.");
            return;
        }
        if (session.user.landItems.get(indexCell).getType() >= 0) {
            serverDialog("Ô đất này đã có cây.");
            return;
        }
        // Kiểm tra ô đất có tồn tại trong danh sách không
        if (indexCell >= 0 && indexCell < this.session.user.landItems.size() && hd.getSoluong()>0 ) {
            // Lấy ô đất tại vị trí indexCell và cập nhật thông tin
            LandItem landItem = this.session.user.landItems.get(indexCell);
            landItem.setPlantedTime(LocalDateTime.now());
            landItem.setType(idSeed); // Đặt mã cây mới
            hd.setSoluong(hd.getSoluong()-1);
            landItem.setSucKhoe(100);
            landItem.setGrowthTime(0); // Reset thời gian tăng trưởng, vì cây mới vừa trồng
            landItem.setResourceCount(0); // Đặt số lượng tài nguyên về 0, vì cây mới chưa có tài nguyên
            landItem.setWatered(false); // Đặt trạng thái tưới nước ban đầu
            landItem.setFertilized(false); // Đặt trạng thái bón phân ban đầu
            landItem.setHarvestable(false); // Đặt trạng thái chưa thể thu hoạch
        } else {
            // Nếu ô đất không tồn tại, có thể xử lý ngoại lệ hoặc thông báo lỗi
            System.out.println("Ô đất không tồn tại hoặc indexCell không hợp lệ.");
        }

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
            ds.writeByte(pb.getId());
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
            ds.writeByte(ns.getId());
            ds.writeShort(ns.getSoluong());
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
        viewedFarmId = userId;
        ms = new Message(Cmd.JOIN);
        DataOutputStream ds = ms.writer();
        ds.writeInt(userId);


        int landSize = this.session.user.landItems.size();
        ds.writeByte(landSize);  // Ghi số lượng ô đất

        // Ghi thông tin các ô đất (cây)
        for (int i = 0; i < landSize; ++i) {
            LandItem landItem =  this.session.user.landItems.get(i);
            if (landItem.getType() >= 0) {
                ds.writeByte(landItem.getType());  //id
                writeInfoCell(ds, landItem);
            } else {
                ds.writeByte(-1);  // Không có cây trong ô đất này
            }
        }
        ds.writeByte(this.session.user.Animal.size());  // Ghi số lượng động vật
        for (Animal animal : this.session.user.Animal) {
            ds.writeByte(animal.getId());  // Ghi ID động vật (có thể là `animal.getType()` hoặc ID khác)
            writeInfoAnimal(ds, animal);  // Ghi thông tin về động vật vào DataOutputStream
        }

        // Ghi thông tin khác
        ds.writeByte(10);// chỉnh kích cỡ chuồng bò
        ds.writeByte(10);//chỉnh kích cỡ hồ cá
        ds.writeShort(10);  //cấp độ cây khế
        ds.writeShort(43);//43//img cay khe
        ds.writeShort(46);//46//img qua khe
        ds.writeShort(this.session.user.getStarfruitQuantity());  // Số khế còn trên cây
        ds.writeShort(0);//
        ds.writeShort(0);//0
        ds.writeShort(0);//0

        // Ghi trạng thái các ô đất
        for (int i = 0; i < landSize; ++i) {
            ds.writeByte(1);  // Trạng thái ô đất
        }

        ds.writeShort(3);//nấu ăn
        ds.writeShort(5);
        ds.flush();

        this.session.sendMessage(ms);
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
