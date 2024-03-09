import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {

    private static volatile boolean serverRunning = true;

    public static final int cycleTime = 600;
    private static final int TIME_LIMIT_SECONDS = 180; // 3 minutes
    private static int UPDATE_SECONDS = 180; // 3 minutes
    public static boolean updateServer = false;
    public static long startTime;
    public static Server clientHandler = null;

    public static java.net.ServerSocket clientListener = null;

    public static boolean shutdownServer = false;
    public static boolean shutdownClientHandler;
    public static int serverlistenerPort = 43594;
    public static PlayerHandler playerHandler = null;
    public static NpcHandler npcHandler = null;

    public static ItemHandler itemHandler = null;
    public static ShopHandler shopHandler = null;
    public static int EnergyRegian = 60;
    public static int MaxConnections = 100000;

    public static String[] Connections = new String[MaxConnections];

    public static int[] ConnectionCount = new int[MaxConnections];

    public static boolean ShutDown = false;

    public static int ShutDownCounter = 0;

    public static void calcTime() {
        if (shutdownServer) {
            return;
        }

        long elapsedTimeInSeconds = (System.nanoTime() - startTime) / 1_000_000_000;
        UPDATE_SECONDS = TIME_LIMIT_SECONDS - (int) elapsedTimeInSeconds;

        if (UPDATE_SECONDS <= 0) {
            shutdownServer = true;
        }
    }

    public static void main(String args[]) {
        clientHandler = new Server();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        executor.execute(clientHandler);

        playerHandler = new PlayerHandler();
        npcHandler = new NpcHandler();
        itemHandler = new ItemHandler();
        shopHandler = new ShopHandler();

        int waitFails = 0;
        long lastTicks = System.nanoTime();
        long totalTimeSpentProcessing = 0;
        int cycle = 0;

        Runnable processHandlers = () -> {
            playerHandler.process();
            npcHandler.process();
            itemHandler.process();
            shopHandler.process();
        };

        while (!shutdownServer) {
            if (updateServer) {
                calcTime();
            }

            executor.execute(processHandlers);

            long timeSpent = System.nanoTime() - lastTicks;
            totalTimeSpentProcessing += timeSpent;
            if (timeSpent >= cycleTime * 1_000_000) { // Converting to nanoseconds
                timeSpent = cycleTime * 1_000_000;
                if (++waitFails > 100) {
                    shutdownServer = true;
                    Misc.println("[KERNEL]: machine is too slow to run this server!");
                }
            }

            try {
                Thread.sleep(Math.max(0, cycleTime - (timeSpent / 1_000_000)));
            } catch (InterruptedException _ex) {
                Thread.currentThread().interrupt(); // Proper interrupt handling
            }

            lastTicks = System.nanoTime();
            cycle++;
            if (cycle % 100 == 0) {
                float time = ((float) totalTimeSpentProcessing) / (cycle * 1_000_000);
                Misc.println_debug("[KERNEL]: " + (time * 100 / cycleTime) + "% processing time");
            }

            if (ShutDown) {
                if (ShutDownCounter >= 100) {
                    shutdownServer = true;
                }
                ShutDownCounter++;
            }
        }

        playerHandler.destruct();
        clientHandler.killServer();
        clientHandler = null;

        try {
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) { // Wait for tasks to finish
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public Server() {
    }

    public void killServer() {
        shutdownClientHandler = true;
        setServerRunning(false);

        try {
            if (clientListener != null) {
                clientListener.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        clientListener = null;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(serverlistenerPort)) {
            System.out.println("Starting server on " + serverSocket.getInetAddress().getHostAddress() + ":"
                    + serverSocket.getLocalPort());

            while (!shutdownClientHandler) {
                final Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);

                String connectingHost = clientSocket.getInetAddress().getHostName();
                System.out.println("ClientHandler: Accepted from " + connectingHost + ":" + clientSocket.getPort());

                new Thread(() -> {
                    playerHandler.newPlayerClient(clientSocket, connectingHost);
                }).start();
            }
        } catch (IOException ioe) {
            if (!shutdownClientHandler) {
                System.out.println(
                        "Error: Unable to startup listener on " + serverlistenerPort + " - port already in use?");
            } else {
                System.out.println("ClientHandler was shut down.");
            }
        }
    }

    public static boolean isServerRunning() {
        return serverRunning;
    }

    public static void setServerRunning(boolean serverRunning) {
        Server.serverRunning = serverRunning;
    }
}
