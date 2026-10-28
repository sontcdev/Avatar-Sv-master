package avatar.handler;

import avatar.model.Menu;
import avatar.model.User;
import avatar.network.Session;
import avatar.play.Zone;
import java.util.Arrays;
import java.util.stream.Collectors;

/** New custom gameplay, independently versioned from original NPC behavior. */
public final class CustomEnglishQuiz {
    public static final int NPC_ID = 2000000645;
    private static final String[] QUESTIONS = {"Cat là gì?", "Dog là gì?", "Hello nghĩa là gì?"};
    private static final String[][] ANSWERS = {{"Mèo", "Chó", "Cá", "Thoát"},
            {"Chó", "Gà", "Chim", "Thoát"}, {"Xin chào", "Tạm biệt", "Cảm ơn", "Thoát"}};
    private final Session session;
    private Zone zone;
    private int step = -1;
    private int score;
    private int menuId = -1;
    private int nextMenuId;

    public CustomEnglishQuiz(Session session) { this.session = session; }

    public static boolean enabled() { return Boolean.getBoolean("avatar.customEnglishQuizV1"); }

    public synchronized void clear() {
        if (step >= 0 && session.user != null) session.user.setMenus(null);
        step = -1; score = 0; zone = null; menuId = -1;
    }

    private boolean validContext() {
        User user = session.user;
        if (!enabled() || !session.connected || !session.login || user == null || user.getSession() != session
                || user.getZone() == null || user.getZone().getMap().getId() != 6) return false;
        User npc = user.getZone().find(NPC_ID);
        if (npc == null) return false;
        long dx = (long) user.getX() - npc.getX(), dy = (long) user.getY() - npc.getY();
        return dx * dx + dy * dy <= 55L * 55L;
    }

    public synchronized void clearIfOutsideContext() {
        if (step >= 0 && (!validContext() || session.user.getZone() != zone)) clear();
    }

    public synchronized void open() {
        clear();
        if (!validContext()) return;
        zone = session.user.getZone(); score = 0; step = 0;
        show("Bài kiểm tra tiếng Anh", new String[]{"Bài kiểm tra tiếng Anh", "Thoát"});
    }

    public synchronized void select(int id, int selection) {
        if (step < 0 || !validContext() || session.user.getZone() != zone) { clear(); return; }
        if (id != menuId) return;
        if (selection < 0 || selection >= (step == 0 ? 2 : 4)) return;
        if ((step == 0 && selection == 1) || (step > 0 && selection == 3)) { clear(); return; }
        if (step > 0 && selection == 0) score++;
        if (step == 3) {
            int result = score; clear();
            session.getAvatarService().serverDialog("Bạn trả lời đúng " + result + "/3 câu.");
            return;
        }
        step++;
        show(QUESTIONS[step - 1], ANSWERS[step - 1]);
    }

    private void show(String question, String[] choices) {
        // shortcut: 128 menu IDs per session; upgrade when the protocol supports nonces.
        if (nextMenuId > 127) {
            clear();
            session.getAvatarService().serverDialog("Vui lòng kết nối lại để tiếp tục bài kiểm tra.");
            return;
        }
        menuId = nextMenuId++;
        session.getAvatarService().openUIMenu(NPC_ID, menuId,
                Arrays.stream(choices).map(s -> Menu.builder().name(s).build()).collect(Collectors.toList()),
                "Cô giáo tiếng Anh", question);
    }
}
