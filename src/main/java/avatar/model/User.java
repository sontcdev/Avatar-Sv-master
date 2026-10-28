package avatar.model;

import avatar.Farm.*;
import avatar.common.BossShopItem;
import avatar.db.DbManager;
import avatar.item.Item;
import avatar.item.Part;
import avatar.lucky.DialLucky;
import avatar.lucky.GiftBox;
import avatar.network.Session;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import avatar.service.*;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;

import java.io.IOException;

import avatar.network.Message;
import avatar.play.MapService;

import avatar.play.Zone;
import avatar.server.GameString;
import avatar.server.ServerManager;
import avatar.server.UserManager;
import avatar.server.Utils;

import java.util.Date;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;

@Getter
@Setter
public class User {
    private static int chestLevel;
    private static final int[] UPGRADE_COST_COINS = {0, 0, 0, 20000, 50000, 100000, 200000, 200000, 500000, 600000, 70000, 0, 1000000, 1200000, 1500000, 1700000, 2000000, 2500000, 2700000, 3000000, 4000000, 5000000};
    private static final int[] UPGRADE_COST_GOLD = {0, 0, 0, 0, 0, 0, 0, 200, 500, 600, 700, 1000, 1000, 1200, 1500, 1700, 2000, 2500, 2700, 3000, 4000, 5000};
    public boolean AutoFish;

    public int bossMapId;
    public int TopPhaoLuong;
    public int TopPhaoXu;

    public int xu_from_boss;
    private boolean spamclickBoss;

    private int intSpanboss;
    public int spam;

    public int HP;

    private boolean isDefeated;
    private boolean isSpam;
/// //////hen ho
    private int idUsHenHo;
    private String namehh;
    private List<Item> wearingMarry;
    private int levelMarry;
    private int PerLevelMarry;
    private int imginfo;
    private String tenNhan;
/// //////////
    private int storedXuUpdate; // Biến lưu trữ xu đã cập nhật
    private static final Logger logger = Logger.getLogger(User.class);
    public Session session;
    private int id;
    private String username;
    private String password;
    private short idFish;
    private byte gender;
    public long xu;
    private boolean farmSaleUncertain;
    private long farmRevision = -1;
    private long playerRevision = -1;

    private SQLException persistenceConflict(String scope) {
        logger.error("Persistence conflict; retained in-memory snapshot, user=" + id + ", scope=" + scope);
        return new SQLTransactionRollbackException("Dữ liệu đã thay đổi ở phiên khác. Vui lòng đăng nhập lại.", "40001");
    }

    public synchronized int[] sellFarmProduct(int product, int price) throws SQLException {
        // shortcut: native 63 has no nonce; replay after a later harvest needs a verified protocol extension.
        if (farmSaleUncertain) throw new SQLException("Sale outcome requires reconnect");
        if (price <= 0 || product < 0) throw new IllegalArgumentException("Giá bán hoặc sản phẩm không hợp lệ.");
        int memoryQuantity = product == 215
                ? NongSanDacBiet.stream().filter(i -> i.getId() == product).mapToInt(i -> i.getSoluong()).sum()
                : NongSan.stream().filter(i -> i.getId() == product).mapToInt(i -> i.getSoluong()).sum();
        if (memoryQuantity <= 0 || memoryQuantity > Short.MAX_VALUE)
            throw new IllegalArgumentException("Không có nông sản hợp lệ để bán.");
        String column = product == 215 ? "nongsandacbiet" : "nongsan";
        try (Connection c = DbManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                long balance;
                try (PreparedStatement p = c.prepareStatement("SELECT xu, persistence_revision FROM players WHERE user_id=? FOR UPDATE")) {
                    p.setInt(1,id);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Missing player for sale");
                        balance = r.getLong(1);
                        if (r.getLong(2) != playerRevision) throw persistenceConflict("sale player");
                    }
                }
                JSONArray stock;
                try (PreparedStatement p = c.prepareStatement("SELECT " + column + ", persistence_revision FROM farm_data WHERE user_id=? FOR UPDATE")) {
                    p.setInt(1,id);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Missing farm for sale");
                        if (r.getLong(2) != farmRevision) throw persistenceConflict("sale farm");
                        Object parsed = JSONValue.parse(r.getString(1));
                        if (!(parsed instanceof JSONArray)) throw new SQLException("Invalid farm inventory");
                        stock = (JSONArray) parsed;
                    }
                }
                JSONObject owned = null;
                for (Object entry : stock) {
                    if (!(entry instanceof JSONObject)) throw new SQLException("Invalid inventory entry");
                    JSONObject item = (JSONObject) entry;
                    if (!(item.get("id") instanceof Long) || !(item.get("soluong") instanceof Long))
                        throw new SQLException("Invalid inventory numbers");
                    if (((Number)item.get("id")).longValue() == product) {
                        if (owned != null) throw new SQLException("Duplicate product entries");
                        owned = item;
                    }
                }
                if (owned == null || ((Number)owned.get("soluong")).longValue() != memoryQuantity || balance != xu)
                    throw new IllegalArgumentException("Kho hoặc số dư đã thay đổi. Vui lòng đăng nhập lại.");
                int earned = Math.multiplyExact(memoryQuantity,price);
                int updated = Math.toIntExact(Math.addExact(balance,earned));
                if (balance < 0 || updated < 0) throw new ArithmeticException("Invalid currency balance");
                owned.put("soluong",0);
                try (PreparedStatement p = c.prepareStatement("UPDATE farm_data SET " + column + "=?, persistence_revision=persistence_revision+1 WHERE user_id=? AND persistence_revision=?")) {
                    p.setString(1,stock.toJSONString()); p.setInt(2,id); p.setLong(3,farmRevision);
                    if (p.executeUpdate() != 1) throw new SQLException("Farm sale update lost");
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE players SET xu=?, persistence_revision=persistence_revision+1 WHERE user_id=? AND persistence_revision=?")) {
                    p.setInt(1,updated); p.setInt(2,id); p.setLong(3,playerRevision);
                    if (p.executeUpdate() != 1) throw new SQLException("Currency sale update lost");
                }
                // A lost commit acknowledgement must never be overwritten by logout's stale snapshot.
                farmSaleUncertain = true;
                c.commit();
                farmRevision++; playerRevision++;
                xu = updated;
                if (product == 215) NongSanDacBiet.stream().filter(i -> i.getId() == product).forEach(i -> i.setSoluong(0));
                else NongSan.stream().filter(i -> i.getId() == product).forEach(i -> i.setSoluong(0));
                farmSaleUncertain = false;
                return new int[]{earned,updated};
            } catch (SQLException | RuntimeException failure) {
                try { c.rollback(); }
                catch (SQLException rollbackFailure) { farmSaleUncertain = true; failure.addSuppressed(rollbackFailure); }
                throw failure;
            }
        }
    }
    public int luong;
    public int luongKhoa;

    private List<Integer> availableSkills;
    private int useSkill;
    private int dame;
    private int dameToXu;
    public long randomTimeInMillis; // Random time in milliseconds
    public long lastTimeSet;
    public int correctAnswer; // Biến lưu trữ kết quả đúng của phép toán

    public int xeng;
    private short clanID;
    private byte role;
    private byte star;
    private int leverMain;
    private int expMain;
    private int leverFarm;
    private byte leverPercen;
    private int expFarm;
    private byte friendly;
    private short crazy;// item sk quatyti
    private byte stylish;
    private byte happy;// 100 hop qua
    private byte hunger;//100 kẹo tối đa
    private byte chestSlot;
    private byte chestHomeSlot;
    private int scores;// điểm sk
    private List<Item> wearing;
    public List<Item> chests;
    public List<Item> chestsHome = new ArrayList<>();


    /// /fam
    public List<LandItem> landItems = new ArrayList<>();
    public List<Animal> Animal = new ArrayList<>();
    public List<HatGiong> hatgiong = new ArrayList<>();
    public List<NongSan> NongSan = new ArrayList<>();
    public List<PhanBon> PhanBon = new ArrayList<>();
    public List<NongSanDacBiet> NongSanDacBiet = new ArrayList<>();
    // shortcut: retain local capacity 180; add level-specific capacity only with a verified economy catalog.
    private int starfruitQuantity = 180;
    private long starfruitLastHarvestMs;
    private int cookingRecipe;
    private long cookingReadyMs;
    private int cookingOutputId;

    public synchronized int cookingRemainingMinutes(long now) {
        return cookingRecipe == 0 ? 0 : (int) Math.min(Short.MAX_VALUE,
                Math.max(0, cookingReadyMs - now + 59_999L) / 60_000L);
    }

    public synchronized void startCooking(FarmCookingCatalog.Recipe recipe, long now) throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Persistence outcome requires reconnect");
        if (cookingRecipe != 0) throw new IllegalArgumentException("Bạn đang nấu một món ăn.");
        if (recipe == null || now <= 0) throw new IllegalArgumentException("Công thức không hợp lệ.");
        long ready = Math.addExact(now, Math.multiplyExact((long) recipe.durationMinutes(), 60_000L));
        for (java.util.Map.Entry<Integer,Integer> ingredient : recipe.ingredients().entrySet()) {
            int count = 0, quantity = 0;
            for (NongSan stock : NongSan) if (stock.getId() == ingredient.getKey()) { count++; quantity = stock.getSoluong(); }
            for (NongSanDacBiet stock : NongSanDacBiet) if (stock.getId() == ingredient.getKey()) { count++; quantity = stock.getSoluong(); }
            if (count != 1 || quantity < ingredient.getValue() || quantity > Short.MAX_VALUE)
                throw new IllegalArgumentException("Kho không đủ nguyên liệu hoặc dữ liệu kho không hợp lệ.");
        }
        for (java.util.Map.Entry<Integer,Integer> ingredient : recipe.ingredients().entrySet()) {
            for (NongSan stock : NongSan) if (stock.getId() == ingredient.getKey()) stock.setSoluong(stock.getSoluong() - ingredient.getValue());
            for (NongSanDacBiet stock : NongSanDacBiet) if (stock.getId() == ingredient.getKey()) stock.setSoluong(stock.getSoluong() - ingredient.getValue());
        }
        cookingRecipe = recipe.id(); cookingOutputId = recipe.outputId(); cookingReadyMs = ready;
        long previousRevision = farmRevision;
        try { saveFarmData(id); }
        catch (SQLException failure) {
            if (farmRevision != previousRevision) farmSaleUncertain = true;
            for (java.util.Map.Entry<Integer,Integer> ingredient : recipe.ingredients().entrySet()) {
                for (NongSan stock : NongSan) if (stock.getId() == ingredient.getKey()) stock.setSoluong(stock.getSoluong() + ingredient.getValue());
                for (NongSanDacBiet stock : NongSanDacBiet) if (stock.getId() == ingredient.getKey()) stock.setSoluong(stock.getSoluong() + ingredient.getValue());
            }
            cookingRecipe = 0; cookingOutputId = 0; cookingReadyMs = 0;
            throw failure;
        }
    }

    public synchronized int collectCooking(long now) throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Persistence outcome requires reconnect");
        if (cookingRecipe <= 0 || cookingOutputId <= 0 || cookingReadyMs <= 0 || now < cookingReadyMs)
            throw new IllegalArgumentException("Món ăn chưa sẵn sàng để thu hoạch.");
        NongSanDacBiet stock = null;
        for (NongSanDacBiet item : NongSanDacBiet) if (item.getId() == cookingOutputId) {
            if (stock != null) throw new IllegalArgumentException("Dữ liệu kho không hợp lệ.");
            stock = item;
        }
        int oldQuantity = stock == null ? 0 : stock.getSoluong();
        if (oldQuantity < 0 || oldQuantity >= Short.MAX_VALUE || (stock == null && NongSanDacBiet.size() >= 127))
            throw new IllegalArgumentException("Kho đã đầy.");
        int recipe = cookingRecipe, output = cookingOutputId; long ready = cookingReadyMs;
        boolean added = stock == null;
        if (added) { stock = new NongSanDacBiet(output, 0); NongSanDacBiet.add(stock); }
        stock.setSoluong(oldQuantity + 1);
        cookingRecipe = 0; cookingOutputId = 0; cookingReadyMs = 0;
        long previousRevision = farmRevision;
        try { saveFarmData(id); }
        catch (SQLException failure) {
            if (farmRevision != previousRevision) farmSaleUncertain = true;
            if (added) NongSanDacBiet.remove(stock); else stock.setSoluong(oldQuantity);
            cookingRecipe = recipe; cookingOutputId = output; cookingReadyMs = ready;
            throw failure;
        }
        return output;
    }


    public synchronized int getStarfruitQuantity() {
        if (starfruitLastHarvestMs <= 0) return Math.max(0, Math.min(180, starfruitQuantity));
        long elapsedMinutes = Math.max(0, (System.currentTimeMillis() - starfruitLastHarvestMs) / 60_000L);
        return (int) (Math.min(60, elapsedMinutes) * 180 / 60);
    }

    public synchronized boolean claimDailyAttendance() throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Persistence outcome requires reconnect");
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        synchronized (chests) {
            Item ticket = chests.stream().filter(item -> item.getId() == 593).findFirst().orElse(null);
            Part ticketPart = avatar.item.PartManager.getInstance().findPartByID(593);
            if (ticketPart == null || ticketPart.getType() != -2)
                throw new IllegalArgumentException("Chưa có dữ liệu vé điểm danh hợp lệ.");
            if (ticket == null && chests.size() >= chestSlot)
                throw new IllegalArgumentException("Rương đồ đã đầy.");
            if (ticket != null && (ticket.getQuantity() < 0 || ticket.getQuantity() >= 20000))
                throw new IllegalArgumentException("Số vé trong rương đã đạt giới hạn.");
            int newScores = Math.addExact(scores, 20);
            JSONArray saved = new JSONArray();
            for (Item item : chests) {
                JSONObject value = new JSONObject();
                value.put("id", item.getId()); value.put("expired", item.getExpired());
                value.put("quantity", item.getQuantity() + (item == ticket ? 1 : 0));
                saved.add(value);
            }
            if (ticket == null) {
                JSONObject value = new JSONObject(); value.put("id", 593);
                value.put("expired", -1); value.put("quantity", 1); saved.add(value);
            }
            // The conditional write commits the reward and day together, including across reconnects.
            try (Connection connection = DbManager.getInstance().getConnection();
                 PreparedStatement update = connection.prepareStatement(
                         "UPDATE players SET chests = ?, scores = ?, attendance_date = ?, persistence_revision=persistence_revision+1 " +
                         "WHERE user_id = ? AND (attendance_date IS NULL OR attendance_date < ?) AND persistence_revision=? LIMIT 1")) {
                update.setString(1, saved.toJSONString()); update.setInt(2, newScores);
                update.setDate(3, java.sql.Date.valueOf(today)); update.setInt(4, id);
                update.setDate(5, java.sql.Date.valueOf(today));
                update.setLong(6, playerRevision);
                connection.setAutoCommit(false);
                try {
                    if (update.executeUpdate() == 0) {
                        connection.rollback();
                        try (PreparedStatement check = connection.prepareStatement("SELECT attendance_date FROM players WHERE user_id=?")) {
                            check.setInt(1,id);
                            try (ResultSet row = check.executeQuery()) {
                                if (row.next() && row.getDate(1) != null && !row.getDate(1).toLocalDate().isBefore(today)) return false;
                            }
                        }
                        throw persistenceConflict("attendance");
                    }
                    farmSaleUncertain = true;
                    connection.commit();
                    playerRevision++;
                    farmSaleUncertain = false;
                } catch (SQLException failure) {
                    try { connection.rollback(); }
                    catch (SQLException rollback) { farmSaleUncertain = true; failure.addSuppressed(rollback); }
                    throw failure;
                }
            } catch (SQLException failure) {
                // Resource close can fail after commit, before the RAM reward is applied.
                farmSaleUncertain = true;
                throw failure;
            }
            if (ticket == null) chests.add(new Item(593, -1, 1));
            else ticket.setQuantity(ticket.getQuantity() + 1);
            scores = newScores;
            return true;
        }
    }

    public synchronized void plantCustomCrop(int index, int seedId, CustomCropPolicy policy, long now) throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Persistence outcome requires reconnect");
        if (index < 0 || index >= landItems.size() || index > 127 || now <= 0 || seedId != policy.seedId)
            throw new IllegalArgumentException("Hạt giống hoặc ô đất không hợp lệ.");
        LandItem previous = landItems.get(index);
        if (previous.getType() >= 0 || previous.getCustomPolicy() != null)
            throw new IllegalArgumentException("Ô đất này đã có cây.");
        HatGiong seed = null;
        for (HatGiong item : hatgiong) if (item.getId() == seedId) {
            if (seed != null) throw new IllegalArgumentException("Dữ liệu kho không hợp lệ.");
            seed = item;
        }
        if (seed == null || seed.getSoluong() <= 0 || seed.getSoluong() > Short.MAX_VALUE)
            throw new IllegalArgumentException("Không đủ hạt giống.");
        long deadline = Math.addExact(now, CustomCropPolicy.GROWTH_MS);
        LandItem crop = new LandItem(0, CustomCropPolicy.CROP_ID, CustomCropPolicy.HEALTH, 0, false, false, false,
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), java.time.ZoneOffset.UTC));
        crop.setCustomCrop(CustomCropPolicy.VERSION, UUID.randomUUID().toString(), now, deadline, policy.productId);
        seed.setSoluong(seed.getSoluong() - 1);
        landItems.set(index, crop);
        long previousRevision = farmRevision;
        try { saveFarmData(id); }
        catch (SQLException | RuntimeException failure) {
            if (farmRevision != previousRevision) farmSaleUncertain = true;
            landItems.set(index, previous); seed.setSoluong(seed.getSoluong() + 1);
            throw failure;
        }
    }

    public synchronized int harvestCustomCrop(int index, CustomCropPolicy policy, long now) throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Persistence outcome requires reconnect");
        if (index < 0 || index >= landItems.size() || index > 127) throw new IllegalArgumentException("Ô đất không hợp lệ.");
        LandItem crop = landItems.get(index);
        if (!CustomCropPolicy.VERSION.equals(crop.getCustomPolicy()) || crop.getType() != CustomCropPolicy.CROP_ID
                || crop.getCustomInstance() == null || crop.getCustomProductId() != policy.productId || crop.getCustomPlantedMs() <= 0
                || crop.getCustomDeadlineMs() - crop.getCustomPlantedMs() != CustomCropPolicy.GROWTH_MS
                || now < crop.getCustomDeadlineMs()) throw new IllegalArgumentException("Cây chưa sẵn sàng thu hoạch.");
        NongSan stock = null;
        for (NongSan item : NongSan) if (item.getId() == policy.productId) {
            if (stock != null) throw new IllegalArgumentException("Dữ liệu kho không hợp lệ.");
            stock = item;
        }
        int old = stock == null ? 0 : stock.getSoluong();
        if (old < 0 || old > Short.MAX_VALUE - CustomCropPolicy.HARVEST_QUANTITY || (stock == null && NongSan.size() >= 127))
            throw new IllegalArgumentException("Kho nông sản đã đầy.");
        boolean added = stock == null;
        if (added) { stock = new NongSan(policy.productId, 0); NongSan.add(stock); }
        stock.setSoluong(old + CustomCropPolicy.HARVEST_QUANTITY);
        landItems.set(index, new LandItem(0, -1, 0, 0, false, false, false, null));
        long previousRevision = farmRevision;
        try { saveFarmData(id); }
        catch (SQLException | RuntimeException failure) {
            if (farmRevision != previousRevision) farmSaleUncertain = true;
            landItems.set(index, crop);
            if (added) NongSan.remove(stock); else stock.setSoluong(old);
            throw failure;
        }
        return CustomCropPolicy.HARVEST_QUANTITY;
    }

    public synchronized void plantCrop(int index, int seedId) throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Persistence outcome requires reconnect");
        if (index < 0 || index >= landItems.size()) throw new IllegalArgumentException("Ô đất không hợp lệ.");
        LandItem previous = landItems.get(index);
        if (previous.getType() >= 0) throw new IllegalArgumentException("Ô đất này đã có cây.");
        HatGiong seed = findhatgiong(seedId);
        farmItem crop = avatar.item.PartManager.getInstance().findFarmitemByID(seedId);
        if (seed == null || seed.getSoluong() <= 0 || crop == null || crop.getTime() <= 0 || crop.getQuantity() <= 0)
            throw new IllegalArgumentException("Không có hạt giống hoặc dữ liệu cây hợp lệ.");
        landItems.set(index, new LandItem(0, seedId, 100, 0, false, false, false, LocalDateTime.now()));
        seed.setSoluong(seed.getSoluong() - 1);
        long previousRevision = farmRevision;
        try { saveFarmData(id); }
        catch (SQLException failure) {
            if (farmRevision != previousRevision) farmSaleUncertain = true;
            landItems.set(index, previous);
            seed.setSoluong(seed.getSoluong() + 1);
            throw failure;
        }
    }

    public synchronized int harvestCrop(int index) throws SQLException {
        if (index < 0 || index >= landItems.size()) throw new IllegalArgumentException("Ô đất không hợp lệ.");
        LandItem land = landItems.get(index);
        if (land.getCustomPolicy() != null) throw new IllegalArgumentException("Cây này không được hỗ trợ.");
        farmItem crop = avatar.item.PartManager.getInstance().findFarmitemByID(land.getType());
        if (crop == null) throw new IllegalArgumentException("Ô đất trống hoặc cây chưa có dữ liệu thu hoạch.");
        if (land.getPlantedTime() == null || land.getMinutesSincePlanted() < crop.getTime())
            throw new IllegalArgumentException("Cây chưa đến thời gian thu hoạch.");
        int quantity = (int) ((long) crop.getQuantity() * Math.max(0, Math.min(100, land.getSucKhoe())) / 100);
        if (quantity <= 0) throw new IllegalArgumentException("Cây không có nông sản để thu hoạch.");
        NongSan stock = NongSan.stream().filter(item -> item.getId() == crop.getId()).findFirst().orElse(null);
        int oldStock = stock == null ? 0 : stock.getSoluong();
        if (oldStock < 0 || quantity > Short.MAX_VALUE - oldStock || (stock == null && NongSan.size() >= 127))
            throw new IllegalArgumentException("Kho nông sản đã đầy.");
        boolean added = stock == null;
        if (added) { stock = new NongSan(crop.getId(), 0); NongSan.add(stock); }
        stock.setSoluong(oldStock + quantity);
        int oldType = land.getType();
        land.setType(-1);
        try { saveFarmData(id); }
        catch (SQLException failure) {
            land.setType(oldType);
            if (added) NongSan.remove(stock); else stock.setSoluong(oldStock);
            throw failure;
        }
        return quantity;
    }

    public synchronized int harvestStarfruit() throws SQLException {
        int quantity = getStarfruitQuantity();
        if (quantity <= 0) return 0;
        NongSanDacBiet stock = null;
        for (NongSanDacBiet item : NongSanDacBiet) if (item.getId() == 215) {
            if (stock != null) throw new SQLException("Duplicate starfruit inventory entries");
            stock = item;
        }
        int oldStock = stock == null ? 0 : stock.getSoluong();
        if (oldStock < 0 || quantity > Short.MAX_VALUE - oldStock || (stock == null && NongSanDacBiet.size() >= 127)) {
            throw new SQLException("Starfruit inventory exceeds client capacity");
        }
        boolean added = stock == null;
        if (added) { stock = new NongSanDacBiet(215, 0); NongSanDacBiet.add(stock); }
        int previousQuantity = starfruitQuantity;
        long previousHarvestMs = starfruitLastHarvestMs;
        stock.setSoluong(oldStock + quantity);
        starfruitQuantity = 0;
        starfruitLastHarvestMs = System.currentTimeMillis();
        try {
            saveFarmData(id);
        } catch (SQLException failure) {
            starfruitQuantity = previousQuantity;
            starfruitLastHarvestMs = previousHarvestMs;
            if (added) NongSanDacBiet.remove(stock); else stock.setSoluong(oldStock);
            throw failure;
        }
        return quantity;
    }
/// /////
    private Zone zone;
    private short x, y;
    private byte direct;
    private List<Menu> menus;
    private DialLucky dialLucky;
    private short idImg = -1;
    private List<Command> listCmd;
    private List<Command> listCmdRotate;

    @Getter
    @Setter
    private boolean loadDataFinish;

    @Getter
    @Setter
    private List<BossShopItem> bossShopItems;
    private List<Part> ShopEvent;


    private List<Byte> boardIDs;
    private byte roomID;
    List<Byte> moneyPutList;
    public boolean isToXong;
    public boolean isHaPhom;

    public User() {
        this.role = -1;
        this.chests = new ArrayList<>();
        this.wearing = new ArrayList<>();

        this.landItems = new ArrayList<>();
        this.Animal = new ArrayList<>();
        this.hatgiong = new ArrayList<>();
        this.NongSan = new ArrayList<>();
        this.NongSanDacBiet = new ArrayList<>();
        this.PhanBon = new ArrayList<>();



        this.listCmd = new ArrayList<>();
        this.listCmdRotate = new ArrayList<>();
        this.isDefeated = false;
        this.isSpam = false;
        this.boardIDs = new ArrayList<>();
        this.moneyPutList = new ArrayList<>();
        this.availableSkills = new ArrayList<>();
        this.useSkill = 0;


        this.wearingMarry = new ArrayList<>();
    }

    public synchronized int getIntSpanboss() {
        return intSpanboss;
    }

    public void incrementIntSpanboss() {
        this.intSpanboss++;
    }

    public void resetIntSpanboss() {
        this.intSpanboss = 0;
    }

    // Phương thức để reset tất cả thông tin của người chơi
    public void resetUser() {
        resetIntSpanboss();
        setspamclickBoss(false);
    }

    public synchronized boolean getspamclickBoss() {
        return spamclickBoss;
    }

    public synchronized void setspamclickBoss(boolean spamclickBoss) {
        this.spamclickBoss = spamclickBoss;
    }

    public synchronized boolean isHaPhom() {
        return this.isHaPhom;
    }

    public synchronized void setHaPhom(boolean isHaPhom) {
        this.isHaPhom = isHaPhom;
    }

    public synchronized boolean isToXong() {
        return this.isToXong;
    }

    public synchronized void setToXong(boolean isToXong) {
        this.isToXong = isToXong;
    }


    public List<Byte> getMoneyPutList() {
        return this.moneyPutList;
    }

    public synchronized void updateMoneyPutList(List<Byte> newMoneyPutList) {
        if (this.moneyPutList == null) {
            this.moneyPutList = new ArrayList<>(); // Khởi tạo danh sách nếu chưa có
        }
        this.moneyPutList.clear(); // Xóa danh sách cũ (nếu cần)
        this.moneyPutList.addAll(newMoneyPutList); // Thêm các phần tử mới
    }

    public synchronized void updateMoneyPutListByIndex(byte indexFrom, byte indexTo) {
        if (this.moneyPutList != null && this.moneyPutList.size() > 0) {
            // Kiểm tra xem indexFrom và indexTo có hợp lệ trong danh sách không
            if (indexFrom >= 0 && indexFrom < this.moneyPutList.size() &&
                    indexTo >= 0 && indexTo < this.moneyPutList.size()) {

                // Lấy giá trị tại vị trí indexFrom
                Byte valueToMove = this.moneyPutList.get(indexFrom);

                // Xóa phần tử tại vị trí indexFrom
                this.moneyPutList.remove(indexFrom);

                // Thêm phần tử vào vị trí indexTo
                this.moneyPutList.add(indexTo, valueToMove);
            }
        }
    }


    public synchronized void setUseSkill(int skill) {
        this.useSkill = skill;
    }

    public List<Integer> getListSkill() {
        return this.availableSkills;
    }

    public synchronized void calculateDameToXu() {
        int totalDamage = 30;
        if (this.getStar() == 2) {
            totalDamage = 80;
        }
        List<Integer> Item1 = Arrays.asList(3440, 3443, 3174, 3972, 4442, 6142, 4121);  // Set mũ
        List<Integer> Item2 = Arrays.asList(3441, 3445, 3176, 3974, 4443, 4122);  // Set áo
        List<Integer> Item3 = Arrays.asList(3442, 3446, 3177, 3975, 4444, 4123);  // Set quần
        int countItem1 = 0, countItem2 = 0, countItem3 = 0;
        boolean cung = false, maybay = false, haoquanhoalong = false, bang = false, hophong = false;
// Kiểm tra toàn bộ items nhân vật đang mặc
        for (Item item : wearing) {
            totalDamage += item.getPart().getLevel();

            // Kiểm tra các item đặc biệt
            cung = cung || item.getId() == 6400;
            maybay = maybay || item.getId() == 4715;
            haoquanhoalong = haoquanhoalong || item.getId() == 5455;
            bang = bang || item.getId() == 6485;
            hophong = hophong || item.getId() == 5828;
            // Kiểm tra nếu item thuộc set siêu anh hùng
            if (Item1.contains(item.getId())) countItem1++;
            if (Item2.contains(item.getId())) countItem2++;
            if (Item3.contains(item.getId())) countItem3++;
        }
// Xử lý các kỹ năng tương ứng
        handleSkillSet(countItem1, countItem2, countItem3);
        handleSkill(cung, 2);  // Kỹ năng cung
        handleSkill(maybay, 4);  // Kỹ năng máy bay
        handleSkill(haoquanhoalong, 5);
        handleSkill(bang, 6);
        handleSkill(hophong, 7);

// Cập nhật damage cuối cùng
        this.dameToXu = totalDamage;
    }

    private void handleSkill(boolean hasItem, int skillId) {
        if (hasItem) {
            addSkill(skillId);  // Thêm kỹ năng
            this.useSkill = skillId;
        } else {
            removeSkill(skillId);  // Xóa kỹ năng nếu không còn item đặc biệt
        }
    }

    private void handleSkillSet(int countItem1, int countItem2, int countItem3) {
        if (countItem1 > 0 && countItem2 > 0 && countItem3 > 0) {
            addSkill(1);  // Thêm skill 1 nếu mặc đủ set trang bị
            this.useSkill = 1;
        } else {
            removeSkill(1);  // Thiếu món nào thì xóa skill 1
        }
    }

    private void addSkill(int skillId) {
        if (!this.availableSkills.contains(skillId)) {
            this.availableSkills.add(skillId);
        }
    }

    private void removeSkill(int skillId) {
        this.availableSkills.remove(Integer.valueOf(skillId));
        this.useSkill = 0;  // Reset skill được sử dụng nếu xóa
    }

    public User(String username, int xuFromBoss) {
        this.username = username;
        this.xu_from_boss = xuFromBoss;
    }

    public User(String username, int xeng, int TopPhaoLuong) {
        this.username = username;
        this.xeng = xeng;
        this.TopPhaoLuong = TopPhaoLuong;
    }

    public User(String username, int id, int xeng, int TopPhaoXu) {
        this.username = username;
        this.id = id;
        this.xeng = xeng;
        this.TopPhaoXu = TopPhaoXu;
    }

    public AvatarService getAvatarService() {
        return session.getAvatarService();
    }

    public FarmService getFarmService() {
        return session.getFarmService();
    }

    public HomeService getHomeService() {
        return session.getHomeService();
    }

    public ParkService getParkService() {
        return session.getParkService();
    }

    public MapService getMapService() {
        if (zone == null) {
            return NoService.getInstance();
        }
        return zone.getService();
    }


    public Service getService() {
        return session.getService();
    }

    public void sortWearing() {
        this.wearing.sort((o1, o2) -> o1.getPart().getZOrder() - o2.getPart().getZOrder());
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public synchronized void updateXu(long xuUp) {
        this.xu += xuUp;
    }

    public synchronized void updateXuKillBoss(int dame) {
        this.storedXuUpdate += dame; // Lưu xu vào biến tạm thời
    }

    public synchronized void applyStoredXuUpdate() {
        //this.updateXu(storedXuUpdate * 5); // Cộng dồn số xu ba lần
        this.Updatexu_from_boss(storedXuUpdate);
        Utils.writeLog(this, "xu : " + storedXuUpdate + " X " + this.getDame() + " dame to xu = >" + this.xu);
        this.storedXuUpdate = 0; // Reset xu đã lưu trữ
    }

    public synchronized void updateCrazy(int crazy) {
        this.crazy += crazy;
    }//1k item câu cá

    public synchronized void updateHappy(int Happy) {
        this.happy += Happy;
    }

    public synchronized void updateHunger(int hunger) {
        this.hunger += (byte) hunger;
    }//100 vp kill bos

    public synchronized void updateXP(int XP) {
        this.expMain += XP;
    }

    public synchronized void Updatexu_from_boss(int xu_from_boss) {
        this.xu_from_boss += xu_from_boss;
    }

    public synchronized void updateLuong(int luongUp) {
        this.luong += luongUp;
        try {
            this.getAvatarService().SendTabmsg("Luong : " + this.luong);
            Utils.writeLog(this, "luong : " + luong);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void updateScores(int ScoresUp) {
        this.scores += ScoresUp;
    }

    public synchronized void updateLuongKhoa(int luongUp) {
        this.luong += luongUp;
    }

    public synchronized void updateXeng(int xengUp) {
        this.xeng += xengUp;
    }

    public synchronized void updateChestSlot(int chestslot) {
        this.chestSlot += (byte) chestslot;
    }

    public synchronized void updateChest_homeSlot(int chestslot) {
        this.chestHomeSlot += (byte) chestslot;
    }

    public synchronized void updateHP(long dame, Boss boss, User us) throws IOException {
        this.HP += dame;
        if (HP <= 0) {
            HP = 0;
            if (!isDefeated) {
                isDefeated = true;
                // Chỉ thực hiện xử lý khi boss chưa bị đánh bại
                boss.handleBossDefeat(boss, us);
            }
        }
    }

    public synchronized void updateSpam(long spams, Boss boss, User us) throws IOException {
        boss.spam += spams;
        System.out.println("Spam " + boss.getSpam());
        if (boss.getSpam() <= 0) {
            boss.spam = 0;
            isSpam = false;
            if (!isSpam) {
                isSpam = true;
                boss.hanlderNhatHopQua(boss, us);
            }
        }
    }

    public boolean isSpam() {
        return isSpam;
    }

    public boolean isDefeated() {
        return isDefeated;
    }

    public synchronized byte getRoomID() {
        return this.roomID;
    }

    public synchronized void setRoomID(byte RoomID) {
        this.roomID = RoomID;
    }

    public long getRandomTimeInMillis() {
        return randomTimeInMillis;
    }

    // Setter cho randomTimeInMillis
    public void setRandomTimeInMillis(long randomTimeInMillis) {
        this.randomTimeInMillis = randomTimeInMillis;
    }

    // Getter cho lastTimeSet
    public long getcorrectAnswer() {
        return this.correctAnswer;
    }

    // Setter cho lastTimeSet
    public void setcorrectAnswer(int sum) {
        this.correctAnswer = sum;
    }

    public void set(long randomTimeInMillis) {
        this.randomTimeInMillis = randomTimeInMillis;
    }

    // Getter cho lastTimeSet
    public long getLastTimeSet() {
        return lastTimeSet;
    }


    public synchronized int getChestLevel() {
        int chestSlotHome = this.getChestHomeSlot(); // Lấy số ô của rương hiện tại

        if (chestSlotHome <= 10) {
            return 1; // Cấp 1 với 10 ô
        } else if (chestSlotHome <= 15) {
            return 2; // Cấp 2 với 15 ô
        } else if (chestSlotHome <= 20) {
            return 3; // Cấp 3 với 20 ô
        } else if (chestSlotHome <= 25) {
            return 4; // Cấp 4 với 25 ô
        } else if (chestSlotHome <= 30) {
            return 5; // Cấp 4 với 25 ô
        } else if (chestSlotHome <= 35) {
            return 6; // Cấp 4 với 25 ô
        } else if (chestSlotHome <= 40) {
            return 7; // Cấp 4 với 25 ô
        } else if (chestSlotHome <= 45) {
            return 8; // Cấp 4 với 25 ô
        } else if (chestSlotHome <= 50) {
            return 9; // Cấp 4 với 25 ô
        } else if (chestSlotHome <= 55) {
            return 10; // Cấp 4 với 25 ô
        }

        return -1; // Trường hợp không hợp lệ
    }

    public synchronized void updateTopPhaoLuong(int luongThaPhao) {
        int next = Math.addExact(TopPhaoLuong,1);
        int nextLuong = Math.addExact(luong,luongThaPhao);
        persistTopCounter("TopPhaoLuong",next);
        luong = nextLuong; TopPhaoLuong = next;
    }

    public synchronized void updateTopPhaoXu(int xuThaPhao) {
        int next = Math.addExact(TopPhaoXu,1);
        long nextXu = Math.addExact(xu,xuThaPhao);
        persistTopCounter("TopPhaoXu",next);
        xu = nextXu; TopPhaoXu = next;
    }

    private void persistTopCounter(String column, int value) {
        if (farmSaleUncertain) throw new IllegalStateException("Persistence outcome requires reconnect");
        try (Connection c = DbManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("UPDATE players SET " + column + "=?, persistence_revision=persistence_revision+1 WHERE user_id=? AND persistence_revision=?")) {
                ps.setInt(1,value); ps.setInt(2,id); ps.setLong(3,playerRevision);
                if (ps.executeUpdate()!=1) throw persistenceConflict("counter");
                farmSaleUncertain = true;
                c.commit(); playerRevision++; farmSaleUncertain = false;
            } catch (SQLException failure) {
                try { c.rollback(); }
                catch (SQLException rollback) { farmSaleUncertain = true; failure.addSuppressed(rollback); }
                throw failure;
            }
        } catch (SQLException failure) {
            // Keep logout blocked if resource close skipped the caller's RAM update.
            farmSaleUncertain = true;
            throw new IllegalStateException("Counter persistence failed",failure);
        }
    }


    public void sendMessage(Message ms) {
        this.session.sendMessage(ms);
    }

    protected synchronized void saveData() {
        JSONArray jChests = new JSONArray();
        for (Item item : this.chests) {
            JSONObject obj = new JSONObject();
            obj.put("id", item.getId());
            obj.put("expired", item.getExpired());
            obj.put("quantity", item.getQuantity());
            checkItemQuantityLog(item, "saveData error" + item.getPart().getName()

            );
            jChests.add(obj);
        }
        JSONArray jWearing = new JSONArray();
        for (Item item : this.wearing) {
            JSONObject obj = new JSONObject();
            obj.put("id", item.getId());
            obj.put("expired", item.getExpired());
            obj.put("quantity", item.getQuantity());
            jWearing.add(obj);
        }

        JSONArray jChestsHome = new JSONArray();
        for (Item item : this.chestsHome) {
            JSONObject obj = new JSONObject();
            obj.put("id", item.getId());
            obj.put("expired", item.getExpired());
            obj.put("quantity", item.getQuantity());
            jChestsHome.add(obj);
        }

        Object[] values = {gender,friendly,crazy,stylish,happy,hunger,chestSlot,chestHomeSlot,xu,luong,luongKhoa,xeng,leverMain,expMain,scores,xu_from_boss,TopPhaoLuong,TopPhaoXu,jChests.toJSONString(),jWearing.toJSONString(),jChestsHome.toJSONString(),id,playerRevision};
        if (farmSaleUncertain) {
            preserveRejectedSnapshot(values);
            logger.error("Skipping stale logout snapshot after uncertain persistence, user=" + id);
            return;
        }
        try (Connection connection = DbManager.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE players SET gender=?,friendly=?,crazy=?,stylish=?,happy=?,hunger=?,chest_slot=?,chest_home_slot=?,xu=?,luong=?,luong_khoa=?,xeng=?,level_main=?,exp_main=?,scores=?,xu_from_boss=?,TopPhaoLuong=?,TopPhaoXu=?,chests=?,wearing=?,chests_home=?,persistence_revision=persistence_revision+1 WHERE user_id=? AND persistence_revision=?")) {
                    for (int i=0;i<values.length;i++) ps.setObject(i+1,values[i]);
                    if (ps.executeUpdate()!=1) throw persistenceConflict("logout player");
                }
                long nextFarmRevision = saveFarmData(connection,id);
                farmSaleUncertain = true;
                connection.commit();
                playerRevision++; farmRevision = nextFarmRevision;
                farmSaleUncertain = false;
            } catch (SQLException failure) {
                try { connection.rollback(); }
                catch (SQLException rollback) { farmSaleUncertain = true; failure.addSuppressed(rollback); }
                throw failure;
            }
        } catch (SQLException failure) {
            preserveRejectedSnapshot(values);
            logger.error("Logout snapshot not saved; reconciliation required, user=" + id, failure);
            throw new IllegalStateException("Logout persistence failed for user " + id,failure);
        }
    }

    public synchronized void saveFarmData(int userId) throws SQLException {
        if (farmSaleUncertain) throw new SQLException("Uncertain sale; reload before saving farm");
        if (userId != id) throw new SQLException("Farm owner mismatch");
        try (Connection connection = DbManager.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                long nextRevision = saveFarmData(connection,userId);
                farmSaleUncertain = true;
                connection.commit();
                farmRevision = nextRevision;
                farmSaleUncertain = false;
            } catch (SQLException failure) {
                try { connection.rollback(); }
                catch (SQLException rollback) { farmSaleUncertain = true; failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }

    private long saveFarmData(Connection connection, int userId) throws SQLException {
        Object[] values = farmSnapshot();
        String query = farmRevision < 0
                ? "INSERT INTO farm_data (land_data,animal_data,hatgiong,phanbon,nongsan,nongsandacbiet,starfruit_quantity,starfruit_last_harvest_ms,cooking_recipe,cooking_ready_ms,cooking_output_id,user_id,persistence_revision) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,0)"
                : "UPDATE farm_data SET land_data=?,animal_data=?,hatgiong=?,phanbon=?,nongsan=?,nongsandacbiet=?,starfruit_quantity=?,starfruit_last_harvest_ms=?,cooking_recipe=?,cooking_ready_ms=?,cooking_output_id=?,persistence_revision=persistence_revision+1 WHERE user_id=? AND persistence_revision=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            for (int i=0;i<values.length;i++) ps.setObject(i+1,values[i]);
            ps.setInt(12,userId);
            if (farmRevision >= 0) ps.setLong(13,farmRevision);
            if (ps.executeUpdate()!=1) throw persistenceConflict("farm");
        }
        return farmRevision < 0 ? 0 : Math.addExact(farmRevision,1);
    }

    private Object[] farmSnapshot() {
        JSONArray landData = new JSONArray();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (LandItem landItem : this.landItems) {
            JSONObject landObject = new JSONObject();
            landObject.put("growthTime", landItem.getGrowthTime());
            landObject.put("type", landItem.getType());//lao
            landObject.put("suckhoe", landItem.getSucKhoe());//skhoe
            landObject.put("resourceCount", landItem.getResourceCount());
            landObject.put("isWatered", landItem.isWatered());
            landObject.put("isFertilized", landItem.isFertilized());
            landObject.put("isHarvestable", landItem.isHarvestable());

            LocalDateTime plantedTime = landItem.getPlantedTime();
            if (plantedTime != null) {
                landObject.put("plantedTime", plantedTime.format(formatter));
            } else {
                landObject.put("plantedTime", "not_planted"); // Hoặc loại bỏ dòng này
            }

            if (landItem.getCustomPolicy() != null) {
                landObject.put("customPolicy", landItem.getCustomPolicy());
                landObject.put("customInstance", landItem.getCustomInstance());
                landObject.put("customPlantedMs", landItem.getCustomPlantedMs());
                landObject.put("customDeadlineMs", landItem.getCustomDeadlineMs());
                landObject.put("customProductId", landItem.getCustomProductId());
            }
            landData.add(landObject);
        }


        JSONArray animalData = new JSONArray();
        for (Animal animal : this.Animal) {
            JSONObject animalObject = new JSONObject();
            animalObject.put("id", animal.getId());
            animalObject.put("health", animal.getHealth());
            animalObject.put("level", animal.getLevel());
            animalObject.put("resourceCount", animal.getResourceCount());
            animalObject.put("nextProductionTime", animal.getNextProductionTime());
            animalObject.put("isAlive", animal.isAlive());
            animalObject.put("isReadyForBreeding", animal.isReadyForBreeding());
            animalObject.put("isHarvestable", animal.isHarvestable());
            animalData.add(animalObject);
        }

        JSONArray hatgiongData = new JSONArray();
        for (HatGiong hatGiong : this.hatgiong) {
            JSONObject hatGiongObject = new JSONObject();
            hatGiongObject.put("id", hatGiong.getId());
            hatGiongObject.put("soluong", hatGiong.getSoluong());
            hatgiongData.add(hatGiongObject);
        }


        JSONArray phanbonData = new JSONArray();
        for (PhanBon phanBon : this.PhanBon) {
            JSONObject phanBonObject = new JSONObject();
            phanBonObject.put("id", phanBon.getId());
            phanBonObject.put("soluong", phanBon.getSoluong());
            phanbonData.add(phanBonObject);
        }

        JSONArray nongsanData = new JSONArray();
        for (NongSan nongSan : this.NongSan) {
            JSONObject nongSanObject = new JSONObject();
            nongSanObject.put("id", nongSan.getId());
            nongSanObject.put("soluong", nongSan.getSoluong());
            nongsanData.add(nongSanObject);
        }

        JSONArray nongsandacbietData = new JSONArray();
        for (NongSanDacBiet nongsandacbiet : this.NongSanDacBiet) {
            JSONObject nongsandacbietObject = new JSONObject();
            nongsandacbietObject.put("id", nongsandacbiet.getId());
            nongsandacbietObject.put("soluong", nongsandacbiet.getSoluong());
            nongsandacbietData.add(nongsandacbietObject);
        }


        return new Object[]{landData.toJSONString(),animalData.toJSONString(),hatgiongData.toJSONString(),phanbonData.toJSONString(),nongsanData.toJSONString(),nongsandacbietData.toJSONString(),starfruitQuantity,starfruitLastHarvestMs,cookingRecipe,cookingReadyMs,cookingOutputId};
    }

    private void preserveRejectedSnapshot(Object[] playerValues) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("logs/persistence-conflicts");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path file = java.nio.file.Files.createTempFile(dir,id+"-",".json",
                    java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
            JSONObject snapshot = new JSONObject();
            snapshot.put("user_id",id); snapshot.put("player_revision",playerRevision); snapshot.put("farm_revision",farmRevision);
            snapshot.put("player_values",java.util.Arrays.asList(playerValues));
            snapshot.put("farm_values",java.util.Arrays.asList(farmSnapshot()));
            try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(file,java.nio.file.StandardOpenOption.WRITE)) {
                java.nio.ByteBuffer bytes = java.nio.charset.StandardCharsets.UTF_8.encode(snapshot.toJSONString());
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            logger.error("Rejected persistence snapshot requires manual reconciliation: " + file);
        } catch (IOException | RuntimeException failure) {
            logger.fatal("Could not retain rejected snapshot; manual recovery required, user=" + id,failure);
        }
    }

    public synchronized void loadFarmData(int userId) throws SQLException {

        String query = "SELECT land_data, animal_data,hatgiong,phanbon,nongsan,nongsandacbiet,starfruit_quantity,starfruit_last_harvest_ms,cooking_recipe,cooking_ready_ms,cooking_output_id,persistence_revision FROM `farm_data` WHERE user_id = ?";

        try (Connection connection = DbManager.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);

            try (ResultSet res = ps.executeQuery()) {
                if (res.next()) {
                    farmRevision = res.getLong("persistence_revision");
                    starfruitQuantity = res.getInt("starfruit_quantity");
                    starfruitLastHarvestMs = res.getLong("starfruit_last_harvest_ms");
                    cookingRecipe = res.getInt("cooking_recipe");
                    cookingReadyMs = res.getLong("cooking_ready_ms");
                    cookingOutputId = res.getInt("cooking_output_id");
                    String landDataString = res.getString("land_data");
                    String animalDataString = res.getString("animal_data");
                    String hatgiongDataString = res.getString("hatgiong");
                    String phanbonDataString = res.getString("phanbon");
                    String nongsanDataString = res.getString("nongsan");
                    String nongsandacbietDataString = res.getString("nongsandacbiet");

                    // Phân tích dữ liệu ô đất (land_data)
                    JSONArray landData = (JSONArray) JSONValue.parse(landDataString);
                    List<LandItem> landItems = new ArrayList<>();

                    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                    for (Object land : landData) {
                        JSONObject obj = (JSONObject) land;
                        int growthTime = ((Long) obj.get("growthTime")).intValue();
                        int type = ((Long) obj.get("type")).intValue();
                        int suckhoe = ((Long) obj.get("suckhoe")).intValue();
                        int resourceCount = ((Long) obj.get("resourceCount")).intValue();
                        boolean isWatered = (Boolean) obj.get("isWatered");
                        boolean isFertilized = (Boolean) obj.get("isFertilized");
                        boolean isHarvestable = (Boolean) obj.get("isHarvestable");

                        String plantedTimeStr = (String) obj.get("plantedTime");
                        LocalDateTime plantedTime = plantedTimeStr == null || "not_planted".equals(plantedTimeStr)
                                ? null : LocalDateTime.parse(plantedTimeStr, formatter);

                        LandItem landItem = new LandItem(growthTime, type,suckhoe, resourceCount, isWatered, isFertilized, isHarvestable, plantedTime);
                        if (obj.get("customPolicy") != null) {
                            if (!(obj.get("customPolicy") instanceof String) || !(obj.get("customInstance") instanceof String)
                                    || !(obj.get("customPlantedMs") instanceof Number) || !(obj.get("customDeadlineMs") instanceof Number)
                                    || !(obj.get("customProductId") instanceof Number))
                                throw new IllegalStateException("Invalid custom crop data");
                            landItem.setCustomCrop((String) obj.get("customPolicy"), (String) obj.get("customInstance"),
                                    ((Number) obj.get("customPlantedMs")).longValue(), ((Number) obj.get("customDeadlineMs")).longValue(),
                                    ((Number) obj.get("customProductId")).intValue());
                        }
                        landItems.add(landItem);
                    }
                    // Cập nhật danh sách ô đất cho người chơi
                    this.landItems = landItems;

                    // Phân tích dữ liệu vật nuôi (animal_data)
                    JSONArray animalData = (JSONArray) JSONValue.parse(animalDataString);
                    List<Animal> animals = new ArrayList<>();

                    for (Object animal : animalData) {
                        JSONObject obj = (JSONObject) animal;
                        int id = ((Long) obj.get("id")).intValue();
                        int health = ((Long) obj.get("health")).intValue();
                        int level = ((Long) obj.get("level")).intValue();
                        int resourceCount = ((Long) obj.get("resourceCount")).intValue();
                        int nextProductionTime = ((Long) obj.get("nextProductionTime")).intValue();
                        boolean isAlive = (Boolean) obj.get("isAlive");
                        boolean isReadyForBreeding = (Boolean) obj.get("isReadyForBreeding");
                        boolean isHarvestable = (Boolean) obj.get("isHarvestable");

                        Animal animalObj = new Animal(id, health, level, resourceCount, nextProductionTime, isAlive, isReadyForBreeding, isHarvestable);
                        animals.add(animalObj);
                    }
                    // Cập nhật danh sách vật nuôi cho người chơi
                    this.Animal = animals;



                    JSONArray hatgiongdata = (JSONArray) JSONValue.parse(hatgiongDataString);
                    List<HatGiong> hatgiongs = new ArrayList<>();

                    for (Object hatgiong : hatgiongdata) {
                        JSONObject obj = (JSONObject) hatgiong;
                        int id = ((Long) obj.get("id")).intValue();
                        int soluong = ((Long) obj.get("soluong")).intValue();

                        HatGiong animalObj = new HatGiong(id, soluong);
                        hatgiongs.add(animalObj);
                    }
                    this.hatgiong = hatgiongs;


                    JSONArray phanbondata = (JSONArray) JSONValue.parse(phanbonDataString);
                    List<PhanBon> phanBons = new ArrayList<>();

                    for (Object phanbon : phanbondata) {
                        JSONObject obj = (JSONObject) phanbon;
                        int id = ((Long) obj.get("id")).intValue();
                        int soluong = ((Long) obj.get("soluong")).intValue();

                        PhanBon pb = new PhanBon(id, soluong);
                        phanBons.add(pb);
                    }
                    this.PhanBon = phanBons;



                    JSONArray nongsandata = (JSONArray) JSONValue.parse(nongsanDataString);
                    List<NongSan> nongSans = new ArrayList<>();

                    for (Object nongsan : nongsandata) {
                        JSONObject obj = (JSONObject) nongsan;
                        int id = ((Long) obj.get("id")).intValue();
                        int soluong = ((Long) obj.get("soluong")).intValue();

                        NongSan ns = new NongSan(id, soluong);
                        nongSans.add(ns);
                    }
                    this.NongSan = nongSans;


                    JSONArray nongsandbdata = (JSONArray) JSONValue.parse(nongsandacbietDataString);
                    List<NongSanDacBiet> nongSandbs = new ArrayList<>();

                    for (Object nongsandb : nongsandbdata) {
                        JSONObject obj = (JSONObject) nongsandb;
                        int id = ((Long) obj.get("id")).intValue();
                        int soluong = ((Long) obj.get("soluong")).intValue();

                        NongSanDacBiet nsdb = new NongSanDacBiet(id, soluong);
                        nongSandbs.add(nsdb);
                    }
                    this.NongSanDacBiet = nongSandbs;


                }
            }
        }

        // Nếu không có dữ liệu, tạo mặc định cho người chơi
        if (this.landItems.isEmpty()) {
            // Tạo mặc định cho 6 ô đất
            List<LandItem> defaultLandItems = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                defaultLandItems.add(new LandItem(0, -1,-1, 0, false, false, false, LocalDateTime.now())); // Cây mặc định
            }
            this.landItems = defaultLandItems;
        }

        if (this.Animal.isEmpty()) {
            // Không có vật nuôi, nên không cần thêm gì
            this.Animal = new ArrayList<>();
        }

        if (this.hatgiong.isEmpty()) {
            // Không có vật nuôi, nên không cần thêm gì
            this.hatgiong = new ArrayList<>();
        }
        if (this.PhanBon.isEmpty()) {
            // Không có vật nuôi, nên không cần thêm gì
            this.PhanBon = new ArrayList<>();
        }
        if (this.NongSan.isEmpty()) {
            // Không có vật nuôi, nên không cần thêm gì
            this.NongSan = new ArrayList<>();
        }
        if (this.NongSanDacBiet.isEmpty()) {
            // Không có vật nuôi, nên không cần thêm gì
            this.NongSanDacBiet = new ArrayList<>();
        }

    }

    public synchronized boolean login() {
        if (!ServerManager.active) {
            getService().serverMessage("Máy chủ đang bảo trì. Vui lòng quay lại sau : v");
            return false;
        }

        String ACCOUNT_LOGIN = "SELECT * FROM `users` WHERE `username` = ? AND `password` = ? LIMIT 1 FOR UPDATE;";
        String SET_LOCK_ACCOUNT = "UPDATE `users` SET `login_lock` = 1 WHERE `id` = ?;";
        try (Connection connection = DbManager.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(ACCOUNT_LOGIN)) {

            ps.setString(1, this.username);
            ps.setString(2, Utils.md5(password));
            connection.setAutoCommit(false);  // Bắt đầu transaction

            try (ResultSet red = ps.executeQuery()) {
                if (red.next()) {
                    this.id = red.getInt("id");
                    this.role = (byte) red.getInt("role");
                    boolean active = red.getBoolean("active");
                    if (!active) {
                        getService().serverMessage(GameString.userLoginActive());
                        connection.rollback();
                        return false;
                    }

                    // Kiểm tra khóa đăng nhập
                    if (red.getInt("login_lock") == 1) {
                        getService().serverMessage(GameString.userLoginMany());
                        connection.rollback();  // Rollback nếu phát hiện người dùng đang đăng nhập
                        return false;
                    }

                    // Đặt khóa đăng nhập
                    try (PreparedStatement setLockStmt = connection.prepareStatement(SET_LOCK_ACCOUNT)) {
                        setLockStmt.setInt(1, this.id);
                        setLockStmt.executeUpdate();
                    }

                    // Kiểm tra nếu tài khoản bị cấm
                    JSONObject banData = (JSONObject) ((red.getString("ban") != null)
                            ? JSONValue.parse(red.getString("ban"))
                            : new JSONObject());
                    if (!banData.isEmpty()) {
                        int banType = ((Long) banData.get("type")).intValue();
                        if (banType == 2) {
                            if (banData.get("forever") != null) {
                                getService().serverMessage(GameString.userLoginLockForever());
                                connection.rollback();
                                return false;
                            }
                            int minutes = ((Long) banData.get("minutes")).intValue();
                            Date timeNowwww = new Date();
                            Date banStart = Utils.getDate((String) banData.get("start"));
                            Date banEnd = new Date(banStart.getTime() + 60000 * minutes);
                            if (banEnd.after(timeNowwww)) {
                                minutes = (int) ((banEnd.getTime() - timeNowwww.getTime()) / 60000L);
                                getService().serverMessage(GameString.userLoginLock(minutes));
                                connection.rollback();
                                return false;
                            }
                        }
                    }

                    // Kiểm tra nếu người dùng đã đăng nhập từ thiết bị khác
                    User us = UserManager.getInstance().find(this.id);
                    if (us != null) {
                        getService().serverMessage(GameString.userLoginMany());
                        connection.rollback();
                        return false;
                    }

                    // Mọi thứ OK, commit và giữ khóa đăng nhập
                    connection.commit();
                    return true;
                } else {
                    getService().serverMessage(GameString.loginPassFail());
                }
            }
        } catch (SQLException ex) {
            getService().serverMessage(ex.getMessage());
        }
        return false;
    }


    public void GetdataUserHenho() {
        String GET_PLAYER_DATA = "SELECT wearing FROM `players` WHERE `user_id` = ? LIMIT 1;";
        try (Connection connection = DbManager.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(GET_PLAYER_DATA);) {
            ps.setInt(1, this.idUsHenHo);
            try (ResultSet res = ps.executeQuery()) {
                if (res.next()) {
                    this.wearingMarry = new ArrayList<>();
                    JSONArray wearing = (JSONArray) JSONValue.parse(res.getString("wearing"));
                    for (Object o : wearing) {
                        JSONObject obj = (JSONObject) o;
                        int id = ((Long) obj.get("id")).intValue();
                        long expired = ((Long) obj.get("expired"));
                        int quantity = 1;
                        if (obj.containsKey("quantity")) {
                            quantity = ((Long) obj.get("quantity")).intValue();
                        }
                        Item item = Item.builder().id(id)
                                .quantity(quantity)
                                .expired(expired)
                                .build();
                        if (item.reliability() > 0) {
                            this.wearingMarry.add(item);
                        }
                    }
                }
            } catch (SQLException ex) {
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loadData() {
        String GET_PLAYER_DATA = "SELECT * FROM `players` WHERE `user_id` = ? LIMIT 1;";
        try (Connection connection = DbManager.getInstance().getConnection();
             PreparedStatement ps = connection.prepareStatement(GET_PLAYER_DATA);) {
            ps.setInt(1, this.id);
            try (ResultSet res = ps.executeQuery()) {
                if (res.next()) {
                    if (res.getInt("user_id") == 7) {
                        //this.id+=(Npc.ID_ADD+1000);
                    }
                    this.playerRevision = res.getLong("persistence_revision");
                    this.leverMain = res.getInt("level_main");
                    this.expMain = res.getInt("exp_main");
                    this.gender = res.getByte("gender");
                    this.chestSlot = res.getByte("chest_slot");
                    this.chestHomeSlot = res.getByte("chest_home_slot");
                    this.xu = res.getLong("xu");
                    Utils.writeLog(this,"xu load :" + this.xu);
                    this.luong = res.getInt("luong");
                    Utils.writeLog(this,"luong load :" + this.luong);
                    this.luongKhoa = res.getInt("luong_khoa");
                    this.xeng = res.getInt("xeng");
                    this.clanID = res.getShort("clan_id");
                    this.friendly = res.getByte("friendly");
                    this.crazy = res.getShort("crazy");//vp sk/
                    this.stylish = res.getByte("stylish");
                    this.happy = res.getByte("happy");
                    this.hunger = res.getByte("hunger");//quà
                    this.star = res.getByte("star");
                    this.scores = res.getInt("scores");
                    this.xu_from_boss = res.getInt("xu_from_boss");
                    this.TopPhaoLuong = res.getInt("TopPhaoLuong");
                    this.TopPhaoXu = res.getInt("TopPhaoXu");
                    this.chests = new ArrayList<>();
                    JSONArray chests = (JSONArray) JSONValue.parse(res.getString("chests"));
                    for (Object chest : chests) {
                        JSONObject obj = (JSONObject) chest;
                        int id = ((Long) obj.get("id")).intValue();
                        long expired = ((Long) obj.get("expired"));
                        int quantity = 1;
                        if (obj.containsKey("quantity")) {
                            quantity = ((Long) obj.get("quantity")).intValue();
                            if(quantity>100||quantity<0){
                                Utils.writeLog(this,"loadData quantity " + quantity);
                            }
                            if(quantity>15000||quantity<0) {
                                Utils.writeLog(this, "loadData quantity và khoa acc" + quantity);
                                Utils.writeLogKhoaAcc(this, "loadData quantity và khoa acc" + quantity);
//                                String sql1 = "UPDATE users SET active = ? WHERE id = ?";
//                                try (Connection connection1 = DbManager.getInstance().getConnection();
//                                     PreparedStatement ps1 = connection1.prepareStatement(sql1)) {
//
//                                    ps1.setInt(1, 0);  // Khóa tài khoản
//                                    ps1.setInt(2, this.id);
//                                    int rowsUpdated = ps1.executeUpdate();
//                                    if (rowsUpdated == 0) {
//                                        Utils.writeLog(this, "Không có bản ghi nào được cập nhật với ID " + this.id);
//                                    }
//                                    this.session.close();
//                                }
                            }
                            //có gì khóa acc
                        }
                        Item item = Item.builder().id(id)
                                .quantity(quantity)
                                .expired(expired)
                                .build();
                        if (item.reliability() > 0) {
                            this.chests.add(item);
                        }
                    }
                    this.wearing = new ArrayList<>();
                    JSONArray wearing = (JSONArray) JSONValue.parse(res.getString("wearing"));
                    for (Object o : wearing) {
                        JSONObject obj = (JSONObject) o;
                        int id = ((Long) obj.get("id")).intValue();
                        long expired = ((Long) obj.get("expired"));
                        int quantity = 1;
                        if (obj.containsKey("quantity")) {
                            quantity = ((Long) obj.get("quantity")).intValue();
                        }
                        Item item = Item.builder().id(id)
                                .quantity(quantity)
                                .expired(expired)
                                .build();
                        if (item.reliability() > 0) {
                            this.wearing.add(item);
                        }
                    }
                    this.chestsHome = new ArrayList<>();
                    JSONArray chestshome = (JSONArray) JSONValue.parse(res.getString("chests_home"));
                    for (Object chest : chestshome) {
                        JSONObject obj = (JSONObject) chest;
                        int id = ((Long) obj.get("id")).intValue();
                        long expired = ((Long) obj.get("expired"));
                        int quantity = 1;
                        if (obj.containsKey("quantity")) {
                            quantity = ((Long) obj.get("quantity")).intValue();
                        }
                        Item item = Item.builder().id(id)
                                .quantity(quantity)
                                .expired(expired)
                                .build();
                        if (item.reliability() > 0) {
                            this.chestsHome.add(item);
                        }
                    }
                    loadFarmData(this.id);
                    calculateDameToXu();



                    String checkExistQuery = "SELECT * FROM marry WHERE idNam = ? OR idNu = ?";
                    int userId = this.id; // ID của người dùng hiện tại

                    try (Connection conn = DbManager.getInstance().getConnection();
                         PreparedStatement psCheck = conn.prepareStatement(checkExistQuery)) {

                        // Cung cấp ID của người dùng để kiểm tra
                        psCheck.setInt(1, userId);
                        psCheck.setInt(2, userId);

                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next()) {
                                // Nếu có kết quả, chúng ta lấy idNam và idNu
                                int idNam = rs.getInt("idNam");
                                int idNu = rs.getInt("idNu");
                                this.levelMarry = rs.getInt("level");
                                this.PerLevelMarry = rs.getInt("perLevel");
                                // Kiểm tra xem ID người dùng hiện tại là idNam hay idNu, và lấy ID còn lại
                                int otherId = (idNam == userId) ? idNu : idNam;

                                // Lấy thông tin của người còn lại
                                String userInfoQuery = "SELECT * FROM users WHERE id = ?";
                                try (PreparedStatement psUser = conn.prepareStatement(userInfoQuery)) {
                                    psUser.setInt(1, otherId);

                                    try (ResultSet rsUser = psUser.executeQuery()) {
                                        if (rsUser.next()) {
                                            // Lấy thông tin của người còn lại từ bảng users
                                            String username = rsUser.getString("username");
                                            int userID = rsUser.getInt("id");
                                            this.setIdUsHenHo(userID);
                                            this.setNamehh(username);
                                            GetdataUserHenho();
                                            System.out.println("Người còn lại: " + username+ " , id = " + userID);
                                        }
                                    }
                                }
                            } else {
                                System.out.println("Không tìm thấy người dùng hẹn hò hoặc kết hôn với bạn.");
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }



                    setLoadDataFinish(true);
                    return true;
                }
            }



        } catch (Exception ex) {
            ex.printStackTrace();
            getService().serverMessage(ex.getMessage());
        }
        return false;
    }

    public void initAvatar() {
        sortWearing();
        listCmd.add(new Command("Chức năng", 2));
        listCmdRotate.add(new Command((short) 0, "Hội nhóm", 41, (byte) 1));
        listCmdRotate.add(new Command((short) 4, "Oan Tu Xi", 44, (byte) 1));
        //listCmdRotate.add(new Command((short) 33, "Hô phong hoán vũ", 1053, (byte) 0));
        //listCmdRotate.add(new Command((short) 34, "Triệu hồi bia mộ", 1053, (byte) 0));
        //listCmdRotate.add(new Command((short) 35, "Cánh thần hiển linh", 1055, (byte) 0));
        //listCmdRotate.add(new Command((short) 48, "pháo sinh nhật(5 lượng)", 1115, (byte) 0));
        //listCmdRotate.add(new Command((short) 47, "Pháo hạnh phúc (5 lượng)", 242, (byte) 0));


        //listCmdRotate.add(new Command((short) 8, "Pháo thịnh vượng (5 lượng)", 241, (byte) 0));//sk


        //listCmdRotate.add(new Command((short) 9, "triệu hồi con chim k nhớ tên", 1082, (byte) 0));
        //listCmdRotate.add(new Command((short) 10, "Rương chỉ sử dụng không được bỏ(sẽ bị xóa item ở rương gốc)", 1204, (byte) 0));
        //listCmdRotate.add(new Command((short) 11, "thả bóng bay (20k xu)", 577, (byte) 0)); // sk


        listCmdRotate.add(new Command((short) 23, "Đổi Skill", 355, (byte) 0));
        listCmdRotate.add(new Command((short) 36, "Hẹn hò", 1096, (byte) 1));
    }

    public void doAction(Message ms) {
        try {
            int idTo = ms.reader().readInt();
            short action = ms.reader().readShort();
            User us = UserManager.getInstance().find(idTo);

            if(this.getZone().getMap().getId() == 16){
                this.getAvatarService().serverDialog("Bạn không thể hành động ở đây !");
                return;
            }
            switch (action) {
                case 101:
                    DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
                    int dayIndex = dayOfWeek.getValue(); // 1 = Monday, 7 = Sunday
                    if (dayIndex == 5 || dayIndex == 6) {
                        getMapService().doAction(id, idTo, action);
                        break;
                    }
                    if(gender== us.gender) {
                        this.getAvatarService().serverDialog("làm gì vậy bro, đồng giới thì thứ 6 thứ 7");
                        break;
                    }
                default:
                    getMapService().doAction(id, idTo, action);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getExpMax() {
        return (this.leverMain * (this.leverMain + 1) / 2) * 1000;
    }

    public byte getLeverMainPercen() {
        return (byte) (this.expMain * 100 / getExpMax());
    }

    public void viewChest(Message ms) throws IOException {
        int type = ms.reader().readInt();
        if(type!=id)
        {
            List<Item> _chests = chests.stream().filter(item -> {
                return item.getPart().getZOrder() == 30 || item.getPart().getZOrder() == 40;
            }).collect(Collectors.toList());
            getAvatarService().viewChest(_chests);
            return;
        }
        List<Item> _chests = chests.stream().filter(item -> {
            return item.getPart().getZOrder() != 30 && item.getPart().getZOrder() != 40;
        }).collect(Collectors.toList());
        getAvatarService().viewChest(_chests);
    }

    // hỏi nâng cấp
    public String getUpgradeRequirements() {
        if (chestSlot/5 >= UPGRADE_COST_COINS.length - 1) {
            return "Rương đã đạt cấp tối đa";
        }

        int nextLevel = (chestSlot/5)+1;
        int coinCost = UPGRADE_COST_COINS[nextLevel];
        int goldCost = UPGRADE_COST_GOLD[nextLevel];

        return String.format(
                "Để nâng cấp lên rương cấp %d bạn cần %d xu và %d lượng hoặc thẻ nâng cấp rương.",
                nextLevel-2, coinCost, goldCost);
    }
    // nâng cấp rương
    public String upgradeChest() {
        if (chestSlot/5 >= UPGRADE_COST_COINS.length - 1) {
            return "Rương đã đạt cấp tối đa";
        }

        int nextLevel = (chestSlot/5)+1;
        int coinCost = UPGRADE_COST_COINS[nextLevel];
        int goldCost = UPGRADE_COST_GOLD[nextLevel];

        Item theNangCap = findItemInChests(3861);
        if(theNangCap != null) {
            this.removeItem(3861,1);
            this.updateChestSlot(+5);
            return String.format(
                    "chúc mừng bạn đã nâng cấp thành công rương cấp %d và có %d ô rương.",
                    nextLevel-2, this.getChestSlot()
            );
        }

        if (xu >= coinCost && luong >= goldCost) {
            this.updateXu(-coinCost);
            this.getAvatarService().updateMoney(0);
            this.updateLuong(-goldCost);
            this.updateChestSlot(+5);
            this.getAvatarService().updateMoney(0);
            return String.format(
                    "chúc mừng bạn đã nâng cấp thành công rương cấp %d và có %d ô rương.",
                    nextLevel-2, this.getChestSlot()
            );
        }


        return "không đủ xu hoặc lượng";
    }
    public void requestYourInfo(Message ms) {
        try {
            int userId = ms.reader().readInt();
            User us = UserManager.getInstance().find(userId);
            if (us != null) {
                this.getAvatarService().requestYourInfo(us);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doAvatarFeel(Message ms) {
        try {
            if (ms.reader().available() <= 0) {
                return;
            }
            byte idFeel = ms.reader().readByte();
            System.out.println("doAvatarFeel msg 57 = " + idFeel + " ");
            this.getMapService().doAvatarFeel(id, idFeel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        if (zone != null) {
            zone.leave(this);
        }
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        DbManager.getInstance().executeUpdate("UPDATE `players` SET `is_online` = ?, `client_id` = ?, `last_online` = ? WHERE `user_id` = ? LIMIT 1;", 0, session.id, timestamp, this.id);
        if (isLoadDataFinish()) {
            saveData();
        }
    }

    @Override
    public String toString() {
        return "User " + this.username;
    }

    public void move(Message ms) {
        try {
            if (ms.reader().available() < 5) {
                return;
            }
            short x = ms.reader().readShort();
            short y = ms.reader().readShort();
            byte direct = ms.reader().readByte();
            if (ms.reader().available() >= 2) {
                ms.reader().readShort();
            }
            this.x = x;
            this.y = y;
            this.direct = direct;
            if (session != null) session.customEnglishQuiz.clearIfOutsideContext();
            getMapService().move(this);
            System.out.println("move " + x + ", y = " + y);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addExp(int exp) {
        this.expMain += exp;
        int expMax = getExpMax();
        if (this.expMain >= expMax) {
            this.leverMain++;
            this.expMain -= expMax;
        }
    }

    public boolean checkFullSlotChest(){
        System.out.println("chestSlot: " + this.chestSlot);
        System.out.println("chests.size(): " + this.chests.size());
        if(this.getChestSlot() <= this.getChests().size())
        {
            getAvatarService().serverDialog("Rương đồ đã đầy");
            return true;
        }
        return false;
    }

    public void addItemToChests(Item item) {
        synchronized (chests) {
            if(this.chestSlot <= chests.size()){
                this.getAvatarService().serverDialog("Rương đồ đã đầy");
                return;
            }
            checkItemQuantityLog(item, "addItemToChests error");

            Item itm = findItemInChests(item.getId());

            if (itm != null) {
                // Nếu item đã tồn tại và loại item cho phép (type == -2), tăng số lượng
                if (itm.getPart().getType() == -2) {
                    itm.increase(this, item.getQuantity(), item.getId()); // Giả định phương thức increase đã xử lý đúng
                } else {
                    // Cập nhật độ tin cậy của item (reliability)
                    setReliabilityForItem(itm, item);
                    this.chests.add(item);
                }
            } else {
                // Nếu không tồn tại trong chests, tìm trong wearing
               // itm = findItemInWearing(item.getId());

                if (itm != null) {
                    // Cập nhật độ tin cậy của item trong wearing nếu tìm thấy
                    setReliabilityForItem(itm, item);
                } else {
                    // Nếu không tồn tại cả trong chests và wearing, thêm item vào chests
                    this.chests.add(item);
                    Utils.writeLogAddChest(this,"add item to chests "+ item.getPart().getName());
                }
            }
        }
    }

    public void checkItemQuantityLog(Item item, String message) {
        if (item == null) {
            Utils.writeLog(this, "Lỗi: item là null trong " + message);
            return; // Dừng nếu item là null
        }

        if (item.getQuantity() >= 2 || item.getQuantity() < -1) {
            Utils.writeLog(this, message + " " + item.getQuantity() + " Item " + (item.getPart() != null ? item.getPart().getName() : "Unknown"));
        }
    }

    public void addItemToChestsHome(Item item) {
        synchronized (chestsHome) {
            if(this.chestHomeSlot <= chestsHome.size()){
                this.getAvatarService().serverDialog("Rương nhà đã đầy");
                return;
            }
            checkItemQuantityLog(item,"addItemToChestsHome error");
            Item itm = findItemInChests(item.getId());
            if (itm != null) {
                if (itm.getPart().getType() == -2) {
                    itm.increase(this,item.getQuantity(), item.getId());
                } else {
                    setReliabilityForItem(itm, item);
                }
                this.chestsHome.add(item);
                Utils.writeLogAddChest(this,"add item to chests 1 "+ item.getPart().getName());

                return;
            } else {
                itm = findItemInChests(item.getId());
                if (itm != null) {
                    setReliabilityForItem(itm, item);
                    return;
                }
            }
            this.chestsHome.add(item);
            Utils.writeLogAddChest(this,"add item to chests 2 "+ item.getPart().getName());
        }

    }


    public void setReliabilityForItem(Item old, Item newI) {
        // item expired == -1;
        if (!old.isForever()) {
            if (newI.isForever() || newI.reliability() > old.reliability()) {
                old.setExpired(newI.getExpired());
            }
        }
    }

    public void removeItemFromChests(Item item) {
        synchronized (chests) {
            this.chests.remove(item);
            this.checkItemQuantityLog(item,"removeItemFromChest bug");
        }
    }

    public void removeItemFromChestsHome(Item item) {
        synchronized (chestsHome) {
            this.chestsHome.remove(item);
        }
    }


    public void addItemToWearing(Item item) {
        synchronized (wearing) {
            checkItemQuantityLog(item,"addItemToWearing error");
            Item itm = findItemInWearing(item.getId());
            if (itm == null) {
                this.wearing.add(item);
            } else {
                itm.increase(this,item.getQuantity(), item.getId());
            }
            calculateDameToXu();
        }
    }



    public void removeItemFromWearing(Item item) {
        synchronized (wearing) {
            this.checkItemQuantityLog(item,"removeItemFromWearning bug");
            this.wearing.remove(item);
            calculateDameToXu();
        }
    }

    public Item findItemInChests(int id) {
        synchronized (chests) {
            for (Item item : chests) {
                if (item.getId() == id) {
                    this.checkItemQuantityLog(item,"FindItemInChests bug");
                    return item;
                }
            }
            return null;
        }
    }

    public Item findItemInWearing(int id) {
        synchronized (wearing) {
            for (Item item : wearing) {
                if (item.getId() == id) {
                    this.checkItemQuantityLog(item,"findItemInWearning bug");
                    return item;
                }
            }
            return null;
        }
    }

    public Item findItemInChestsHome(int id) {
        synchronized (chestsHome) {
            for (Item item : chestsHome) {
                if (item.getId() == id) {
                    return item;
                }
            }
            return null;
        }
    }

    public Item findItemWearingByZOrder(int zOrder) {
        synchronized (wearing) {
            for (Item item : wearing) {
                if (item.getPart().getZOrder() == zOrder) {
                    this.checkItemQuantityLog(item,"findItemWearningByZorder bug");
                    return item;
                }
            }
            return null;
        }
    }

    public boolean removeItem(int id, int quantity) {
        Item item = findItemInChests(id);
        checkItemQuantityLog(item,"removeItem error");
        if (item != null) {
            int q = item.reduce(quantity);
            if (q <= 0) {
                removeItemFromChests(item);
            }
            return true;
        }
        return false;
    }


    public HatGiong findhatgiong(int id) {
        synchronized (hatgiong) {
            for (HatGiong hd : hatgiong) {
                if (hd.getId() == id) {
                    return hd;
                }
            }
            return null;
        }
    }









    public void usingItem(short itemID, byte type) {
        try {
            logger.debug("itemID: " + itemID + " type: " + type);
            if (type == 1) {
                logger.debug("Find Item");
                Item item = findItemInChests(itemID);
                logger.debug("Find Done");
                if (item == null) {
                    getService().serverDialog("Không tìm thấy vật phẩm");
                    return;
                }
                logger.debug("Find Detail Item");
                Part part = item.getPart();
                int gender = part.getGender();
                logger.debug("Item Gender=" + gender);
                if ((gender == 1 || gender == 2) && (this.gender != gender)) {
                    getService().serverDialog("Giới tính không phù hợp");
                    return;
                }
                logger.debug("Get Type");
                short pType = part.getType();
                logger.debug("Type =" + pType);
                if (pType == -1) {

                    // hp quà ma quái
                    if(item.getId()==5532)
                    {
                        if((this.chests.size() >= this.getChestSlot())){
                            getService().serverMessage("Bạn phải có ít nhất 1 ô trống");
                            return;
                        }
                        removeItem(item.getId(), 1);
                        GiftBox giftBox = new GiftBox();
                        giftBox.openHopQuaMaQuai(this,item);
                        return;
                    }
                    int zOrder = part.getZOrder();
                    Item w = findItemWearingByZOrder(zOrder);
                    if(this.chestSlot <= chests.size()){
                        this.getAvatarService().serverDialog("Rương đồ đã đầy 001 ");
                        return;
                    }
                    if (w != null) {
                        removeItemFromWearing(w);
                        addItemToChests(w);
                    }
                    addItemToWearing(item);
                    removeItemFromChests(item);
                    sortWearing();
                    getMapService().usingPart(id, itemID);
                } else if (pType == -2) {
                    if(item.getId()==683)
                    {
                        if((this.chests.size() >= this.getChestSlot())){
                            getService().serverMessage("Bạn phải có ít nhất 1 ô trống");
                            return;
                        }
                        removeItem(item.getId(), 1);
                        GiftBox giftBox = new GiftBox();
                        giftBox.open(this,item);
                        return;
                    }
                    if(item.getId()==5408)
                    {
                        if((this.chests.size() >= this.getChestSlot()-4)){
                            getService().serverMessage("Bạn phải có ít nhất 5 ô trống để mở hộp quà hải tặc");
                            return;
                        }
                        removeItem(item.getId(), 1);
                        GiftBox giftBox = new GiftBox();
                        giftBox.openHaiTac(this,item);
                    }
                    if(item.getId()==5324)
                    {
                        if((this.chests.size() >= this.getChestSlot()-2)){
                            getService().serverMessage("Bạn phải có ít nhất 3 ô trống để mở hộp quà siêu nhân");
                            return;
                        }
                        removeItem(item.getId(), 1);
                        GiftBox giftBox = new GiftBox();
                        giftBox.openSieuNhan(this,item);
                    }
                    if(item.getId()==5880)
                    {
                        if((this.chests.size() >= this.getChestSlot()-3)){
                            getService().serverMessage("Bạn phải có ít nhất 4 ô trống để mở hộp quà siêu anh hùng");
                            return;
                        }
                        removeItem(item.getId(), 1);
                        GiftBox giftBox = new GiftBox();
                        giftBox.openSetVuTru(this,item);
                    }

                    else {
                        getService().serverMessage(String.format("Số lượng: %,d", item.getQuantity()));
                    }

                } else {
//                    item = findItemInWearing(itemID);
//                    removeItemFromWearing(item);
//                    addItemToChests(item);
//                    getMapService().usingPart(id, itemID);
                      getService().serverDialog("error 0020");//Vật phẩm shop Loi, sẽ sớm fix
                }
            } else {
                Item item = findItemInWearing(itemID);
                if (item == null) {
                    return;
                }
                int zOrder = item.getPart().getZOrder();
                if (zOrder == 10 || zOrder == 20 || zOrder == 50) {
                    getService().serverDialog("Không thể cất vật phẩm này.");
                    return;
                }
                if(this.chestSlot <= chests.size()){
                    this.getAvatarService().serverDialog("Rương đồ đã đầy 002 ");
                    return;
                }
                removeItemFromWearing(item);
                addItemToChests(item);
                getMapService().usingPart(id, itemID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void usingItem(Message ms) {
        try {
            short itemID = ms.reader().readShort();
            byte type = ms.reader().readByte();
            usingItem(itemID, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void chat(Message ms) {
        try {
            if (ms.reader().available() < 4) {
                return;
            }
            String message = ms.reader().readUTF();
            getMapService().chat(this, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doRemoveItem(Message ms) {
        try {
            short itemID = ms.reader().readShort();
            byte type = ms.reader().readByte();
            if (type == 0) {
                Item item = findItemInWearing(itemID);
                if (item != null) {
                    int zOrder = item.getPart().getZOrder();
                    if (zOrder == 10 || zOrder == 20 || zOrder == 50) {
                        getAvatarService().serverDialog("error : 001");
                        return;
                    }
                    removeItemFromWearing(item);
                    getMapService().removeItem(id, itemID);
                    if (getStylish() > 0) {
                        setStylish((byte) (getStylish() - 1));
                        getAvatarService().requestYourInfo(this);
                    }
                }
            } else {
                Item item = findItemInChests(itemID);
                if (item != null) {
                    removeItemFromChests(item);
                    getAvatarService().removeItem(id, itemID);
                    if (getStylish() > 0) {
                        setStylish((byte) (getStylish() - 1));
                        getAvatarService().requestYourInfo(this);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notifyNetWaitMessage() throws IOException {
        synchronized (this.session.obj) {
            this.session.obj.notifyAll();
        }
    }

    public void skillUidToBoss(List<User> players,int us ,int npcID,byte skill1,byte skill2){
        for (User player : players) {
            EffectService.createEffect()
                    .session(player.session)
                    .id(skill1)
                    .style((byte) 0)
                    .loopLimit((byte) 5)
                    .loop((short) 1)
                    .loopType((byte) 1)
                    .radius((short) 1)
                    .idPlayer(us)
                    .send();
            EffectService.createEffect()
                    .session(player.session)
                    .id(skill2)
                    .style((byte) 0)
                    .loopLimit((byte) 5)
                    .loop((short) 1)
                    .loopType((byte) 1)
                    .radius((short) 1)
                    .idPlayer(npcID)
                    .send();
        };
    }
}
