package mandelBrot;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import pixelWindow.PixelWindow;

public class MandelBrot {

	private static final int MAX_ITERATIONS = 1000;
	private static final double ZOOM_FACTOR = 1.5;
	private static final double MOVE_FACTOR = 0.1;
	private static final String TITLE = "Mandelbrot";

	private static final int width = 1000;
	private static final int height = 700;
	private PixelWindow window = new PixelWindow(width, height, TITLE);

	private static final double X_START = -2.0;
	private static final double Y_START = X_START * height / width;
	private static final double X_SIZE = 4.0;
	private static final double Y_SIZE = X_SIZE * height / width;

	private double xStart = X_START;
	private double yStart = Y_START;
	private double xSize = X_SIZE;
	private double ySize = Y_SIZE;

	public static void main(String[] args) {
		new MandelBrot().run();
	}

	private void run() {
		window.addMouseWheelListener(new Scroller());
		window.addKeyListener(new Mover());
		window.addMouseMotionListener(new Dragger());
		render();
	}

	private void render() {
		long startTime = System.currentTimeMillis();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
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
			long time = System.currentTimeMillis();
			if (time - startTime > 1000L) {
				System.out.printf("%f%% done\n", x * 100.0 / width);
				startTime += 1000L;
			}
		}
		String title = String.format("%s x: %.15f, y: %.15f\n", TITLE, xSize, ySize);
		window.setTitle(title);
		window.render();
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
		/*
		 * if (it == MAX_ITERATIONS) { return 0; } int startRed = 0x00, startGreen =
		 * 0x00, startBlue = 0xFF; int endRed = 0xFF, endGreen = 0xFF, endBlue = 0xFF;
		 * int r = startRed + (int) (scale * (endRed - startRed)); int g = startGreen +
		 * (int) (scale * (endGreen - startGreen)); int b = startBlue + (int) (scale *
		 * (endBlue - startBlue)); return r + g + b;
		 */
		if (it == MAX_ITERATIONS) {
			return 0;
		}

		double scale = ((double) it) / MAX_ITERATIONS;
		int greyScale = 0xFF / 2 + (int) (0.5 * scale * 0xFF);
		return greyScale * 0x10000 + greyScale * 0x100 + greyScale;
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

	private class Dragger implements MouseMotionListener, MouseListener {
		private int x = -1, y = -1;

		@Override
		public void mouseDragged(MouseEvent e) {
			if (x == -1)
				x = e.getX();
			if (y == -1)
				y = e.getY();
			if (window.mouseButtonPressed(MouseEvent.BUTTON1)) {
				int dx = e.getX() - x;
				int dy = e.getY() - y;
				double xMove = -((double) dx) / width * xSize;
				double yMove = ((double) dy) / height * ySize;
				move(xMove, yMove);
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
			case 'w':
				move(0.0, ySize * MOVE_FACTOR);
				break;
			case 'a':
				move(-xSize * MOVE_FACTOR, 0.0);
				break;
			case 's':
				move(0.0, -ySize * MOVE_FACTOR);
				break;
			case 'd':
				move(xSize * MOVE_FACTOR, 0.0);
				break;
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
