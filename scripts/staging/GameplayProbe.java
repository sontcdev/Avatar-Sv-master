import avatar.server.Avatar;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

/** Staging-only entry point; never change the production service to this class. */
public class GameplayProbe {
    public static void main(String[] args) {
        Thread probe = new Thread(() -> {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            while (!Thread.currentThread().isInterrupted()) {
                MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                System.out.printf("STAGING_METRIC heapUsed=%d heapCommitted=%d heapMax=%d javaThreads=%d processCpuNs=%d%n",
                        heap.getUsed(), heap.getCommitted(), heap.getMax(),
                        ManagementFactory.getThreadMXBean().getThreadCount(), os.getProcessCpuTime());
                try { Thread.sleep(10_000); }
                catch (InterruptedException stop) { Thread.currentThread().interrupt(); }
            }
        }, "staging-metrics");
        probe.setDaemon(true);
        probe.start();
        Avatar.main(args);
    }
}
