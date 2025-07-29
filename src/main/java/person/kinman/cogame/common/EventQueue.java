package person.kinman.cogame.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventQueue {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private boolean running = false;

    public void start() {
        running = true;
        new Thread(this::processEvents).start();
    }

    public void stop() {
        running = false;
    }

    public void enqueue(Runnable event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processEvents() {
        while (running) {
            try {
                Runnable event = queue.take();
                event.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}