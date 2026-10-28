import avatar.server.Avatar;
import avatar.server.UserManager;
import avatar.db.DbManager;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.management.*;
import java.nio.file.*;
import java.util.*;
public class M25Metrics {
 public static void main(String[] args)throws Exception {
  if(!Path.of("").toRealPath().toString().equals("/home/sontc/avatar2d-gameplay-stage")||System.getProperty("avatar.farm.sales.catalog")!=null)throw new IllegalStateException("staging only");
  Thread t=new Thread(()->{try {
   var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
   var field=DbManager.class.getDeclaredField("hikariDataSource");field.setAccessible(true);
   Path out=Path.of("m25-metrics.csv");
   Files.writeString(out,"epoch_ms,cpu_ns,heap_used,heap_committed,gc_count,gc_ms,java_threads,authenticated,pool_active,pool_idle,pool_waiting,rss_kb,os_threads,read_bytes,write_bytes\n");
   while(true){
    var mem=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();long gc=0,gcms=0;
    for(var x:ManagementFactory.getGarbageCollectorMXBeans()){gc+=Math.max(0,x.getCollectionCount());gcms+=Math.max(0,x.getCollectionTime());}
    int auth=0; synchronized(UserManager.users){for(var u:UserManager.users)if(u.session!=null&&u.session.login&&u.session.isConnected())auth++;}
    var ds=(HikariDataSource)field.get(DbManager.getInstance());
    int a=-1,i=-1,w=-1;if(ds!=null){var mx=ds.getHikariPoolMXBean();a=mx.getActiveConnections();i=mx.getIdleConnections();w=mx.getThreadsAwaitingConnection();}
    var status=Files.readString(Path.of("/proc/self/status"));var io=Files.readString(Path.of("/proc/self/io"));
    long rss=number(status,"VmRSS:"),threads=number(status,"Threads:"),read=number(io,"read_bytes:"),write=number(io,"write_bytes:");
    Files.writeString(out,String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",System.currentTimeMillis(),os.getProcessCpuTime(),mem.getUsed(),mem.getCommitted(),gc,gcms,ManagementFactory.getThreadMXBean().getThreadCount(),auth,a,i,w,rss,threads,read,write),StandardOpenOption.APPEND);
    Thread.sleep(5000);
   }
  }catch(Exception e){e.printStackTrace();}},"staging-metrics"); t.setDaemon(true);t.start();Avatar.main(args);
 }
 static long number(String s,String key){for(String l:s.split("\n"))if(l.startsWith(key))return Long.parseLong(l.substring(key.length()).trim().split("\\s+")[0]);return -1;}
}
