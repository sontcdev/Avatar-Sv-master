import avatar.db.DbManager;
import avatar.model.User;
import avatar.play.MapManager;
import avatar.server.Avatar;
import avatar.server.UserManager;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Standalone, staging-only initial-position fixture; never packaged in the server JAR. */
public final class Npc850Fixture {
    public static void main(String[] args) throws Exception {
        if (!Path.of("").toRealPath().equals(Path.of("/home/sontc/avatar2d-gameplay-stage")))
            throw new IllegalStateException("Fixture requires the isolated staging directory");
        Properties config=new Properties();
        try (var in=Files.newInputStream(Path.of("config.properties"))) { config.load(in); }
        if (!"19129".equals(config.getProperty("server.port")) || !"127.0.0.1".equals(config.getProperty("server.bind"))
                || System.getProperty("avatar.farm.sales.catalog")!=null)
            throw new IllegalStateException("Unsafe staging endpoint/sales configuration");
        Properties db=new Properties();
        try (var in=Files.newInputStream(Path.of(System.getProperty("avatar.database.config","database.properties")))) { db.load(in); }
        if (!"avatar2d_gameplay_stage".equals(db.getProperty("dbname")) || !"13317".equals(db.getProperty("port"))
                || !"127.0.0.1".equals(db.getProperty("host")))
            throw new IllegalStateException("Fixture requires isolated staging database");
        // No persisted location fields exist; save and restore only this fixture's live placement.
        Path restore=Path.of("m23-fixture-restore.request");
        if (Files.exists(restore)) throw new IllegalStateException("Remove previous restore request only after inspecting it");
        Map<User,Runnable> placed=new IdentityHashMap<>();
        Thread fixture=new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (Files.exists(restore)) {
                        placed.forEach((player,action) -> { if (UserManager.getInstance().find(71)==player) action.run(); });
                        System.out.println("STAGING_FIXTURE restored live placement; no location DB fields modified");
                        return;
                    }
                    User u=UserManager.getInstance().find(71);
                    if (u!=null && u.getZone()!=null && !placed.containsKey(u)) {
                        synchronized (u) {
                            var old=u.getZone(); short x=u.getX(),y=u.getY();
                            var target=MapManager.getInstance().find(25).getZones().get(0);
                            User npc=target.find(2000000850);
                            if (npc==null) throw new IllegalStateException("NPC850 absent");
                            try (Connection c=DbManager.getInstance().getConnection()) {
                                if (!"avatar2d_gameplay_stage".equals(c.getCatalog()) || !c.getMetaData().getURL().contains(":13317/"))
                                    throw new IllegalStateException("Unexpected live fixture DB");
                            }
                            System.out.printf("STAGING_FIXTURE user=71 original map=%d zone=%d x=%d y=%d target map=25 zone=0 x=90 y=65 npcX=%d npcY=%d%n",old.getMap().getId(),old.getId(),x,y,npc.getX(),npc.getY());
                            target.enter(u,(short)90,(short)65); placed.put(u,() -> old.enter(u,x,y));
                        }
                    }
                    Thread.sleep(250);
                } catch (InterruptedException stop) { Thread.currentThread().interrupt(); }
                catch (Exception failure) { failure.printStackTrace(); return; }
            }
        },"staging-npc850-fixture");
        fixture.setDaemon(true); fixture.start(); Avatar.main(args);
    }
}
