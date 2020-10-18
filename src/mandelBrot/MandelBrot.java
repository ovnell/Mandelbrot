package mandelBrot;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pixelWindow.PixelWindow;

public class MandelBrot {

	private static final int THREADS = 8;
	private static final int PARTITIONS = 32;

	private static final int MAX_ITERATIONS = 1000;
	private static final double ZOOM_FACTOR = 1.5;
	private static final String TITLE = "Mandelbrot";

	private static final int width = 1000;
	private static final int height = 700;
	private PixelWindow window = new PixelWindow(width, height, TITLE);

	private static final double X_START = -2.3;
	private static final double X_SIZE = 3.5;
	private static final double Y_START = -X_SIZE * height / width / 2.0;
	private static final double Y_SIZE = X_SIZE * height / width;

	private double xStart = X_START;
	private double yStart = Y_START;
	private double xSize = X_SIZE;
	private double ySize = Y_SIZE;

	private ExecutorService pool;
	
	public static void main(String[] args) {
		new MandelBrot().run();
	}

	private void run() {
		window.addMouseWheelListener(new Scroller());
		window.addKeyListener(new Mover());
		Dragger d = new Dragger();
		window.addMouseMotionListener(d);
		window.addMouseListener(d);
		render();
	}

	private void render() {
		long startTime = System.nanoTime();
		pool = Executors.newFixedThreadPool(THREADS);
		
		for (int i = 0; i < PARTITIONS; i++) {
			int xStart = i * width / PARTITIONS;
			int xEnd = (i + 1) * width / PARTITIONS;
			pool.execute(new Renderer(xStart, xEnd - xStart));
			//render(xStart, 0, xEnd - xStart, height);
		}

		// Wait for the threads to finish
		pool.shutdown();
		while (!pool.isTerminated()) {
		}

		long time = System.nanoTime();
		String title = String.format("%s x: %.15f, y: %.15f %.0fms/frame", TITLE, xSize, ySize, 1e-6 * (time - startTime));
		window.setTitle(title);
		window.render();
	}

	private void render(int startX, int startY, int dx, int dy) {
		for (int x = startX; x < startX + dx; x++) {
			for (int y = startY; y < startY + dy; y++) {
				int yp = height - y - 1; // reverse y
				double x0 = getX(x);
				double y0 = getY(y);
				double xx = 0.0, yy = 0.0;
				int it = 0;
				for (; xx * xx + yy * yy < 4.0 && it < MAX_ITERATIONS; it++) {
					double xTemp = xx * xx - yy * yy + x0;
					yy = 2 * xx * yy + y0;
					xx = xTemp;
				}
				window.setPixel(x, yp, palette(it));
			}
		}
	}

	private double getX(double x) {
		double scale = x / width;
		return xStart + xSize * scale;
	}

	private double getY(double y) {
		double scale = y / height;
		return yStart + ySize * scale;
	}

	private int palette(int it) {
		if (it == MAX_ITERATIONS) {
			return 0;
		}
		double scale = ((double) it) / MAX_ITERATIONS;	// 0 to 1
		int greyScale = 0xFF / 2 + (int) (0.5 * scale * 0xFF);
		return PixelWindow.getColorCode(greyScale, greyScale, greyScale);
	}

	private void zoom(double amount) {
		double sizeMultiple = 1.0 / amount;
		double x = window.getMouseX(), y = window.getMouseY();
		double leftSidePerc = x / width, botSidePerc = 1.0 - y / height;
		xStart -= (sizeMultiple - 1.0) * xSize * leftSidePerc;
		yStart -= (sizeMultiple - 1.0) * ySize * botSidePerc;
		xSize *= sizeMultiple;
		ySize *= sizeMultiple;
		render();
	}

	private void move(double dx, double dy) {
		xStart += dx;
		yStart += dy;
		render();
	}

	private void reset() {
		xStart = X_START;
		yStart = Y_START;
		xSize = X_SIZE;
		ySize = Y_SIZE;
		render();
	}

	private class Renderer implements Runnable {
		int xStart, dx;

		public Renderer(int xStart, int dx) {
			this.xStart = xStart;
			this.dx = dx;
		}

		@Override
		public void run() {
			render(xStart, 0, dx, height);
		}
	}

	// Only moves when you release the mouse button
	private class Dragger implements MouseMotionListener, MouseListener {
		private int x = -1, y = -1;
		private int dx = 0, dy = 0;

		@Override
		public void mouseDragged(MouseEvent e) {
			if (x == -1)
				x = e.getX();
			if (y == -1)
				y = e.getY();
			if (window.mouseButtonPressed(MouseEvent.BUTTON1)) {
				dx += e.getX() - x;
				dy += e.getY() - y;
			}
			x = e.getX();
			y = e.getY();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			x = e.getX();
			y = e.getY();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			double xMove = -((double) dx) / width * xSize;
			double yMove = ((double) dy) / height * ySize;
			move(xMove, yMove);
			dx = dy = 0;
		}

	}

	private class Scroller implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.getWheelRotation() > 0) { // Scrolled down
				zoom(1.0 / ZOOM_FACTOR);
			} else { // Scrolled up
				zoom(ZOOM_FACTOR);
			}
		}
	}

	private class Mover implements KeyListener {

		private static final double BIG_ZOOM = 10.0;

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
				System.exit(0);
			}
			switch (e.getKeyChar()) {
			case 'r':
				reset();
				break;
			case '+':
				zoom(BIG_ZOOM);
				break;
			case '-':
				zoom(1.0 / BIG_ZOOM);
				break;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

}
