package com.smarttransit.smart_transit.ticket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A minimal single-page PDF writer using raw PDF syntax: text (Helvetica), straight lines and filled rectangles.
 * This covers everything a ticket layout needs without pulling in a heavyweight PDF library.
 */
final class MinimalPdfWriter {

	private static final double PAGE_WIDTH = 595; // A4 in points
	private static final double PAGE_HEIGHT = 842;

	private final StringBuilder content = new StringBuilder();

	double pageWidth() { return PAGE_WIDTH; }

	double pageHeight() { return PAGE_HEIGHT; }

	void text(double x, double y, double fontSize, String text) {
		content.append("BT /F1 ").append(fontSize).append(" Tf ").append(fmt(x)).append(' ').append(fmt(y))
				.append(" Td (").append(escape(text)).append(") Tj ET\n");
	}

	void boldText(double x, double y, double fontSize, String text) {
		content.append("BT /F2 ").append(fontSize).append(" Tf ").append(fmt(x)).append(' ').append(fmt(y))
				.append(" Td (").append(escape(text)).append(") Tj ET\n");
	}

	void line(double x1, double y1, double x2, double y2, double width) {
		content.append(fmt(width)).append(" w ").append(fmt(x1)).append(' ').append(fmt(y1)).append(" m ")
				.append(fmt(x2)).append(' ').append(fmt(y2)).append(" l S\n");
	}

	void rect(double x, double y, double w, double h, boolean fill) {
		content.append(fmt(x)).append(' ').append(fmt(y)).append(' ').append(fmt(w)).append(' ').append(fmt(h))
				.append(" re ").append(fill ? "f" : "S").append('\n');
	}

	/** Renders a QR module matrix as filled squares inside the given box. */
	void qr(boolean[][] modules, double x, double y, double size) {
		double cell = size / modules.length;
		content.append("0 g\n");
		for (int row = 0; row < modules.length; row++) {
			for (int col = 0; col < modules[row].length; col++) {
				if (modules[row][col]) {
					double px = x + col * cell;
					double py = y + size - (row + 1) * cell;
					rect(px, py, cell, cell, true);
				}
			}
		}
	}

	byte[] build() {
		try {
			List<byte[]> objects = new ArrayList<>();
			objects.add(obj("<< /Type /Catalog /Pages 2 0 R >>"));
			objects.add(obj("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
			objects.add(obj("<< /Type /Page /Parent 2 0 R /Resources 4 0 R /MediaBox [0 0 " + fmt(PAGE_WIDTH) + " "
					+ fmt(PAGE_HEIGHT) + "] /Contents 5 0 R >>"));
			objects.add(obj("<< /Font << /F1 6 0 R /F2 7 0 R >> >>"));
			byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
			objects.add(streamObj(stream));
			objects.add(obj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
			objects.add(obj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
			int[] offsets = new int[objects.size() + 1];
			for (int i = 0; i < objects.size(); i++) {
				offsets[i + 1] = out.size();
				out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
				out.write(objects.get(i));
				out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
			}
			int xrefStart = out.size();
			out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
			out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
			for (int i = 1; i <= objects.size(); i++) {
				out.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.ISO_8859_1));
			}
			out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefStart
					+ "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static byte[] obj(String body) {
		return body.getBytes(StandardCharsets.ISO_8859_1);
	}

	private static byte[] streamObj(byte[] streamBytes) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(("<< /Length " + streamBytes.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
		out.write(streamBytes);
		out.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
		return out.toByteArray();
	}

	private static String fmt(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String escape(String text) {
		String ascii = text == null ? "" : text.replaceAll("[^\\x20-\\x7E]", "");
		return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
	}
}
