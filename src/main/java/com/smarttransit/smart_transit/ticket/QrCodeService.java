package com.smarttransit.smart_transit.ticket;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Encodes text as a QR code, as a module matrix (for PDF) or an SVG data URI (for the web ticket). */
@Service
public class QrCodeService {

	private static final int QUIET_ZONE = 4;

	public boolean[][] matrix(String content) {
		try {
			BitMatrix bits = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
					Map.of(EncodeHintType.MARGIN, 0, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
			boolean[][] modules = new boolean[bits.getHeight()][bits.getWidth()];
			for (int y = 0; y < bits.getHeight(); y++) {
				for (int x = 0; x < bits.getWidth(); x++) {
					modules[y][x] = bits.get(x, y);
				}
			}
			return modules;
		}
		catch (WriterException ex) {
			throw new IllegalStateException("QR code could not be generated", ex);
		}
	}

	public String svgDataUri(String content) {
		boolean[][] modules = matrix(content);
		int size = modules.length + 2 * QUIET_ZONE;
		StringBuilder path = new StringBuilder();
		for (int y = 0; y < modules.length; y++) {
			for (int x = 0; x < modules[y].length; x++) {
				if (modules[y][x]) {
					path.append('M').append(x + QUIET_ZONE).append(' ').append(y + QUIET_ZONE).append("h1v1h-1z");
				}
			}
		}
		String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + size + ' ' + size
				+ "\" shape-rendering=\"crispEdges\"><rect width=\"" + size + "\" height=\"" + size
				+ "\" fill=\"#fff\"/><path fill=\"#000\" d=\"" + path + "\"/></svg>";
		return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
	}
}
