package rogatkin.mobile.data.pertusin;

import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import io.nayuki.qrcodegen.QrCode;
import io.nayuki.qrcodegen.QrCode.Ecc;
import com.coolbong.barcodegenerator.model.Code128;

public class BarcodeAssistant {

	public boolean saveQRBarcode(OutputStream stream, String text, int scale, int border, Ecc erc) {
		return QrCode.encodeText(text, erc).toImage(scale, border).compress(CompressFormat.PNG, 80, stream);
	}

	public Bitmap getQR(String text) {
		return getQR(text, 10, 4, Ecc.MEDIUM);
	}

	public Bitmap getQR(String text, Context context) {
		int scale = getScale(context);
		return getQR(text, scale, scale > 10 ? 4 : 2, Ecc.MEDIUM);
	}

	public Bitmap getQR(String text, int scale, int border, Ecc erc) {
		return QrCode.encodeText(text, erc).toImage(scale, border);
	}

	public Bitmap getCode128(String text, Context context) {
		int scale = getScale(context);
		int w = 68 * scale;
		int h = 30 * scale;
		Code128 code = new Code128(context);
		code.setData(text);
		return code.getBitmap(w, h);
	}

	public static int getScale(Context context) {
		int dpi = context.getResources().getDisplayMetrics().densityDpi;
		if (dpi > 520)
			return 16;
		else if (dpi > 410)
			return 12;
		else if (dpi > 310)
			return 10;
		return 5;
	}
}
